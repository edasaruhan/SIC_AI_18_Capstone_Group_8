"""Reproducible, preliminary Model Exploration experiment on real raw logs.

Run: python -m src.models.real_data_exploration
This is a limited research experiment, not the final team evaluation protocol.
Raw logs are unchanged; row-level derived outputs remain in ignored data/processed.
"""

import argparse
from dataclasses import asdict
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path
import platform
import tempfile

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.metrics import (
    average_precision_score, confusion_matrix, f1_score, precision_recall_curve,
    precision_score, recall_score, roc_auc_score, roc_curve,
)
from sklearn.model_selection import GroupKFold
from sklearn.pipeline import Pipeline

from .baselines import LogisticRegressionBaseline, RuleBaseline, ThresholdRule
from .features import prepare_features
from .research_data import FEATURE_NAMES, RESEARCH_FEATURES, WindowConfig, research_windows


def fit_proxy_thresholds(training: pd.DataFrame, quantile: float) -> dict:
    """Fit an operational proxy on training users only, not a clinical label.

    Target: next-window longest unlocked run OR next-window unlock count >= the
    respective training quantile. Floors avoid classifying zero use as positive.
    Night use is deliberately absent until a defensible local-time policy exists.
    """
    if not 0 < quantile < 1:
        raise ValueError("proxy quantile must be between 0 and 1")
    return {
        "longest_session_seconds": max(1.0, float(training["next_longest_session_seconds"].quantile(quantile))),
        "unlock_count": max(1.0, float(training["next_unlock_count"].quantile(quantile))),
    }


def proxy_labels(frame: pd.DataFrame, thresholds: dict) -> pd.Series:
    target = frame[["next_longest_session_seconds", "next_unlock_count"]]
    if target.isna().any().any():
        raise ValueError("Missing target measurements must not become negative labels")
    return (target.iloc[:, 0].ge(thresholds["longest_session_seconds"])
            | target.iloc[:, 1].ge(thresholds["unlock_count"])).astype(int)


def model_candidates(thresholds: dict):
    return {
        # A simple current-intensity rule anticipates the next-window proxy. Its
        # current total screen duration is not the future longest-run measurement.
        "Rule": RuleBaseline([
            ThresholdRule("screen_active_seconds", thresholds["longest_session_seconds"]),
            ThresholdRule("unlock_count", thresholds["unlock_count"]),
        ], features=RESEARCH_FEATURES),
        "Logistic Regression": LogisticRegressionBaseline(features=RESEARCH_FEATURES),
        "Random Forest": Pipeline([
            ("imputer", SimpleImputer(strategy="median", add_indicator=True,
                                      keep_empty_features=True)),
            ("classifier", RandomForestClassifier(
                n_estimators=100, max_depth=8, min_samples_leaf=5,
                class_weight="balanced", random_state=42, n_jobs=1,
            )),
        ]),
    }


def model_metrics(y, predictions, scores, evaluated: pd.DataFrame):
    days = evaluated[["participant_id", "window_end_ms"]].copy()
    days["utc_day"] = days["window_end_ms"] // 86_400_000
    day_count = len(days[["participant_id", "utc_day"]].drop_duplicates())
    false_positives = int(((np.asarray(y) == 0) & (predictions == 1)).sum())
    return {
        "precision": float(precision_score(y, predictions, zero_division=0)),
        "recall": float(recall_score(y, predictions, zero_division=0)),
        "f1": float(f1_score(y, predictions, zero_division=0)),
        "pr_auc_ap": float(average_precision_score(y, scores)) if np.any(np.asarray(y) == 1) else None,
        "roc_auc": float(roc_auc_score(y, scores)) if len(np.unique(y)) == 2 else None,
        "confusion_matrix": confusion_matrix(y, predictions, labels=[0, 1]).tolist(),
        "false_alerts_per_evaluated_participant_utc_day": false_positives / day_count,
        "evaluated_participant_utc_days": day_count,
        "rows": len(y), "positive_rows": int(np.count_nonzero(np.asarray(y) == 1)),
    }


def compare_models(table: pd.DataFrame, *, folds: int = 3, quantile: float = 0.9):
    splitter = GroupKFold(n_splits=folds)
    results, predictions, participant_results = [], [], []
    for fold, (train_idx, test_idx) in enumerate(
        splitter.split(table, groups=table["participant_id"]), 1,
    ):
        training, test = table.iloc[train_idx], table.iloc[test_idx]
        if set(training["participant_id"]) & set(test["participant_id"]):
            raise RuntimeError("Participant leakage")
        thresholds = fit_proxy_thresholds(training, quantile)
        train_y, test_y = proxy_labels(training, thresholds), proxy_labels(test, thresholds)
        if train_y.nunique() != 2 or test_y.nunique() != 2:
            raise ValueError("Both classes are required in every exploration fold")
        train_X = prepare_features(training, RESEARCH_FEATURES)
        test_X = prepare_features(test, RESEARCH_FEATURES)
        for name, model in model_candidates(thresholds).items():
            model.fit(train_X, train_y)
            scores = (model.predict_scores(test_X) if hasattr(model, "predict_scores")
                      else model.predict_proba(test_X)[:, 1])
            predicted = model.predict(test_X)
            results.append({
                "model": name, "fold": fold, "thresholds": thresholds,
                "training_rows": len(training), "test_rows": len(test),
                "training_users": training["participant_id"].nunique(),
                "test_users": test["participant_id"].nunique(),
                "positive_fraction": float(test_y.mean()),
                **model_metrics(test_y, predicted, scores, test),
            })
            part = test[["participant_id", "window_start_ms", "window_end_ms"]].copy()
            part["model"], part["fold"] = name, fold
            part["y"], part["prediction"], part["score"] = test_y, predicted, scores
            predictions.append(part)
            for participant, group in part.groupby("participant_id"):
                participant_results.append({
                    "participant_id": participant, "model": name, "fold": fold,
                    **model_metrics(group["y"], group["prediction"].to_numpy(),
                                    group["score"].to_numpy(), group),
                })
        print(f"Completed fold {fold}/{folds}: {len(training)} train / {len(test)} test rows", flush=True)
    metrics = ["precision", "recall", "f1", "pr_auc_ap", "roc_auc",
               "false_alerts_per_evaluated_participant_utc_day"]
    means = pd.DataFrame(results).groupby("model")[metrics].mean().to_dict(orient="index")
    return results, means, pd.concat(predictions, ignore_index=True), participant_results


def plot_results(predictions: pd.DataFrame, output: Path):
    cache = Path(tempfile.gettempdir()) / "sic-exploration-cache"
    cache.mkdir(parents=True, exist_ok=True)
    os.environ.setdefault("MPLCONFIGDIR", str(cache / "matplotlib"))
    os.environ.setdefault("XDG_CACHE_HOME", str(cache))
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    names = list(predictions["model"].unique())
    fig, axes = plt.subplots(1, len(names), figsize=(12, 3.8), constrained_layout=True)
    for ax, name in zip(axes, names):
        group = predictions.loc[predictions["model"].eq(name)]
        matrix = confusion_matrix(group["y"], group["prediction"], labels=[0, 1])
        ax.imshow(matrix, cmap="Blues")
        for (i, j), value in np.ndenumerate(matrix):
            ax.text(j, i, str(value), ha="center", va="center",
                    color="white" if value > matrix.max() / 2 else "black")
        ax.set(xticks=[0, 1], yticks=[0, 1], xlabel="Predicted proxy", ylabel="Observed proxy", title=name)
    fig.suptitle("Out-of-fold confusion matrices (training-fold proxy thresholds)")
    fig.savefig(output / "confusion_matrices.png", dpi=160)
    plt.close(fig)
    fig, axes = plt.subplots(1, 2, figsize=(11, 4), constrained_layout=True)
    for name in names:
        group = predictions.loc[predictions["model"].eq(name)]
        fpr, tpr, _ = roc_curve(group["y"], group["score"])
        precision, recall, _ = precision_recall_curve(group["y"], group["score"])
        axes[0].plot(fpr, tpr, label=name)
        axes[1].step(recall, precision, where="post", label=name)
    axes[0].plot([0, 1], [0, 1], "--", color="gray", label="Chance reference")
    axes[1].axhline(predictions["y"].mean(), ls="--", color="gray", label="Proxy prevalence")
    axes[0].set(xlabel="False positive rate", ylabel="True positive rate", title="ROC (pooled OOF)")
    axes[1].set(xlabel="Recall", ylabel="Precision", title="Precision-recall (pooled OOF)")
    for ax in axes:
        ax.legend(fontsize=8)
        ax.set_xlim(0, 1)
        ax.set_ylim(0, 1.02)
    fig.suptitle("Preliminary proxy experiment; thresholds learned per training fold")
    fig.savefig(output / "roc_pr_curves.png", dpi=160)
    plt.close(fig)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=Path("src/data/mobile_phone_use"))
    parser.add_argument("--participants", type=int, default=12)
    parser.add_argument("--folds", type=int, default=3)
    parser.add_argument("--proxy-quantile", type=float, default=0.9)
    parser.add_argument("--gap-minutes", type=float, default=30)
    parser.add_argument("--max-session-minutes", type=float, default=120)
    parser.add_argument("--output", type=Path, default=Path("reports/model_exploration"))
    parser.add_argument("--workdir", type=Path, default=Path("data/processed/model_exploration"))
    args = parser.parse_args()
    if not 2 <= args.folds <= args.participants:
        parser.error("Require 2 <= folds <= participants")
    config = WindowConfig(args.gap_minutes, args.max_session_minutes)
    files = sorted((args.dataset / "data").glob("*.csv"))[:args.participants]
    if len(files) != args.participants:
        parser.error("Not enough participant CSVs")
    frames, diagnostics, manifest = [], [], []
    for file in files:
        frame, detail = research_windows(file, config)
        frames.append(frame)
        diagnostics.append(detail)
        digest = hashlib.sha256()
        with file.open("rb") as stream:
            for block in iter(lambda: stream.read(1 << 20), b""):
                digest.update(block)
        manifest.append({"file": file.name, "bytes": file.stat().st_size, "sha256": digest.hexdigest()})
        print(f"Prepared {file.name}: {len(frame)} valid adjacent-window pairs", flush=True)
    table = pd.concat(frames, ignore_index=True)
    results, means, predictions, user_metrics = compare_models(
        table, folds=args.folds, quantile=args.proxy_quantile,
    )
    args.workdir.mkdir(parents=True, exist_ok=True)
    args.output.mkdir(parents=True, exist_ok=True)
    table.to_csv(args.workdir / "windows.csv", index=False)
    predictions.to_csv(args.workdir / "oof_predictions.csv", index=False)
    local = {"diagnostics": diagnostics, "source_manifest": manifest, "per_user_metrics": user_metrics}
    (args.workdir / "local_details.json").write_text(json.dumps(local, indent=2, allow_nan=False) + "\n")
    report = {
        "scope": "Preliminary real-data proxy exploration, not final personalization evaluation",
        "selection": f"First {args.participants} CSV filenames in lexicographic order; not a representative sample",
        "participants": len(files), "valid_window_pairs": len(table),
        "features": FEATURE_NAMES, "window_config": asdict(config),
        "cross_validation": {"method": "GroupKFold by participant", "folds": args.folds,
                             "hyperparameter_search": False},
        "proxy": {"quantile": args.proxy_quantile, "fit_on": "training users only in each fold",
                  "definition": "next-window longest unlocked run >= threshold OR unlock count >= threshold; no night component"},
        "hyperparameters": {
            "Logistic Regression": {"C": 1.0, "max_iter": 1000, "random_state": 42,
                                    "decision_threshold": 0.5, "preprocessing": "training median + missing indicators + StandardScaler"},
            "Random Forest": {"n_estimators": 100, "max_depth": 8, "min_samples_leaf": 5,
                              "class_weight": "balanced", "random_state": 42, "n_jobs": 1,
                              "preprocessing": "training median + missing indicators"},
            "Rule": {"combination": "any current screen-time/unlock threshold met",
                     "score": "match fraction, not a probability"},
        },
        "metric_definitions": {"pr_auc_ap": "average precision, not trapezoidal PR integration",
                               "false_alerts": "FP / unique evaluated participant-UTC-date pairs, including partial days",
                               "mean_metrics": "unweighted mean over held-out participant folds",
                               "plots": "pooled OOF; proxy thresholds may differ between folds"},
        "mean_fold_metrics": means, "fold_results": results,
        "environment": {"python": platform.python_version(), **{
            package: importlib.metadata.version(package)
            for package in ["numpy", "pandas", "scikit-learn", "matplotlib"]}},
        "limitations": [
            "12-user default subset; not all 342 participants and not representative.",
            "Proxy labels do not validate unwanted use, addiction or benefit to users.",
            "Session/gap policies are explicit research assumptions and require sensitivity checks.",
            "Quality-window selection is retrospective, not verified live collection coverage.",
            "No personal-only/sample-weighted history curves, user-feedback validation or untouched final test.",
            "No local-time/night features: raw UTC and source local-time fields require further reconciliation.",
        ],
    }
    (args.output / "results.json").write_text(json.dumps(report, indent=2, allow_nan=False) + "\n")
    plot_results(predictions, args.output)
    print(json.dumps(means, indent=2))
    print(f"Aggregate results/plots: {args.output}; local row-level outputs: {args.workdir}")


if __name__ == "__main__":
    main()
