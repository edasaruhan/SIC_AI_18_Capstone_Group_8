"""Run with: python -m src.models.synthetic_demo

Only a deterministic smoke test, not participant/time evaluation. All labels,
thresholds and next-window values below are artificial, not a proxy definition.
"""

import json

import numpy as np
import pandas as pd

from . import LogisticRegressionBaseline, RuleBaseline, ThresholdRule


def make_synthetic_data(n: int = 48, seed: int = 42):
    """Return current-window X and a separately generated next-window toy target."""
    if n < 8:
        raise ValueError("Use at least 8 rows for the two-class smoke example")
    rng = np.random.default_rng(seed)
    # Alternating toy regimes guarantee both classes; these are not real users.
    elevated = np.arange(n) % 2
    active = np.clip(240 + 1000 * elevated + rng.normal(0, 50, n), 0, 1800)
    previous = np.clip(active + rng.normal(0, 80, n), 0, None)
    deviation = active - 600
    previous[::5] = np.nan
    deviation[::5] = np.nan  # Illustrative lack of personal history.
    frame = pd.DataFrame({
        "screen_active_seconds": active,
        "unlock_count": rng.integers(0, 3, n) + 5 * elevated,
        "app_switch_count": rng.integers(0, 5, n) + 8 * elevated,
        "previous_session_duration_seconds": previous,
        "hour_of_day": np.full(n, 12),
        "day_of_week": np.arange(n) % 7,
        "night_activity_seconds": np.zeros(n),
        "screen_active_deviation_seconds": deviation,
    })
    # Next-window observations are NOT added to X. This arbitrary threshold is
    # local to this synthetic example and never becomes a model/default label rule.
    next_active = np.clip(200 + 1100 * elevated + rng.normal(0, 60, n), 0, 1800)
    target = pd.Series((next_active >= 900).astype(int), name="synthetic_next_label")
    return frame, target


def main():
    train_X, train_y = make_synthetic_data()
    probe_X, _ = make_synthetic_data(8, seed=7)
    # Explicit demo-only values, not empirically selected real-data thresholds.
    models = {
        "rule": RuleBaseline([
            ThresholdRule("screen_active_seconds", 900),
            ThresholdRule("unlock_count", 5),
        ]),
        "logistic_regression": LogisticRegressionBaseline(),
    }
    output = {"synthetic_only": True, "training_rows": len(train_X),
              "prediction_rows": len(probe_X), "models": {}}
    for name, model in models.items():
        model.fit(train_X, train_y)
        scores = model.predict_scores(probe_X)
        predictions = model.predict(probe_X)
        if not np.isfinite(scores).all() or predictions.shape != (len(probe_X),):
            raise RuntimeError("Smoke test produced invalid predictions")
        output["models"][name] = {
            "score_kind": "rule_match_fraction" if name == "rule" else "P(next_label=1)",
            "scores": scores.round(4).tolist(), "predictions": predictions.tolist(),
        }
    print(json.dumps(output, indent=2))


if __name__ == "__main__":
    main()
