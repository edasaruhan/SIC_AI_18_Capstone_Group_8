"""Two baselines sharing fit(X, y), predict_scores(X), predict(X).

Labels are supplied by the caller and must refer to the NEXT window. This module
does not derive proxy labels, split participants/time or implement app inference.
"""

from dataclasses import dataclass
from numbers import Real
from typing import Sequence

import numpy as np
import pandas as pd
from sklearn.impute import SimpleImputer
from sklearn.exceptions import NotFittedError
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from .features import DEFAULT_FEATURES, FeatureSpec, prepare_features, validate_schema


def _require_fitted(model, attribute: str) -> None:
    # These lightweight wrappers are not sklearn estimators/meta-estimators.
    if not hasattr(model, attribute):
        raise NotFittedError(f"Call fit before predicting with {type(model).__name__}")


def _finite_number(value: float, name: str) -> float:
    if (isinstance(value, (bool, np.bool_)) or not isinstance(value, Real)
            or not np.isfinite(value)):
        raise ValueError(f"{name} must be a finite number")
    return float(value)


def _target(y, X: pd.DataFrame, *, both_classes: bool) -> np.ndarray:
    if isinstance(y, pd.Series) and not y.index.equals(X.index):
        raise ValueError("y index must match X index in the same order")
    labels = np.asarray(y)
    if labels.ndim != 1 or len(labels) != len(X):
        raise ValueError("y must be one-dimensional and match X row count")
    for value in labels:
        if (isinstance(value, (bool, np.bool_)) or not isinstance(value, Real)
                or not np.isfinite(value) or value not in (0, 1)):
            raise ValueError("y must contain only observed numeric labels 0 and 1")
    labels = labels.astype(int)
    if both_classes and len(np.unique(labels)) != 2:
        raise ValueError("Logistic Regression requires both target classes (0 and 1)")
    return labels


@dataclass(frozen=True)
class ThresholdRule:
    """A current-window feature >= an explicitly supplied threshold."""

    feature: str
    threshold: float


class RuleBaseline:
    """Fixed rules anticipating the next window, not a proxy-label generator.

    Score = fraction of configured rules met; it is NOT a probability.
    At least min_matches rules must hold for a positive decision. Missing values
    in rule inputs raise an error (unknown does not mean negative). Missing values
    in other features, including personal cold-start history, are allowed.
    """

    def __init__(
        self, rules: Sequence[ThresholdRule], *,
        features: Sequence[FeatureSpec] = DEFAULT_FEATURES, min_matches: int = 1,
    ):
        self.features = validate_schema(features)
        self.rules = tuple(rules)
        if not self.rules or not all(isinstance(r, ThresholdRule) for r in self.rules):
            raise ValueError("At least one ThresholdRule is required")
        if type(min_matches) is not int or not 1 <= min_matches <= len(self.rules):
            raise ValueError("min_matches must be between 1 and the number of rules")
        self.min_matches = min_matches
        names = {spec.name for spec in self.features}
        for rule in self.rules:
            if rule.feature not in names:
                raise ValueError(f"Rule feature not selected: {rule.feature}")
            _finite_number(rule.threshold, "Rule threshold")

    def _matches(self, X: pd.DataFrame) -> np.ndarray:
        frame = prepare_features(X, self.features)
        columns = [rule.feature for rule in self.rules]
        if frame[columns].isna().any().any():
            raise ValueError("Missing rule input: no negative decision can be inferred")
        return np.column_stack([
            frame[rule.feature].to_numpy() >= rule.threshold for rule in self.rules
        ])

    def fit(self, X: pd.DataFrame, y=None):
        self._matches(X)
        if y is not None:
            _target(y, X, both_classes=False)
        self.fitted_ = True
        return self

    def predict_scores(self, X: pd.DataFrame) -> np.ndarray:
        _require_fitted(self, "fitted_")
        return self._matches(X).mean(axis=1)

    def predict(self, X: pd.DataFrame) -> np.ndarray:
        _require_fitted(self, "fitted_")
        return (self._matches(X).sum(axis=1) >= self.min_matches).astype(int)


def build_logistic_pipeline(
    *, C: float = 1.0, max_iter: int = 1000, random_state: int = 42,
) -> Pipeline:
    """Training-only median imputation, missing indicators, scaling, classifier.

    Entirely missing training columns stay in the model: the imputer fills these
    with zero and adds an indicator. This is a numerical placeholder, not observed
    non-use. Other missing values use training medians. No test statistics are fit.
    """
    C = _finite_number(C, "C")
    if C <= 0:
        raise ValueError("C must be positive")
    if type(max_iter) is not int or max_iter < 1:
        raise ValueError("max_iter must be a positive integer")
    return Pipeline([
        ("imputer", SimpleImputer(strategy="median", keep_empty_features=True,
                                  add_indicator=True)),
        ("scaler", StandardScaler()),
        ("classifier", LogisticRegression(C=C, max_iter=max_iter,
                                           random_state=random_state)),
    ])


class LogisticRegressionBaseline:
    """Configurable feature schema and decision threshold; no label thresholds.

    predict_scores returns P(y_next=1); predict_proba returns [P(0), P(1)].
    A classification threshold is separate from the upstream proxy definition.
    Caller-provided labels are not generated or thresholded by this model.
    """

    def __init__(
        self, *, features: Sequence[FeatureSpec] = DEFAULT_FEATURES,
        decision_threshold: float = 0.5, C: float = 1.0, max_iter: int = 1000,
        random_state: int = 42,
    ):
        self.features = validate_schema(features)
        self.decision_threshold = _finite_number(decision_threshold, "decision_threshold")
        if not 0 <= self.decision_threshold <= 1:
            raise ValueError("decision_threshold must be between 0 and 1")
        self.C = C
        self.max_iter = max_iter
        self.random_state = random_state
        # Fail on invalid training configuration before receiving any data.
        build_logistic_pipeline(C=C, max_iter=max_iter, random_state=random_state)

    def fit(self, X: pd.DataFrame, y):
        frame = prepare_features(X, self.features)
        labels = _target(y, frame, both_classes=True)
        pipeline = build_logistic_pipeline(
            C=self.C, max_iter=self.max_iter, random_state=self.random_state,
        )
        pipeline.fit(frame, labels)
        self.pipeline_ = pipeline
        return self

    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        _require_fitted(self, "pipeline_")
        return self.pipeline_.predict_proba(prepare_features(X, self.features))

    def predict_scores(self, X: pd.DataFrame) -> np.ndarray:
        return self.predict_proba(X)[:, 1]

    def predict(self, X: pd.DataFrame) -> np.ndarray:
        return (self.predict_scores(X) >= self.decision_threshold).astype(int)
