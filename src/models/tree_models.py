"""Model-side tree classifiers; labels and participant splits belong to callers."""

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline

from .baselines import _finite_number, _require_fitted, _target
from .features import DEFAULT_FEATURES, prepare_features, validate_schema


def validate_sample_weight(sample_weight, frame):
    """Keep personal weights aligned with training rows; never weight test scores."""
    if sample_weight is None:
        return None
    if isinstance(sample_weight, pd.Series) and not sample_weight.index.equals(frame.index):
        raise ValueError("sample_weight index must match X index")
    raw = np.asarray(sample_weight)
    if raw.dtype.kind not in "iuf" or raw.shape != (len(frame),):
        raise ValueError("sample_weight must be a numeric vector matching X")
    weights = raw.astype(float)
    if not np.isfinite(weights).all() or (weights < 0).any() or not (weights > 0).any():
        raise ValueError("sample_weight must be finite, nonnegative and not all zero")
    return weights


class _TreeModel:
    def _configure(self, features, decision_threshold, params):
        self.features = validate_schema(features)
        self.decision_threshold = _finite_number(decision_threshold, "decision_threshold")
        if not 0 <= self.decision_threshold <= 1:
            raise ValueError("decision_threshold must be between 0 and 1")
        self.params = params

    def fit(self, X, y, sample_weight=None):
        frame = prepare_features(X, self.features)
        labels = _target(y, frame, both_classes=True)
        weights = validate_sample_weight(sample_weight, frame)
        if weights is not None and any(weights[labels == c].sum() == 0 for c in (0, 1)):
            raise ValueError("Both classes must have positive training weight")
        # All preprocessing is learned inside fit, exclusively from these rows.
        pipeline = Pipeline([
            ("imputer", SimpleImputer(strategy="median", add_indicator=True,
                                      keep_empty_features=True)),
            ("classifier", self._classifier()),
        ])
        pipeline.fit(frame, labels, classifier__sample_weight=weights)
        self.pipeline_ = pipeline
        return self

    def predict_proba(self, X):
        _require_fitted(self, "pipeline_")
        return self.pipeline_.predict_proba(prepare_features(X, self.features))

    def predict_scores(self, X):
        return self.predict_proba(X)[:, 1]

    def predict(self, X):
        return (self.predict_scores(X) >= self.decision_threshold).astype(int)


class RandomForestModel(_TreeModel):
    def __init__(self, *, features=DEFAULT_FEATURES, decision_threshold=0.5,
                 n_estimators=100, max_depth=8, min_samples_leaf=5,
                 class_weight="balanced", random_state=42, n_jobs=1):
        self._configure(features, decision_threshold, dict(
            n_estimators=n_estimators, max_depth=max_depth,
            min_samples_leaf=min_samples_leaf, class_weight=class_weight,
            random_state=random_state, n_jobs=n_jobs,
        ))

    def _classifier(self):
        return RandomForestClassifier(**self.params)


class XGBoostModel(_TreeModel):
    def __init__(self, *, features=DEFAULT_FEATURES, decision_threshold=0.5,
                 n_estimators=150, max_depth=3, learning_rate=0.05,
                 min_child_weight=5, subsample=0.8, colsample_bytree=1.0,
                 reg_lambda=1.0, scale_pos_weight=1.0, random_state=42, n_jobs=1):
        self._configure(features, decision_threshold, dict(
            objective="binary:logistic", eval_metric="logloss", tree_method="hist",
            n_estimators=n_estimators, max_depth=max_depth, learning_rate=learning_rate,
            min_child_weight=min_child_weight, subsample=subsample,
            colsample_bytree=colsample_bytree, reg_lambda=reg_lambda,
            scale_pos_weight=scale_pos_weight, random_state=random_state, n_jobs=n_jobs,
        ))

    def _classifier(self):
        # Lazy import keeps the original baselines usable without XGBoost/OpenMP.
        from xgboost import XGBClassifier
        return XGBClassifier(**self.params)
