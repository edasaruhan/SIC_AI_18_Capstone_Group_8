"""ONNX export regressions with small, synthetic training data."""

import numpy as np
import pandas as pd
import pytest

from src.models.dataset_v1 import V1_FEATURES
from src.models.mobile_export import NAMES, build_graph, compare_scores, runtime_session
from src.models.tree_models import XGBoostModel


@pytest.fixture(autouse=True)
def onnx_dependencies():
    for package in ("onnx", "onnxruntime", "onnxmltools"):
        pytest.importorskip(package)


@pytest.mark.parametrize("training_has_missing", [False, True])
def test_onnx_score_preserves_imputation_and_fitted_indicators(training_has_missing, tmp_path):
    base = [180.25, 2, 120.125, 0, 40, 12, 3]
    X = pd.DataFrame([base] * 64, columns=NAMES, dtype=float)
    labels = np.arange(len(X)) % 2
    if training_has_missing:
        X["app_switches"] = np.nan
        X.loc[labels == 1, "prev_session_s"] = np.nan
    else:
        X.loc[labels == 1, "prev_session_s"] = 80
    model = XGBoostModel(
        features=V1_FEATURES, decision_threshold=0.25,
        n_estimators=8, max_depth=2, learning_rate=0.3, min_child_weight=1, subsample=1,
    ).fit(X, labels)
    exported, preprocessing = build_graph(model)
    if training_has_missing:
        assert preprocessing["indicatorIndices"] == [3, 4]
        assert preprocessing["statistics"][3:5] == [0.0, 40.0]
        assert preprocessing["classifierColumns"][-2:] == [
            "missingindicator_app_switches", "missingindicator_prev_session_s"]
    else:
        assert preprocessing["indicatorIndices"] == []
        assert preprocessing["statistics"][3:5] == [0.0, 60.0]

    observed = [*base]
    observed[4] = preprocessing["statistics"][4]
    frame = pd.DataFrame([observed, base, base, [np.nan] * len(NAMES)], columns=NAMES, dtype=float)
    frame.loc[1, "prev_session_s"] = np.nan
    frame.loc[2, "app_switches"] = np.nan
    export_path = tmp_path / "model.onnx"
    export_path.write_bytes(exported.SerializeToString())
    runtime = runtime_session(export_path.read_bytes())
    for single_row in (False, True):
        report, scores, decisions = compare_scores(model, runtime, frame, single_row=single_row)
        assert report["rows"] == len(frame)
        assert report["decisionMismatches"] == 0
        if training_has_missing:
            assert scores[1] > scores[0]
            assert decisions[:2].tolist() == [0, 1]
        else:
            assert scores[1] == scores[0]
