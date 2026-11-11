"""Bounded general-model refinement on the team's prepared dataset_v1.

Run: python -m src.models.refinement_exploration
Reuses the existing participant split/threshold policy. This model-side research
does not replace Hamza's final evaluation or Ismail's data processing pipeline.
"""

import argparse
import importlib.metadata
import json
from pathlib import Path
import platform

import joblib
import numpy as np
import pandas as pd

from .baselines import LogisticRegressionBaseline
from .dataset_v1 import V1_FEATURES, load_v1
from .features import prepare_features
from .real_data_exploration import model_metrics, plot_results
from .threshold_exploration import DECISION_GRID, participant_splits, select_decision_threshold
from .tree_models import RandomForestModel, XGBoostModel


# Declared before running; no test-dependent expansion of the search.
CANDIDATES = {
    "Logistic Regression": [{"C": 1.0}, {"C": 0.1}],
    "Random Forest": [{"max_depth": 8, "min_samples_leaf": 5},
                      {"max_depth": 12, "min_samples_leaf": 10}],
    "XGBoost": [{"max_depth": 3, "n_estimators": 150},
                {"max_depth": 5, "n_estimators": 150},
                {"max_depth": 3, "n_estimators": 300}],
}
MODEL_CLASSES = {"Logistic Regression": LogisticRegressionBaseline,
                 "Random Forest": RandomForestModel, "XGBoost": XGBoostModel}


def causal_rule_scores(frame):
    """Current-window intensity, never retrospective lab_any or clean_screen_s.

    This anticipatory baseline uses current overlap >=20 min, >=6 session starts,
    or 00--05 local-clock screen duration >=5 min. It cannot reproduce the source
    long-session label, which uses a complete session extending past the window.
    """
    X = prepare_features(frame, V1_FEATURES)
    columns = ["longest_run_s", "sessions_started", "hour", "screen_s"]
    if X[columns].isna().any().any():
        raise ValueError("Missing causal rule input")
    votes = np.column_stack([
        X.longest_run_s.ge(1200), X.sessions_started.ge(6),
        X.hour.ge(0) & X.hour.lt(5) & X.screen_s.ge(300),
    ])
    return votes.mean(axis=1)  # Match fraction, not calibrated probability.


def v1_metrics(y, predicted, scores, frame):
    # Reuse the existing metric calculations, but count the SOURCE LOCAL clock's
    # prediction dates. Shifted local epoch seconds are explicitly NOT UTC.
    dates = pd.DataFrame({"participant_id": frame.participant_id,
                          "window_end_ms": (frame.w_start_s + 1800) * 1000})
    result = model_metrics(y, np.asarray(predicted), np.asarray(scores), dates)
    result["false_alerts_per_evaluated_participant_local_day"] = result.pop(
        "false_alerts_per_evaluated_participant_utc_day")
    result["evaluated_participant_local_days"] = result.pop("evaluated_participant_utc_days")
    return result


def select_candidate(training, validation, name, candidates):
    """Fit only training users; choose configuration AND cutoff on validation.

    All configurations share the same features/splits. Exact config-score ties
    prefer the first (predeclared) candidate; threshold ties prefer a higher cutoff.
    No early stopping and no refit: baseline and selected policies are traceable.
    """
    train_X, val_X = (prepare_features(f, V1_FEATURES) for f in (training, validation))
    y_train, y_val = (f.y_next_any.astype(int) for f in (training, validation))
    if y_train.nunique() != 2 or y_val.nunique() != 2:
        raise ValueError("Fitting and validation each require both target classes")
    rows, initial, best, best_rank = [], None, None, None
    for index, params in enumerate(candidates):
        model = MODEL_CLASSES[name](features=V1_FEATURES, **params).fit(train_X, y_train)
        scores = model.predict_scores(val_X)
        threshold, curve = select_decision_threshold(y_val, scores)
        metrics = v1_metrics(y_val, scores >= threshold, scores, validation)
        row = {"candidate": index, "parameters": params, "threshold": threshold,
               "validation_metrics": metrics, "threshold_curve": curve}
        rows.append(row)
        if initial is None:
            initial = (model, 0.5, params)
        rank = (metrics["f1"], -index)
        if best_rank is None or rank > best_rank:
            best, best_rank = (model, threshold, params), rank
    if best is None:
        raise ValueError("At least one candidate is required")
    return initial, best, rows


def compare_refinement(table, *, folds=3, seed=42, model_dir=None):
    fold_results, oof, searches, splits, per_user, artifacts = [], [], [], [], [], []
    for fold, train_idx, val_idx, test_idx in participant_splits(table, folds=folds, seed=seed):
        frames = {name: table.iloc[idx] for name, idx in
                  [("training", train_idx), ("validation", val_idx), ("test", test_idx)]}
        train, val, test = (frames[n] for n in ("training", "validation", "test"))
        split = {"fold": fold, **{n: sorted(f.participant_id.unique().tolist())
                                  for n, f in frames.items()}}
        splits.append(split)
        counts = {n: {"users": f.participant_id.nunique(), "rows": len(f),
                       "positives": int(f.y_next_any.sum())} for n, f in frames.items()}
        for name in ["Rule", *CANDIDATES]:
            if name == "Rule":
                policies = [("fixed_rule", None, None, {})]
            else:
                initial, selected, search = select_candidate(train, val, name, CANDIDATES[name])
                searches.append({"fold": fold, "model": name, "candidates": search})
                policies = [("initial_0.50", *initial), ("validation_selected", *selected)]
            for policy, model, cutoff, params in policies:
                metrics, scores_by_split = {}, {}
                for split_name, frame in frames.items():
                    scores = (causal_rule_scores(frame) if model is None
                              else model.predict_scores(frame))
                    predicted = (scores > 0 if model is None else scores >= cutoff).astype(int)
                    metrics[split_name] = v1_metrics(frame.y_next_any, predicted, scores, frame)
                    scores_by_split[split_name] = scores
                fold_results.append({"fold": fold, "model": name, "policy": policy,
                                     "parameters": params, "decision_threshold": cutoff,
                                     "split_counts": counts, "metrics": metrics})
                if policy == "initial_0.50":
                    continue
                scores = scores_by_split["test"]
                predicted = (scores > 0 if model is None else scores >= cutoff).astype(int)
                part = test[["participant_id", "w_start_s"]].copy()
                part["model"], part["fold"] = name, fold
                part["y"], part["score"], part["prediction"] = test.y_next_any.astype(int), scores, predicted
                oof.append(part)
                for uid, group in part.groupby("participant_id"):
                    per_user.append({"participant_id": uid, "model": name, "fold": fold,
                                     **v1_metrics(group.y, group.prediction, group.score, group)})
                if model is not None and model_dir is not None:
                    path = Path(model_dir) / f"fold_{fold}" / (name.lower().replace(" ", "_") + ".joblib")
                    path.parent.mkdir(parents=True, exist_ok=True)
                    model.decision_threshold = cutoff
                    joblib.dump(model, path)
                    restored = joblib.load(path)
                    # Verify every held-out score after loading the trusted local artifact.
                    np.testing.assert_allclose(restored.predict_scores(test), scores, rtol=0, atol=0)
                    np.testing.assert_array_equal(restored.predict(test), predicted)
                    metadata = {"fold": fold, "model": name, "policy": policy,
                                "decision_threshold": cutoff, "features": [s.name for s in V1_FEATURES],
                                "target": "y_next_any", "overrides": params,
                                "reload_all_test_scores_equal": True,
                                "reload_all_test_decisions_equal": True}
                    path.with_suffix(".json").write_text(json.dumps(metadata, indent=2) + "\n")
                    artifacts.append(metadata)
            print(f"Fold {fold}/{folds}: {name} completed ({len(train)} fit / {len(val)} validation / {len(test)} test)", flush=True)
    summary_rows = []
    numeric = ["precision", "recall", "f1", "pr_auc_ap", "roc_auc",
               "false_alerts_per_evaluated_participant_local_day"]
    for row in fold_results:
        for split_name, metrics in row["metrics"].items():
            summary_rows.append({"model": row["model"], "policy": row["policy"],
                                 "split": split_name, **{k: metrics[k] for k in numeric}})
    means = pd.DataFrame(summary_rows).groupby(["model", "policy", "split"])[numeric].mean().reset_index().to_dict("records")
    return {"fold_results": fold_results, "mean_metrics": means,
            "saved_model_checks": artifacts}, pd.concat(oof, ignore_index=True), {
                "splits": splits, "searches": searches, "per_user_metrics": per_user,
            }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=Path("data/processed/dataset_v1"))
    parser.add_argument("--pipeline", type=Path, default=Path("src/data/mpu_pipeline.py"))
    parser.add_argument("--output", type=Path, default=Path("reports/model_refinement_dataset_v1"))
    parser.add_argument("--workdir", type=Path, default=Path("data/processed/model_refinement_dataset_v1"))
    parser.add_argument("--model-dir", type=Path, default=Path("models/refinement_dataset_v1"))
    args = parser.parse_args()
    table, coverage, manifest = load_v1(args.dataset, args.pipeline)
    print(f"Validated dataset_v1: {coverage}", flush=True)
    report, predictions, local = compare_refinement(table, model_dir=args.model_dir)
    report.update({
        "scope": "General-model research on the delivered dataset_v1; not personalized or deployed",
        "coverage": coverage, "data_manifest": manifest,
        "features": [s.name for s in V1_FEATURES],
        "excluded_inputs": ["uuid", "w_start_s", "day_index", "valid", "target_ok",
                            "lab_*", "y_next_*", "unlocks_raw", "clean_screen_s", "notifs",
                            "dev_screen", "dev_unlocks", "hist_days", "is_night", "is_weekend"],
        "target": {"name": "y_next_any", "label_source": "retrospective behavioural proxy",
                   "parameters": json.loads((args.dataset / "summary.json").read_text())["parameters"],
                   "long_component": "Total duration of a valid session intersecting the next window; may extend beyond that window"},
        "protocol": {"outer": "3-fold GroupKFold by participant",
                     "inner": "20% of development users via GroupShuffleSplit, random_state=42",
                     "selection": "Maximum validation F1 jointly over bounded configurations and decision grid; config ties prefer first, cutoff ties higher",
                     "decision_grid": list(DECISION_GRID), "candidates": CANDIDATES,
                     "refit_after_selection": False, "early_stopping": False,
                     "proxy_cutoffs": "Fixed source v1 definition; not learned or optimized on this test",
                     "feature_selection": "Manual common seven-feature set; no performance-driven selection or ablation",
                     "class_weighting": "RF balanced on fitting labels; LR unweighted; XGBoost scale_pos_weight=1; decision cutoff selected separately",
                     "seed": 42},
        "fixed_parameters": {"Logistic Regression": {"max_iter": 1000, "random_state": 42,
                                                     "preprocessing": "training median + indicators + StandardScaler"},
                             "Random Forest": RandomForestModel(features=V1_FEATURES).params,
                             "XGBoost": XGBoostModel(features=V1_FEATURES).params},
        "metric_definitions": {"mean": "Unweighted mean over outer folds, separately for training/validation/test",
                               "pr_auc_ap": "Average precision, not trapezoidal PR integration",
                               "false_alert_day": "FP / unique evaluated participant and source-local prediction-date pairs, including partial dates; not actual notifications",
                               "validation": "Used for selection; optimistic, not an independent performance estimate",
                               "training": "In-sample fit diagnostics; not generalization performance"},
        "environment": {"python": platform.python_version(), **{p: importlib.metadata.version(p)
                        for p in ("numpy", "pandas", "scikit-learn", "xgboost", "matplotlib", "joblib")}},
        "limitations": [
            "New data/labels/eligibility/features differ from previous research. Old-versus-new scores do not isolate model improvement.",
            "Long-session labels depend on full session length and retrospective <=3-hour acceptance; the outcome is not strictly confined to the next 30 minutes.",
            "Eligibility and heartbeat coverage are retrospective cohort/quality decisions, not proof of live availability.",
            "Local clock uses static participant timezone + one hour, not a verified timezone/DST history.",
            "The original cohort has already been explored; outer folds are held out within this run, not a new external cohort.",
            "No user-feedback target, personal-history adaptation, calibration, SHAP or Android validation in this experiment.",
            "The source persistence lab_any baseline uses retrospective labels and is not a causal deployable competitor.",
        ],
    })
    validation_path = args.dataset / "independent_validation.json"
    if validation_path.exists():
        check = json.loads(validation_path.read_text())
        if check["pipeline_sha256"] != manifest["mpu_pipeline.py"]["sha256"]:
            raise ValueError("Independent validation used a different source script")
        report["independent_source_check"] = {
            "participants": len(check["records"]),
            "reproduced_windows": sum(r["rows_reproduced"] for r in check["records"]),
            "truncation_windows": sum(r["truncation_windows"] for r in check["records"]),
            "truncation_mismatches": sum(r["truncation_mismatches"] for r in check["records"]),
            "scope": check["scope"],
        }
    args.output.mkdir(parents=True, exist_ok=True)
    args.workdir.mkdir(parents=True, exist_ok=True)
    (args.output / "results.json").write_text(json.dumps(report, indent=2, allow_nan=False) + "\n")
    (args.workdir / "local_details.json").write_text(json.dumps(local, indent=2, allow_nan=False) + "\n")
    predictions.to_csv(args.workdir / "oof_predictions.csv", index=False)
    plot_results(predictions, args.output,
                 subtitle="Dataset v1 proxy; user-disjoint outer test folds; validation-selected settings")
    plot_refinement(report["mean_metrics"], args.output)
    print(json.dumps([r for r in report["mean_metrics"] if r["split"] == "test"], indent=2))


def plot_refinement(means, output):
    """Show within-v1 control comparisons and train/validation/test fit gaps."""
    # plot_results already initializes the writable cache and Agg backend.
    import matplotlib.pyplot as plt
    rows = pd.DataFrame(means).set_index(["model", "policy", "split"])
    names = list(CANDIDATES)
    x = np.arange(len(names))
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.4), constrained_layout=True)
    for offset, policy, label in [(-.18, "initial_0.50", "Initial config, cutoff 0.50"),
                                 (.18, "validation_selected", "Validation-selected config and cutoff")]:
        axes[0].bar(x + offset, [rows.loc[(n, policy, "test"), "f1"] for n in names],
                    width=.35, label=label)
    for offset, split in [(-.25, "training"), (0, "validation"), (.25, "test")]:
        axes[1].bar(x + offset, [rows.loc[(n, "validation_selected", split), "f1"] for n in names],
                    width=.24, label=split.capitalize())
    for ax in axes:
        ax.set(xticks=x, xticklabels=names, ylabel="Mean outer-fold F1", ylim=(0, 1))
        ax.legend(fontsize=8)
    axes[0].set_title("Within dataset v1 controls")
    axes[1].set_title("Selected model fit diagnostics")
    fig.suptitle("Same features and participant splits; validation used for selection")
    fig.savefig(Path(output) / "refinement_metrics.png", dpi=160)
    plt.close(fig)


if __name__ == "__main__":
    main()
