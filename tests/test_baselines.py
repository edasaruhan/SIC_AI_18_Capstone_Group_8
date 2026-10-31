"""Baseline model tests only; no participant splitting or evaluation framework."""

import numpy as np
import pandas as pd
import pytest
from sklearn.exceptions import NotFittedError

from src.models import (
    DEFAULT_FEATURES, FeatureSpec, LogisticRegressionBaseline, RuleBaseline,
    ThresholdRule, prepare_features,
)
from src.models.synthetic_demo import make_synthetic_data


@pytest.fixture
def data():
    return make_synthetic_data()


def make_rule(**kwargs):
    return RuleBaseline([
        ThresholdRule("screen_active_seconds", 900),
        ThresholdRule("unlock_count", 5),
    ], **kwargs)


@pytest.mark.parametrize("factory", [make_rule, LogisticRegressionBaseline])
def test_training_prediction_and_named_column_order(data, factory):
    X, y = data
    model = factory().fit(X, y)
    probe, _ = make_synthetic_data(8, seed=7)
    before = probe.copy(deep=True)
    scores = model.predict_scores(probe)
    labels = model.predict(probe)
    assert scores.shape == labels.shape == (8,)
    assert np.isfinite(scores).all() and ((0 <= scores) & (scores <= 1)).all()
    assert set(labels) <= {0, 1}
    # Explicit feature selection ignores metadata and a deliberately wrong target.
    extra = probe.iloc[:, ::-1].assign(participant_id="not_a_feature", target=999)
    np.testing.assert_allclose(model.predict_scores(extra), scores)
    pd.testing.assert_frame_equal(probe, before)


def test_fixed_rule_boundary_votes_and_score_meaning(data):
    X, y = data
    probe = X.iloc[:4].copy()
    probe["screen_active_seconds"] = [0, 899, 900, 1000]
    probe["unlock_count"] = [0, 4, 4, 5]
    np.testing.assert_array_equal(make_rule().fit(X, y).predict(probe), [0, 0, 1, 1])
    strict = make_rule(min_matches=2).fit(X, 1 - y)
    np.testing.assert_array_equal(strict.predict(probe), [0, 0, 0, 1])
    np.testing.assert_allclose(strict.predict_scores(probe), [0, 0, 0.5, 1])
    # Labels do not tune fixed thresholds.
    np.testing.assert_allclose(
        make_rule().fit(X, y).predict_scores(probe),
        make_rule().fit(X, 1 - y).predict_scores(probe),
    )


@pytest.mark.parametrize("factory", [make_rule, LogisticRegressionBaseline])
def test_prediction_requires_fit(data, factory):
    X, _ = data
    with pytest.raises(NotFittedError):
        factory().predict(X)


def test_custom_names_and_thresholds():
    features = (FeatureSpec("usage", 0, 1800), FeatureSpec("checks", 0, integer=True))
    X = pd.DataFrame({"checks": [0, 1, 5, 8], "usage": [0, 100, 1000, 1500]})
    y = [0, 0, 1, 1]
    rule = RuleBaseline([ThresholdRule("usage", 1000)], features=features).fit(X, y)
    np.testing.assert_array_equal(rule.predict(X), [0, 0, 1, 1])
    lr = LogisticRegressionBaseline(features=features, decision_threshold=0.7).fit(X, y)
    np.testing.assert_array_equal(lr.predict(X), (lr.predict_scores(X) >= 0.7).astype(int))
    np.testing.assert_allclose(lr.predict_proba(X).sum(axis=1), 1)
    np.testing.assert_allclose(lr.predict_scores(X), lr.predict_proba(X)[:, 1])


def test_imputer_scaler_use_training_statistics_only():
    features = (FeatureSpec("usage", 0),)
    X = pd.DataFrame({"usage": [0.0, 10.0, np.nan, 30.0]})
    model = LogisticRegressionBaseline(features=features).fit(X, [0, 0, 1, 1])
    imputer = model.pipeline_.named_steps["imputer"]
    scaler = model.pipeline_.named_steps["scaler"]
    np.testing.assert_allclose(imputer.statistics_, [10.0])
    np.testing.assert_allclose(scaler.mean_, [12.5, 0.25])
    medians, means = imputer.statistics_.copy(), scaler.mean_.copy()
    model.predict(pd.DataFrame({"usage": [500.0, np.nan]}))
    np.testing.assert_array_equal(imputer.statistics_, medians)
    np.testing.assert_array_equal(scaler.mean_, means)


def test_all_missing_personal_history_is_retained(data):
    X, y = data
    X = X.copy()
    cold = ["previous_session_duration_seconds", "screen_active_deviation_seconds"]
    X[cold] = np.nan
    model = LogisticRegressionBaseline().fit(X, y)
    assert model.pipeline_.named_steps["imputer"].n_features_in_ == 8
    assert model.pipeline_.named_steps["imputer"].transform(prepare_features(X)).shape == (48, 10)
    assert np.isfinite(model.predict_scores(X)).all()
    # Later observed values can be transformed without changing feature dimensions.
    warm, _ = make_synthetic_data(8, seed=7)
    assert model.predict(warm).shape == (8,)
    assert make_rule().fit(X, y).predict(X).shape == (48,)


def test_unknown_rule_input_is_not_silently_negative(data):
    X, y = data
    model = make_rule().fit(X, y)
    unknown = X.iloc[:1].copy()
    unknown["unlock_count"] = np.nan
    with pytest.raises(ValueError, match="Missing rule input"):
        model.predict(unknown)
    with pytest.raises(ValueError, match="Missing rule input"):
        make_rule().fit(unknown)


@pytest.mark.parametrize("name,value", [
    ("screen_active_seconds", 1801), ("unlock_count", -1),
    ("unlock_count", 1.5), ("unlock_count", True),
    ("unlock_count", "4"), ("hour_of_day", 24), ("day_of_week", 7),
    ("previous_session_duration_seconds", float("inf")),
    ("screen_active_deviation_seconds", -1801),
    ("screen_active_deviation_seconds", 1801),
])
def test_invalid_feature_values_raise(data, name, value):
    X, _ = data
    invalid = X.iloc[:1].copy().astype(object)
    invalid[name] = value
    with pytest.raises(ValueError, match=name):
        prepare_features(invalid)


def test_cross_feature_bound_and_long_previous_session(data):
    X, _ = data
    invalid = X.iloc[:1].copy()
    invalid["night_activity_seconds"] = invalid["screen_active_seconds"] + 1
    with pytest.raises(ValueError, match="exceeds"):
        prepare_features(invalid)
    invalid["night_activity_seconds"] = 0
    invalid["previous_session_duration_seconds"] = 3600
    assert prepare_features(invalid)["previous_session_duration_seconds"].iloc[0] == 3600


@pytest.mark.parametrize("factory", [make_rule, LogisticRegressionBaseline])
def test_missing_duplicate_empty_or_unnamed_input(data, factory):
    X, y = data
    model = factory().fit(X, y)
    with pytest.raises(ValueError, match="Missing feature"):
        model.predict(X.drop(columns="unlock_count"))
    with pytest.raises(ValueError, match="duplicate"):
        model.predict(pd.concat([X, X[["unlock_count"]]], axis=1))
    with pytest.raises(ValueError, match="at least one row"):
        model.predict(X.iloc[:0])
    with pytest.raises(TypeError, match="DataFrame"):
        model.predict(X.to_numpy())


def test_null_representations_and_no_input_mutation(data):
    X, _ = data
    frame = X.iloc[:3].copy().astype(object)
    frame["previous_session_duration_seconds"] = [None, pd.NA, np.nan]
    before = frame.copy(deep=True)
    result = prepare_features(frame)
    assert result["previous_session_duration_seconds"].isna().all()
    pd.testing.assert_frame_equal(frame, before)


def test_single_class_length_and_index_alignment(data):
    X, y = data
    with pytest.raises(ValueError, match="both target classes"):
        LogisticRegressionBaseline().fit(X, np.zeros(len(X)))
    # A fixed rule needs no learned decision boundary, so a single-class y is OK.
    make_rule().fit(X, np.zeros(len(X)))
    for labels in [y.iloc[:-1], y.to_numpy().reshape(-1, 1)]:
        with pytest.raises(ValueError):
            LogisticRegressionBaseline().fit(X, labels)
    with pytest.raises(ValueError, match="index"):
        LogisticRegressionBaseline().fit(X, y.iloc[::-1])


@pytest.mark.parametrize("bad", [None, np.nan, -1, 2, "1", True])
def test_invalid_target_is_not_imputed(data, bad):
    X, y = data
    invalid = y.astype(object)
    invalid.iloc[0] = bad
    with pytest.raises(ValueError, match="labels 0 and 1"):
        LogisticRegressionBaseline().fit(X, invalid)


@pytest.mark.parametrize("kwargs", [
    {"decision_threshold": -0.1}, {"decision_threshold": 1.1},
    {"decision_threshold": np.nan}, {"C": 0}, {"max_iter": 0},
])
def test_invalid_logistic_configuration(kwargs):
    with pytest.raises(ValueError):
        LogisticRegressionBaseline(**kwargs)


def test_rule_and_schema_configuration():
    with pytest.raises(ValueError):
        RuleBaseline([])
    with pytest.raises(ValueError):
        RuleBaseline([ThresholdRule("unknown", 1)])
    with pytest.raises(ValueError):
        RuleBaseline([ThresholdRule("unlock_count", np.nan)])
    with pytest.raises(ValueError):
        make_rule(min_matches=0)
    with pytest.raises(ValueError):
        LogisticRegressionBaseline(features=(DEFAULT_FEATURES[0], DEFAULT_FEATURES[0]))
    with pytest.raises(ValueError):
        LogisticRegressionBaseline(features=(FeatureSpec("bad", 2, 1),))


def test_deterministic_logistic_training(data):
    X, y = data
    left = LogisticRegressionBaseline().fit(X, y)
    right = LogisticRegressionBaseline().fit(X, y)
    np.testing.assert_allclose(left.predict_scores(X), right.predict_scores(X))
