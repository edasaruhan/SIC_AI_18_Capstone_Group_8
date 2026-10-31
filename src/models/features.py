"""Model-side validation of already extracted features, not event preprocessing."""

from dataclasses import dataclass
from numbers import Real
from typing import Sequence

import numpy as np
import pandas as pd


@dataclass(frozen=True)
class FeatureSpec:
    """Configurable column name and numeric constraints; null values are allowed."""

    name: str
    minimum: float | None = None
    maximum: float | None = None
    integer: bool = False
    upper_bound_feature: str | None = None


DEFAULT_FEATURES = (
    FeatureSpec("screen_active_seconds", 0, 1800),
    FeatureSpec("unlock_count", 0, integer=True),
    FeatureSpec("app_switch_count", 0, integer=True),
    FeatureSpec("previous_session_duration_seconds", 0),
    FeatureSpec("hour_of_day", 0, 23, integer=True),
    FeatureSpec("day_of_week", 0, 6, integer=True),
    FeatureSpec("night_activity_seconds", 0, 1800,
                upper_bound_feature="screen_active_seconds"),
    FeatureSpec("screen_active_deviation_seconds", -1800, 1800),
)


def validate_schema(features: Sequence[FeatureSpec]) -> tuple[FeatureSpec, ...]:
    specs = tuple(features)
    if not specs or not all(isinstance(spec, FeatureSpec) for spec in specs):
        raise ValueError("features must contain at least one FeatureSpec")
    names = [spec.name for spec in specs]
    if any(not isinstance(name, str) or not name.strip() for name in names):
        raise ValueError("Feature names must be non-empty strings")
    if len(names) != len(set(names)):
        raise ValueError("Feature names must be unique")
    for spec in specs:
        for bound in (spec.minimum, spec.maximum):
            if bound is not None and (
                isinstance(bound, (bool, np.bool_))
                or not isinstance(bound, Real) or not np.isfinite(bound)
            ):
                raise ValueError("Feature bounds must be finite numbers")
        if (spec.minimum is not None and spec.maximum is not None
                and spec.minimum > spec.maximum):
            raise ValueError("Feature minimum cannot exceed maximum")
        if spec.upper_bound_feature is not None and (
            spec.upper_bound_feature not in names
            or spec.upper_bound_feature == spec.name
        ):
            raise ValueError("upper_bound_feature must name another selected feature")
    return specs


def prepare_features(
    X: pd.DataFrame, features: Sequence[FeatureSpec] = DEFAULT_FEATURES,
) -> pd.DataFrame:
    """Select/reorder named columns; reject invalid values rather than repair them.

    Extra metadata/target columns are ignored, never implicitly used as predictors.
    None, pd.NA and NaN remain missing. No imputation is performed here.
    """
    specs = validate_schema(features)
    if not isinstance(X, pd.DataFrame):
        raise TypeError("X must be a pandas DataFrame with named feature columns")
    if X.empty:
        raise ValueError("X must contain at least one row")
    if not X.columns.is_unique:
        raise ValueError("X must not have duplicate columns")
    names = [spec.name for spec in specs]
    missing = [name for name in names if name not in X.columns]
    if missing:
        raise ValueError(f"Missing feature columns: {missing}")
    result = pd.DataFrame(index=X.index)
    for spec in specs:
        values = []
        for value in X[spec.name]:
            if value is None or value is pd.NA:
                values.append(np.nan)
                continue
            if isinstance(value, (bool, np.bool_)) or not isinstance(value, Real):
                raise ValueError(f"{spec.name}: expected a numeric value or null")
            value = float(value)
            if np.isnan(value):
                values.append(np.nan)
                continue
            if not np.isfinite(value):
                raise ValueError(f"{spec.name}: infinity is not allowed")
            if spec.integer and not value.is_integer():
                raise ValueError(f"{spec.name}: expected an integer")
            if spec.minimum is not None and value < spec.minimum:
                raise ValueError(f"{spec.name}: value below {spec.minimum}")
            if spec.maximum is not None and value > spec.maximum:
                raise ValueError(f"{spec.name}: value above {spec.maximum}")
            values.append(value)
        result[spec.name] = values
    for spec in specs:
        if spec.upper_bound_feature is not None:
            if (result[spec.name] > result[spec.upper_bound_feature]).any():
                raise ValueError(f"{spec.name}: exceeds {spec.upper_bound_feature}")
    return result
