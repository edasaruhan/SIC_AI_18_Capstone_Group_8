"""Validate model decision thresholds without inspecting held-out test users.

Run: python -m src.models.threshold_exploration
Reuses the existing full-data window table. This is a model exploration extension,
not the team's final evaluation infrastructure. No classifier hyperparameter search.
"""

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.metrics import f1_score
from sklearn.model_selection import GroupKFold, GroupShuffleSplit

from .features import prepare_features
from .research_data import RESEARCH_FEATURES
from .real_data_exploration import (
    fit_proxy_thresholds, model_candidates, model_metrics, plot_results, proxy_labels,
)


DECISION_GRID = tuple(np.round(np.arange(0.05, 1.0, 0.05), 2))


def select_decision_threshold(y_validation, scores, candidates=DECISION_GRID):
    """Maximize validation F1; break exact ties in favor of the higher threshold.

    Only validation labels/scores enter this function. Proxy-label cutoffs are a
    separate operation. This criterion is a research choice, not an alert budget.
    """
    y = np.asarray(y_validation)
    scores = np.asarray(scores, dtype=float)
    if (y.ndim != 1 or scores.shape != y.shape or not len(y)
            or not np.isin(y, [0, 1]).all() or len(np.unique(y)) != 2):
        raise ValueError("Validation needs matching 1-D scores and both binary classes")
    if not np.isfinite(scores).all() or ((scores < 0) | (scores > 1)).any():
        raise ValueError("Validation scores must be finite and within [0, 1]")
    grid = np.asarray(candidates, dtype=float)
    if grid.ndim != 1 or not len(grid) or not np.isfinite(grid).all() or ((grid <= 0) | (grid >= 1)).any():
        raise ValueError("Candidate thresholds must be strictly between 0 and 1")
    rows = [{"threshold": float(t),
             "validation_f1": float(f1_score(y, scores >= t, zero_division=0))}
            for t in grid]
    best = max(rows, key=lambda row: (row["validation_f1"], row["threshold"]))
    return best["threshold"], rows


def participant_splits(table, folds=3, validation_fraction=0.2, seed=42):
    """Disjoint fitting/validation/test users; selection uses IDs, never labels."""
    if not 0 < validation_fraction < 1:
        raise ValueError("validation_fraction must be between 0 and 1")
    outer = GroupKFold(n_splits=folds)
    for fold, (development_idx, test_idx) in enumerate(
        outer.split(table, groups=table["participant_id"]), 1,
    ):
        development = table.iloc[development_idx]
        inner = GroupShuffleSplit(n_splits=1, test_size=validation_fraction, random_state=seed)
        fitting, validation = next(inner.split(development, groups=development["participant_id"]))
        train_idx, val_idx = development_idx[fitting], development_idx[validation]
        groups = [set(table.iloc[idx]["participant_id"]) for idx in (train_idx, val_idx, test_idx)]
        if any(groups[i] & groups[j] for i, j in ((0, 1), (0, 2), (1, 2))):
            raise RuntimeError("Participant leakage across fitting/validation/test")
        yield fold, train_idx, val_idx, test_idx


def compare_thresholds(table, folds=3, validation_fraction=0.2, seed=42, quantile=0.9):
    results, predictions, split_details, curves, per_user = [], [], [], [], []
    for fold, train_idx, val_idx, test_idx in participant_splits(
        table, folds, validation_fraction, seed,
    ):
        train, validation, test = (table.iloc[idx] for idx in (train_idx, val_idx, test_idx))
        # Even the operational proxy cutoffs are learned only on fitting users.
        proxy = fit_proxy_thresholds(train, quantile)
        y_train, y_val = proxy_labels(train, proxy), proxy_labels(validation, proxy)
        if y_train.nunique() != 2 or y_val.nunique() != 2:
            raise ValueError("Fitting and validation sets must each contain both proxy classes")
        train_X = prepare_features(train, RESEARCH_FEATURES)
        val_X = prepare_features(validation, RESEARCH_FEATURES)
        test_X = prepare_features(test, RESEARCH_FEATURES)
        split_details.append({"fold": fold, **{
            name: sorted(frame["participant_id"].unique().tolist())
            for name, frame in [("fitting", train), ("validation", validation), ("test", test)]
        }})
        for name, model in model_candidates(proxy).items():
            model.fit(train_X, y_train)
            if name == "Rule":
                policies = [("fixed_rule", None)]
            else:
                val_scores = (model.predict_scores(val_X) if hasattr(model, "predict_scores")
                              else model.predict_proba(val_X)[:, 1])
                threshold, curve = select_decision_threshold(y_val, val_scores)
                curves.append({"fold": fold, "model": name, "candidates": curve})
                policies = [("default_0.50", 0.5), ("validation_f1", threshold)]
            # The frozen fitted model is NOT refitted after threshold selection:
            # 0.50 and selected-cutoff test predictions use identical scores.
            scores = (model.predict_scores(test_X) if hasattr(model, "predict_scores")
                      else model.predict_proba(test_X)[:, 1])
            y_test = proxy_labels(test, proxy)
            for policy, threshold in policies:
                predicted = model.predict(test_X) if threshold is None else (scores >= threshold).astype(int)
                results.append({
                    "fold": fold, "model": name, "policy": policy,
                    "decision_threshold": threshold, "proxy_thresholds": proxy,
                    "training_users": train["participant_id"].nunique(),
                    "validation_users": validation["participant_id"].nunique(),
                    "test_users": test["participant_id"].nunique(),
                    "training_rows": len(train), "validation_rows": len(validation),
                    "test_rows": len(test), **model_metrics(y_test, predicted, scores, test),
                })
                part = test[["participant_id", "window_start_ms", "window_end_ms"]].copy()
                part["model"], part["policy"], part["fold"] = name, policy, fold
                part["y"], part["prediction"], part["score"] = y_test, predicted, scores
                predictions.append(part)
                for participant, group in part.groupby("participant_id"):
                    per_user.append({
                        "participant_id": participant, "fold": fold, "model": name, "policy": policy,
                        **model_metrics(group["y"], group["prediction"].to_numpy(), group["score"].to_numpy(), group),
                    })
        print(f"Threshold fold {fold}: {train['participant_id'].nunique()} fit / "
              f"{validation['participant_id'].nunique()} validation / "
              f"{test['participant_id'].nunique()} test users", flush=True)
    columns = ["precision", "recall", "f1", "pr_auc_ap", "roc_auc",
               "false_alerts_per_evaluated_participant_utc_day"]
    means = pd.DataFrame(results).groupby(["model", "policy"])[columns].mean().reset_index().to_dict("records")
    return {"fold_results": results, "mean_metrics": means}, pd.concat(predictions, ignore_index=True), {
        "splits": split_details, "validation_curves": curves, "per_user_metrics": per_user,
    }


def verify_sources(dataset, manifest):
    """Check the cached window table's source manifest without parsing logs again."""
    for item in manifest:
        file = Path(dataset) / "data" / item["file"]
        if file.stat().st_size != item["bytes"]:
            raise ValueError(f"Source size changed: {file.name}")
        digest = hashlib.sha256()
        with file.open("rb") as stream:
            for block in iter(lambda: stream.read(1 << 20), b""):
                digest.update(block)
        if digest.hexdigest() != item["sha256"]:
            raise ValueError(f"Source content changed: {file.name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=Path, default=Path("data/processed/model_exploration_all_342"))
    parser.add_argument("--base-report", type=Path, default=Path("reports/model_exploration_all_342/results.json"))
    parser.add_argument("--dataset", type=Path, default=Path("src/data/mobile_phone_use"))
    parser.add_argument("--output", type=Path, default=Path("reports/model_exploration_thresholds"))
    parser.add_argument("--workdir", type=Path, default=Path("data/processed/model_exploration_thresholds"))
    args = parser.parse_args()
    base = json.loads(args.base_report.read_text())
    cached = json.loads((args.cache / "local_details.json").read_text())
    verify_sources(args.dataset, cached["source_manifest"])
    table = pd.read_csv(args.cache / "windows.csv")
    if (len(table) != base["valid_window_pairs"] or table["participant_id"].nunique() != base["participants"]
            or set(table["participant_id"]) != {Path(x["file"]).stem for x in cached["source_manifest"]}):
        raise ValueError("Cached table coverage differs from the source report/manifest")
    report, predictions, local = compare_thresholds(table)
    report.update({
        "scope": "Decision-threshold exploration with disjoint fitting, validation and outer-test users",
        "participants": table["participant_id"].nunique(), "window_pairs": len(table),
        "features": base["features"], "window_config": base["window_config"],
        "hyperparameters": base["hyperparameters"], "environment": base["environment"],
        "selection_protocol": {
            "outer": "3-fold GroupKFold by participant",
            "inner": "GroupShuffleSplit: 20% of outer-development users for validation, random_state=42",
            "proxy": "90th percentiles from fitting users only; same OR rule and floors as initial exploration",
            "criterion": "maximum validation F1; exact ties choose higher threshold",
            "decision_grid": [float(t) for t in DECISION_GRID],
            "probability_decision": "positive iff score >= threshold",
            "refit_after_threshold_selection": False,
        },
        "mean_definition": "Unweighted mean of outer-test fold metrics; AP is average precision",
        "false_alert_denominator": "Unique evaluated participant-UTC-date pairs, including partial days",
        "cache_sha256": hashlib.sha256((args.cache / "windows.csv").read_bytes()).hexdigest(),
        "source_report_sha256": hashlib.sha256(args.base_report.read_bytes()).hexdigest(),
        "limitations": [
            "Not a final unseen deployment cohort; the dataset was already used in earlier exploration.",
            "New fitting subset also changes proxy cutoffs. Compare 0.50 and selected thresholds WITHIN this study, not as a direct improvement over initial full-data metrics.",
            "Maximizing validation F1 does not enforce a notification budget or guarantee fewer false alerts.",
            "Operational proxy, retrospective window selection; no unwanted-use, personalization or Android validation.",
        ],
    })
    args.output.mkdir(parents=True, exist_ok=True)
    args.workdir.mkdir(parents=True, exist_ok=True)
    (args.output / "results.json").write_text(json.dumps(report, indent=2, allow_nan=False) + "\n")
    (args.workdir / "local_details.json").write_text(json.dumps(local, indent=2, allow_nan=False) + "\n")
    predictions.to_csv(args.workdir / "oof_predictions.csv", index=False)
    chosen = predictions.loc[predictions["policy"].isin(["fixed_rule", "validation_f1"])]
    plot_results(chosen, args.output)
    print(json.dumps(report["mean_metrics"], indent=2))


if __name__ == "__main__":
    main()
