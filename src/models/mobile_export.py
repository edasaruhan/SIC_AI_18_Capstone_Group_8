"""Package the saved dataset_v1 fold_1 XGBoost model for ONNX Runtime."""

import argparse
from dataclasses import asdict
from hashlib import sha256
from importlib.metadata import version
from itertools import product
import json
from pathlib import Path
from tempfile import TemporaryDirectory

import joblib
import numpy as np
import pandas as pd

from .dataset_v1 import V1_FEATURES, load_v1, sha256_file
from .tree_models import XGBoostModel


NAMES = [spec.name for spec in V1_FEATURES]
ANDROID_FIELDS = ("screenSeconds", "sessionsStarted", "longestRunSeconds", "appSwitches",
                  "previousSessionSeconds", "hour", "dayOfWeek")
INPUT = "features"
OUTPUT = "positive_score"
SCORE_ATOL = 1e-6
FEATURE_SCHEMA_VERSION = "v1"
PREPROCESSING_VERSION = "v1-median-indicator-float32"
OPSET = 13


def load_fold_1(model_dir, experiment):
    path = Path(model_dir) / "fold_1" / "xgboost.joblib"
    metadata = json.loads(path.with_suffix(".json").read_text())
    if (metadata["fold"], metadata["model"], metadata["policy"],
            metadata["target"], metadata["features"]) != (
                1, "XGBoost", "validation_selected", "y_next_any", NAMES):
        raise ValueError("Expected the preselected dataset_v1 fold_1 XGBoost artifact")
    if metadata not in experiment["saved_model_checks"]:
        raise ValueError("Fold metadata differs from the recorded experiment")
    # The input must be a trusted local joblib artifact.
    model = joblib.load(path)
    if (type(model) is not XGBoostModel or model.features != V1_FEATURES
            or model.decision_threshold != metadata["decision_threshold"]
            or any(model.params.get(key) != value for key, value in metadata["overrides"].items())):
        raise ValueError("Saved Python model does not match the fold metadata")
    return model, metadata, sha256_file(path)


def build_graph(model):
    import onnx
    from onnx import TensorProto, helper, numpy_helper
    from onnxmltools.convert import convert_xgboost
    from onnxmltools.convert.common.data_types import FloatTensorType
    from sklearn.impute import SimpleImputer

    if list(model.pipeline_.named_steps) != ["imputer", "classifier"]:
        raise ValueError("Unexpected XGBoost pipeline steps")
    imputer, classifier = model.pipeline_.named_steps.values()
    names = [spec.name for spec in model.features]
    if (type(imputer) is not SimpleImputer or imputer.strategy != "median"
            or not imputer.keep_empty_features or not imputer.add_indicator
            or not np.isnan(imputer.missing_values)
            or list(imputer.feature_names_in_) != names):
        raise ValueError("Unexpected fitted imputation pipeline")
    medians = imputer.statistics_
    indicators = imputer.indicator_.features_
    width = len(names) + len(indicators)
    if (medians.shape != (len(names),) or not np.isfinite(medians.astype(np.float32)).all()
            or classifier.n_features_in_ != width or classifier.objective != "binary:logistic"
            or classifier.classes_.tolist() != [0, 1]):
        raise ValueError("Unexpected classifier input or score semantics")

    converted = convert_xgboost(
        classifier, initial_types=[("classifier_input", FloatTensorType([None, width]))],
        target_opset=OPSET,
    )
    if (len(converted.graph.node) != 1
            or [(op.domain, op.version) for op in converted.opset_import] != [("ai.onnx.ml", 1)]):
        raise ValueError("Unexpected XGBoost converter graph")
    tree = converted.graph.node[0]
    if (tree.domain, tree.op_type, list(tree.input), len(tree.output)) != (
            "ai.onnx.ml", "TreeEnsembleClassifier", ["classifier_input"], 2):
        raise ValueError("Unexpected XGBoost converter node")
    # The wrapper uses the positive-class probability, with its own decision threshold.
    # The converter's native label follows a different cutoff and is kept internal.
    imputed = "imputed" if indicators.size else "classifier_input"
    nodes = [
        helper.make_node("IsNaN", [INPUT], ["missing_mask"]),
        helper.make_node("Where", ["missing_mask", "medians", INPUT], [imputed]),
    ]
    initializers = [
        numpy_helper.from_array(medians.astype(np.float32), "medians"),
        numpy_helper.from_array(np.array(1, dtype=np.int64), "positive_class_index"),
    ]
    if indicators.size:
        initializers.append(numpy_helper.from_array(indicators.astype(np.int64), "indicator_columns"))
        nodes.extend([
            helper.make_node("Gather", ["missing_mask", "indicator_columns"], ["indicator_mask"], axis=1),
            helper.make_node("Cast", ["indicator_mask"], ["indicators"], to=TensorProto.FLOAT),
            helper.make_node("Concat", [imputed, "indicators"], ["classifier_input"], axis=1),
        ])
    nodes.extend([
        tree,
        helper.make_node("Gather", [tree.output[1], "positive_class_index"], [OUTPUT], axis=1),
    ])
    graph = helper.make_graph(
        nodes, "dataset_v1_binary_score",
        [helper.make_tensor_value_info(INPUT, TensorProto.FLOAT, ["batch", len(names)])],
        [helper.make_tensor_value_info(OUTPUT, TensorProto.FLOAT, ["batch"])],
        initializer=initializers,
    )
    exported = helper.make_model(
        graph, ir_version=7, producer_name="adaptive-digital-wellbeing-coach",
        opset_imports=[helper.make_opsetid("", OPSET), helper.make_opsetid("ai.onnx.ml", 1)],
    )
    onnx.checker.check_model(exported, full_check=True)
    preprocessing = {
        "preprocessingVersion": PREPROCESSING_VERSION,
        "location": "embedded_in_onnx",
        "imputer": "SimpleImputer",
        "strategy": "median",
        "missingValue": "NaN",
        "addIndicator": True,
        "keepEmptyFeatures": True,
        "statistics": medians.tolist(),
        "indicatorIndices": indicators.tolist(),
        "classifierColumns": names + [f"missingindicator_{names[i]}" for i in indicators],
        "classifierDtype": "float32",
        "scaling": "none",
    }
    return exported, preprocessing


def runtime_session(model_bytes):
    import onnxruntime as ort

    options = ort.SessionOptions()
    options.intra_op_num_threads = 1
    return ort.InferenceSession(model_bytes, sess_options=options, providers=["CPUExecutionProvider"])


def compare_scores(model, runtime, frame, *, single_row=False):
    expected = model.predict_scores(frame)
    expected_decisions = model.predict(frame)
    values = frame.to_numpy(dtype=np.float32)
    if single_row:
        # Match the Android Predictor's one-window-at-a-time call shape.
        actual = np.concatenate([runtime.run([OUTPUT], {INPUT: row[None, :]})[0] for row in values])
    else:
        actual, = runtime.run([OUTPUT], {INPUT: values})
    cutoff = model.decision_threshold
    actual_decisions = actual.astype(np.float64) >= cutoff
    np.testing.assert_allclose(actual, expected, rtol=0, atol=SCORE_ATOL, equal_nan=False)
    if not np.isfinite(actual).all() or ((actual < 0) | (actual > 1)).any():
        raise ValueError("ONNX output must contain finite probabilities in [0, 1]")
    np.testing.assert_array_equal(actual_decisions, expected_decisions)
    return {
        "rows": len(frame),
        "maxAbsScoreError": float(np.max(np.abs(actual - expected))),
        "minPythonScoreDistanceToThreshold": float(np.min(np.abs(expected.astype(np.float64) - cutoff))),
        "decisionMismatches": 0,
    }, expected, expected_decisions


def synthetic_inputs(model):
    # Immediate float32 neighbours of the first screen-duration split.
    classifier = model.pipeline_.named_steps["classifier"]
    root = json.loads(classifier.get_booster().get_dump(dump_format="json")[0])
    if root["split"] != "f0":
        raise ValueError("The expected fold_1 screen_s split has changed")
    split = np.float32(root["split_condition"])
    missing_case = [760.125, 4, 410.5, 12, 175.25, 19, 5]
    return {
        "no_activity": [0, 0, 0, 0, 0, 0, 0],
        "max_duration_and_clock": [1800, 1, 1800, 20, 10800, 23, 6],
        "carried_session": [900.125, 0, 900.125, 4, 0, 8, 1],
        "just_below_cutoff": [582.703, 2, 55.651, 12, 3279.461, 20, 6],
        "just_above_cutoff": [108.951, 5, 22.993, 15, 2424.009, 19, 6],
        "below_tree_split": [float(np.nextafter(split, np.float32(-np.inf))), 3, 300, 9, 300, 18, 4],
        "at_tree_split": [float(split), 3, 300, 9, 300, 18, 4],
        "above_tree_split": [float(np.nextafter(split, np.float32(np.inf))), 3, 300, 9, 300, 18, 4],
        **{
            f"missing_{name}": [None if j == i else value for j, value in enumerate(missing_case)]
            for i, name in enumerate(NAMES)
        },
        "all_missing": [None] * len(NAMES),
    }


def write_package(output, files):
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        raise FileExistsError(f"Output already exists: {output}")
    with TemporaryDirectory(dir=output.parent) as tmp:
        staging = Path(tmp) / "package"
        staging.mkdir()
        for name, value in files.items():
            path = staging / name
            if isinstance(value, bytes):
                path.write_bytes(value)
            else:
                path.write_text(json.dumps(value, indent=2, allow_nan=False) + "\n", encoding="utf-8")
        staging.rename(output)


def export_package(model_dir, dataset, experiment_path, output):
    import onnx

    experiment = json.loads(experiment_path.read_text())
    model, source, digest = load_fold_1(model_dir, experiment)
    model_version = f"dataset-v1-xgboost-fold-1-{digest[:12]}"
    exported, preprocessing = build_graph(model)
    onnx.helper.set_model_props(exported, {
        "modelVersion": model_version,
        "featureSchemaVersion": FEATURE_SCHEMA_VERSION,
        "preprocessingVersion": PREPROCESSING_VERSION,
        "target": source["target"],
        "decisionThreshold": str(model.decision_threshold),
    })
    model_bytes = exported.SerializeToString()
    runtime = runtime_session(model_bytes)
    examples = synthetic_inputs(model)
    synthetic, scores, decisions = compare_scores(
        model, runtime, pd.DataFrame(list(examples.values()), columns=NAMES, dtype=float),
        single_row=True)
    masks = [[None if missing else value for missing, value in zip(mask, examples["carried_session"])]
             for mask in product((False, True), repeat=len(NAMES))]
    missing_masks, _, _ = compare_scores(model, runtime, pd.DataFrame(masks, columns=NAMES, dtype=float))
    table, coverage, data_manifest = load_v1(dataset)
    if any(info != experiment["data_manifest"].get(name) for name, info in data_manifest.items()):
        raise ValueError("Local dataset_v1 content differs from the recorded experiment")
    real, real_scores, _ = compare_scores(model, runtime, table[NAMES])
    distance = np.abs(real_scores.astype(np.float64) - model.decision_threshold)
    closest = np.argsort(distance, kind="stable")[:256]
    near_threshold, _, _ = compare_scores(model, runtime, table.iloc[closest][NAMES], single_row=True)
    manifest = {
        "packageVersion": 1,
        "modelVersion": model_version,
        "expectedFeatureSchemaVersion": FEATURE_SCHEMA_VERSION,
        "preprocessingVersion": PREPROCESSING_VERSION,
        "isRealModel": True,
        "modelFile": "model.onnx",
        "modelSha256": sha256(model_bytes).hexdigest(),
        "input": {"name": INPUT, "dtype": "float32", "shape": ["batch", len(NAMES)]},
        "output": {"name": OUTPUT, "dtype": "float32", "shape": ["batch"],
                   "meaning": "P(y_next_any=1), the next 30-minute window's behavioral proxy score"},
        "decisionThreshold": model.decision_threshold,
        "decisionOperator": ">=",
        "source": {"dataset": "dataset_v1", "targetDefinition": experiment["target"],
                   "fold": source["fold"], "policy": source["policy"],
                   "trainingParameters": model.params, "joblibSha256": digest,
                   "experimentSha256": sha256_file(experiment_path),
                   "xgboostBaseScore": model.pipeline_.named_steps["classifier"].intercept_.tolist()},
        "onnx": {"ir": exported.ir_version, "opset": OPSET, "ai.onnx.ml": 1},
    }
    files = {
        "model.onnx": model_bytes,
        "manifest.json": manifest,
        "feature_schema.json": {
            "version": FEATURE_SCHEMA_VERSION,
            "windowSeconds": 1800,
            "orderedFeatures": [
                {**asdict(spec), "androidField": field, "unit": unit}
                for spec, field, unit in zip(V1_FEATURES, ANDROID_FIELDS,
                    ("seconds", "count", "seconds", "count", "seconds", "hour", "weekday_monday_zero"))
            ],
        },
        "preprocessing.json": preprocessing,
        "golden_samples.json": {
            "modelVersion": model_version,
            "source": "synthetic",
            "inputOrder": NAMES,
            "scoreAbsoluteTolerance": SCORE_ATOL,
            "samples": [
                {"id": name, "features": row, "expectedPythonScore": float(score),
                 "expectedDecision": bool(decision)}
                for (name, row), score, decision in zip(examples.items(), scores, decisions)
            ],
        },
        "parity_report.json": {
            "modelVersion": model_version, "scoreAbsoluteTolerance": SCORE_ATOL,
            "realDatasetV1": {**real, "participants": coverage["selected_users"],
                              "scope": "all eligible target-bearing windows across all splits",
                              "windowsSha256": data_manifest["windows.csv.gz"]["sha256"]},
            "realNearestThresholdSingleRow": near_threshold,
            "syntheticGoldenSamples": synthetic, "syntheticMissingMasks": missing_masks,
            "runtimeProvider": "CPUExecutionProvider",
            "versions": {name: version(name) for name in (
                "xgboost", "scikit-learn", "onnx", "onnxmltools", "onnxruntime", "skl2onnx")},
        },
    }
    write_package(output, files)
    return files["parity_report.json"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, default=Path("models/refinement_dataset_v1"))
    parser.add_argument("--dataset", type=Path, default=Path("data/processed/dataset_v1"))
    parser.add_argument("--experiment", type=Path,
                        default=Path("reports/model_refinement_dataset_v1/results.json"))
    parser.add_argument("--output", type=Path, default=Path("models/mobile/dataset_v1-xgboost-fold-1-v1"))
    args = parser.parse_args()
    report = export_package(args.model_dir, args.dataset, args.experiment, args.output)
    print(json.dumps(report, indent=2))
    print(f"Package: {args.output}")


if __name__ == "__main__":
    main()
