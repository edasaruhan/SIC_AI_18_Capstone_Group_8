"""Model behavior and personal sample-weight support, independent of real data."""

import joblib
import numpy as np
import pandas as pd
import pytest
from sklearn.exceptions import NotFittedError

from src.models import FeatureSpec, RandomForestModel, XGBoostModel


FEATURES = (FeatureSpec("usage", 0), FeatureSpec("history", 0))


def make_model(kind, **kwargs):
    if kind == "rf":
        return RandomForestModel(features=FEATURES, n_estimators=8,
                                 min_samples_leaf=1, class_weight=None, **kwargs)
    return XGBoostModel(features=FEATURES, n_estimators=8, min_child_weight=1, **kwargs)


@pytest.mark.parametrize("kind", ["rf", "xgb"])
def test_training_order_missing_values_weights_and_roundtrip(kind, tmp_path):
    X = pd.DataFrame({"usage": [0., 10., 20., 30., np.nan, 50.], "history": np.nan})
    y = pd.Series([0, 0, 0, 1, 1, 1])
    model = make_model(kind)
    with pytest.raises(NotFittedError):
        model.predict(X)
    before = X.copy(deep=True)
    model.fit(X, y, sample_weight=pd.Series([1., 1., 1., 2., 2., 2.]))
    scores = model.predict_scores(X)
    assert scores.shape == (6,) and np.isfinite(scores).all()
    np.testing.assert_allclose(model.predict_proba(X).sum(axis=1), 1, atol=1e-6)
    np.testing.assert_allclose(model.predict_scores(X.iloc[:, ::-1].assign(y_next_any=999)), scores)
    np.testing.assert_allclose(model.pipeline_.named_steps["imputer"].statistics_, [20., 0.])
    stats = model.pipeline_.named_steps["imputer"].statistics_.copy()
    model.predict(pd.DataFrame({"usage": [1000., np.nan], "history": [2., np.nan]}))
    np.testing.assert_array_equal(stats, model.pipeline_.named_steps["imputer"].statistics_)
    model.decision_threshold = 0.7
    np.testing.assert_array_equal(model.predict(X), scores >= 0.7)
    path = tmp_path / "model.joblib"
    joblib.dump(model, path)
    np.testing.assert_array_equal(joblib.load(path).predict_scores(X), scores)
    pd.testing.assert_frame_equal(X, before)


@pytest.mark.parametrize("kind", ["rf", "xgb"])
def test_sample_weights_actually_change_fitted_predictions(kind):
    # Identical features and conflicting labels isolate weighting from features.
    X = pd.DataFrame({"usage": np.zeros(40), "history": np.nan})
    y = np.tile([0, 1], 20)
    uniform = make_model(kind).fit(X, y)
    weighted = make_model(kind).fit(X, y, sample_weight=np.where(y == 1, 8., 1.))
    assert weighted.predict_scores(X).mean() > uniform.predict_scores(X).mean() + 0.1


@pytest.mark.parametrize("weights", [[1, 1], [0]*4, [1, -1, 1, 1],
                                     [1, np.nan, 1, 1], [True]*4,
                                     [[1]]*4, [1, 0, 1, 0]])
def test_bad_or_single_class_effective_weights_rejected(weights):
    X = pd.DataFrame({"usage": [0., 1., 2., 3.], "history": np.nan})
    with pytest.raises(ValueError):
        make_model("rf").fit(X, [0, 1, 0, 1], sample_weight=weights)


def test_weight_alignment_and_single_class_are_explicit():
    X = pd.DataFrame({"usage": [0., 1., 2., 3.], "history": np.nan})
    with pytest.raises(ValueError, match="index"):
        make_model("rf").fit(X, [0, 1, 0, 1], sample_weight=pd.Series([1.]*4, index=[3, 2, 1, 0]))
    with pytest.raises(ValueError, match="both target classes"):
        make_model("xgb").fit(X, [0]*4)
