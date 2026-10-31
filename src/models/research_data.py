"""Small, explicit raw-log adapter for the Model Exploration experiment only.

Not the shared preprocessing pipeline. Research choices are recorded in results:
30-minute UTC windows; gaps >30 minutes and ambiguous/incomplete/long sessions
invalidate affected windows; no local-time/night feature is guessed. Source CSVs
are read only. No proxy labels or participant splits are created in this module.
"""

from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd

from .features import DEFAULT_FEATURES


RESEARCH_FEATURES = DEFAULT_FEATURES[:4]
FEATURE_NAMES = [spec.name for spec in RESEARCH_FEATURES]
STEP_MS = 30 * 60 * 1000


@dataclass(frozen=True)
class WindowConfig:
    gap_minutes: float = 30
    max_session_minutes: float = 120
    event_year: int = 2016

    def __post_init__(self):
        if (not np.isfinite(self.gap_minutes) or self.gap_minutes <= 0
                or not np.isfinite(self.max_session_minutes) or self.max_session_minutes <= 0):
            raise ValueError("Gap and session limits must be positive finite numbers")


def research_windows(path: Path, config: WindowConfig = WindowConfig()):
    """Return features plus *separate* next-window measurements and diagnostics.

    Selection quality is assessed retrospectively from the log. Hence these are
    research-quality windows, not validated live Android coverage. Previous session
    duration uses only sessions ending strictly before the prediction boundary.
    """
    path = Path(path)
    raw = pd.read_csv(path, usecols=["ts_raw", "sensor_id", "Screen_Value", "App_Value"],
                      dtype=str, keep_default_na=False)
    numeric = pd.to_numeric(raw["ts_raw"], errors="coerce")
    utc = pd.to_datetime(numeric, unit="ms", utc=True, errors="coerce")
    keep = utc.notna() & utc.dt.year.eq(config.event_year) & numeric.mod(1).eq(0)
    diagnostics = {"participant_id": path.stem, "raw_rows": len(raw),
                   "excluded_timestamp_rows": int((~keep).sum())}
    events = raw.loc[keep].copy()
    if events.empty:
        raise ValueError(f"{path.name}: no events in configured recording year")
    events["ts_raw"] = numeric[keep].astype("int64")
    events = events.sort_values("ts_raw", kind="stable")
    times = events["ts_raw"].drop_duplicates().to_numpy()
    first = int(((times[0] + STEP_MS - 1) // STEP_MS) * STEP_MS)
    last = int((times[-1] // STEP_MS) * STEP_MS)
    count = (last - first) // STEP_MS
    if count < 2:
        raise ValueError(f"{path.name}: fewer than two complete windows")
    starts = first + np.arange(count, dtype="int64") * STEP_MS
    ends = starts + STEP_MS
    valid = np.ones(count, dtype=bool)
    active = np.zeros(count, dtype=float)
    longest = np.zeros(count, dtype=float)

    def indices(start, end):
        # Half-open intervals: an event at window end belongs to the next window.
        left = max(0, int((start - first) // STEP_MS))
        right = min(count, int((end - first + STEP_MS - 1) // STEP_MS))
        return range(left, max(left, right))

    def invalidate(start, end):
        for i in indices(start, end):
            valid[i] = False

    gaps = np.flatnonzero(np.diff(times) > config.gap_minutes * 60_000)
    for i in gaps:
        invalidate(int(times[i]), int(times[i + 1]))
    diagnostics["recording_gaps"] = len(gaps)

    screen = events.loc[events["sensor_id"].eq("Screen"), ["ts_raw", "Screen_Value"]]
    before = len(screen)
    screen = screen.drop_duplicates()
    diagnostics["duplicate_screen_events"] = before - len(screen)
    unknown = ~screen["Screen_Value"].isin(["On", "Off", "Unlocked"])
    if unknown.any():
        raise ValueError(f"{path.name}: unknown screen values")
    if screen.empty:
        raise ValueError(f"{path.name}: no screen events")
    invalidate(first, int(screen["ts_raw"].iloc[0]))
    # Logging resumption alone does not establish the screen state. Wait for a
    # screen event after a gap instead of assuming the pre-gap state persisted.
    screen_times = screen["ts_raw"].to_numpy()
    for i in gaps:
        restored = np.searchsorted(screen_times, times[i + 1], side="left")
        restore_time = int(screen_times[restored]) if restored < len(screen_times) else int(times[-1]) + 1
        invalidate(int(times[i]), restore_time)
    ties = set(screen.groupby("ts_raw")["Screen_Value"].nunique().loc[lambda s: s > 1].index)
    sessions = []
    opened = None
    interrupted = orphan_off = too_long = 0
    for timestamp, value in screen.itertuples(index=False, name=None):
        timestamp = int(timestamp)
        if timestamp in ties:
            invalidate(timestamp if opened is None else opened, timestamp + 1)
            opened = None
            continue
        if value == "Unlocked":
            if opened is not None:
                interrupted += 1
                invalidate(opened, timestamp)
            opened = timestamp
        elif value == "On":
            # On alone neither starts nor ends an unlocked-to-Off session. It can
            # be delivered after Unlocked; only Off establishes the ending.
            pass
        elif opened is not None:
            duration = timestamp - opened
            if 0 < duration <= config.max_session_minutes * 60_000:
                sessions.append((opened, timestamp))
            else:
                too_long += 1
                invalidate(opened, timestamp)
            opened = None
        else:
            orphan_off += 1
    if opened is not None:
        invalidate(opened, int(times[-1]) + 1)
    for start, end in sessions:
        for i in indices(start, end):
            overlap = max(0, min(end, int(ends[i])) - max(start, int(starts[i]))) / 1000
            active[i] += overlap
            longest[i] = max(longest[i], overlap)
    # Sessions spanning a recording gap have unknown true duration. Do not use
    # such durations as previous-session features, even after the gap has ended.
    reliable = []
    for start, end in sessions:
        crosses_gap = any(start < times[i + 1] and end > times[i] for i in gaps)
        if not crosses_gap:
            reliable.append((start, end))
    previous_duration = np.full(count, np.nan)
    if reliable:
        session_ends = np.array([end for _, end in reliable])
        durations = np.array([(end - start) / 1000 for start, end in reliable])
        previous_idx = np.searchsorted(session_ends, ends, side="left") - 1
        available = previous_idx >= 0
        previous_duration[available] = durations[previous_idx[available]]
        if len(gaps):
            gap_ends = times[gaps + 1]
            gap_idx = np.searchsorted(gap_ends, ends, side="left") - 1
            since_gap = gap_idx >= 0
            known_end = np.full(count, -1, dtype="int64")
            known_end[available] = session_ends[previous_idx[available]]
            stale = since_gap & (known_end < gap_ends[np.maximum(gap_idx, 0)])
            previous_duration[stale] = np.nan

    def count_events(event_times):
        bucket = ((np.asarray(event_times, dtype="int64") - first) // STEP_MS)
        bucket = bucket[(bucket >= 0) & (bucket < count)]
        return np.bincount(bucket, minlength=count)

    unlocks = count_events(screen.loc[screen["Screen_Value"].eq("Unlocked"), "ts_raw"])
    apps = events.loc[events["sensor_id"].eq("App"), ["ts_raw", "App_Value"]]
    excluded_apps = apps["App_Value"].eq("") | apps["App_Value"].str.startswith(("+", "=", "-"))
    diagnostics["excluded_app_events"] = int(excluded_apps.sum())
    apps = apps.loc[~excluded_apps].drop_duplicates()
    app_ties = set(apps.groupby("ts_raw")["App_Value"].nunique().loc[lambda s: s > 1].index)
    for timestamp in app_ties:
        invalidate(int(timestamp), int(timestamp) + 1)
    apps = apps.loc[~apps["ts_raw"].isin(app_ties)]
    switches = apps["App_Value"].ne(apps["App_Value"].shift())
    breaks = np.sort(np.r_[times[gaps + 1], list(app_ties)])
    if len(breaks) and not apps.empty:
        segments = pd.Series(np.searchsorted(breaks, apps["ts_raw"], side="right"), index=apps.index)
        switches &= segments.eq(segments.shift())
    if not switches.empty:
        switches.iloc[0] = False  # First observed app is not an observed transition.
    app_switches = count_events(apps.loc[switches, "ts_raw"])
    frame = pd.DataFrame({
        "participant_id": path.stem, "window_start_ms": starts, "window_end_ms": ends,
        "target_end_ms": ends + STEP_MS, "screen_active_seconds": active,
        "unlock_count": unlocks, "app_switch_count": app_switches,
        "previous_session_duration_seconds": previous_duration,
    })
    # Shift on the complete grid BEFORE dropping invalid windows, so a gap can
    # never turn the next available row into a fake adjacent-window target.
    frame["next_screen_active_seconds"] = frame["screen_active_seconds"].shift(-1)
    frame["next_unlock_count"] = frame["unlock_count"].shift(-1)
    frame["next_longest_session_seconds"] = pd.Series(longest).shift(-1)
    paired = valid & np.r_[valid[1:], False]
    result = frame.loc[paired].reset_index(drop=True)
    diagnostics.update({
        "candidate_windows": count, "valid_windows": int(valid.sum()),
        "paired_windows": len(result), "sessions": len(sessions),
        "long_or_zero_sessions": too_long, "interrupted_sessions": interrupted,
        "unclosed_sessions": int(opened is not None), "orphan_off_events": orphan_off,
        "ambiguous_screen_timestamps": len(ties), "ambiguous_app_timestamps": len(app_ties),
    })
    return result, diagnostics
