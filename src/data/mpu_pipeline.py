#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Grup 8 - Veri hazırlama ve öznitelik çıkarımı hattı (Mobile Phone Use Dataset)

Ne yapar?
  1) 342 katılımcının CSV dosyasını okur (yalnızca gereken sütunlar).
  2) Ekran olaylarından oturum çıkarır (Unlocked -> Off).
  3) 30 dakikalık pencereler ve öznitelikler üretir.
  4) Vekil etiketleri (uzun oturum / gece kullanımı / tekrarlı kilit açma) üretir,
     bir sonraki pencerenin etiketini hedef (y_next_*) olarak ekler.
  5) Katılımcı envanteri, veri kalitesi raporu, günlük tablo, etiket duyarlılık
     tablosu ve şekilleri yazar.

Kullanım (örnek):
  python mpu_pipeline.py --root mobile_phone_use --out cikti --jobs 4
  python mpu_pipeline.py --root mobile_phone_use --out cikti_deneme --limit 10 --leak-test 3

Beklenen klasör yapısı:
  <root>/pinfo.csv
  <root>/data/<uuid>.csv      (yoksa <root> içindeki *.csv dosyaları kullanılır)

Gereksinim: pandas, numpy, matplotlib   (pyarrow gerekmez)
"""
import argparse
import hashlib
import json
import sys
import time
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
import pandas as pd

# ----------------------------------------------------------------------------- ayarlar
WIN_S = 1800                      # 30 dakikalık pencere (saniye)
DAY_WINDOWS = 48                  # günde 48 pencere
HEARTBEAT = {"Batt", "BattDrain", "Light", "Data", "Acc", "Loc", "Wifi",
             "CellTower", "Orient", "Charging", "Noise"}   # düzenli kayıt alan sensörler
USECOLS = {"ts_raw", "sensor_id", "Screen_Value", "App_Value", "Notif_Action"}
NON_APP_PREFIX = ("+", "=", "-")  # ana ekran, klavye, sistem olayları uygulama geçişi sayılmaz

DEFAULTS = dict(
    max_session_h=3.0,      # bundan uzun oturum "geçersiz" (muhtemelen eksik Off olayı)
    long_min=20,            # uzun oturum eşiği (dk)
    night_start=0, night_end=5,
    night_min=5,            # gece kullanımı eşiği (dk ekran süresi)
    rapid_n=6,              # bir pencerede tekrarlı kilit açma eşiği
    rapid_source="sessions",# "sessions" = yeni başlayan oturumlar (oturum açıkken gelen tekrarlı Unlocked çift sayılmaz); "unlocks" = ham Unlocked olayları
    split_on=0,             # 1 = oturum açıkken gelen "On" olayında oturumu böl (kaçan "Off" olaylarına karşı)
)
GRID = dict(long_min=[10, 15, 20, 30], night_min=[2, 5, 10], rapid_n=[4, 6, 8])

FEATURE_COLS = ["screen_s", "sessions_started", "longest_run_s", "app_switches", "notifs",
                "prev_session_s", "hour", "dow", "is_night", "is_weekend",
                "dev_screen", "dev_unlocks", "hist_days"]
LABEL_COLS = ["lab_long", "lab_night", "lab_rapid", "lab_any"]
TARGET_COLS = ["y_next_long", "y_next_night", "y_next_rapid", "y_next_any"]


# ----------------------------------------------------------------------------- yükleme
def load_events(path, tz_hours, lo=None, hi=None):
    """Olayları yerel saat (saniye, epoch) cinsinden yükler. tz_hours: pinfo.timezone (+1 yaz saati).
    lo/hi: makul zaman aralığı (yerel epoch sn). Dışındaki olaylar (bozuk telefon saati) atılır ve sayılır."""
    d = pd.read_csv(path, usecols=lambda c: c in USECOLS, low_memory=False)
    for c in USECOLS:
        if c not in d.columns:
            d[c] = np.nan
    d["ts_s"] = pd.to_numeric(d["ts_raw"], errors="coerce") / 1000.0 + (tz_hours + 1) * 3600.0
    d = d.dropna(subset=["ts_s", "sensor_id"]).drop(columns=["ts_raw"])
    n_dup = int(d.duplicated().sum())
    d = d.drop_duplicates()
    n_out = 0
    if lo is not None and hi is not None:
        keep = (d.ts_s >= lo) & (d.ts_s <= hi)
        n_out = int((~keep).sum())
        d = d[keep]
    d = d.sort_values("ts_s", kind="stable").reset_index(drop=True)
    return d, n_dup, n_out


# ----------------------------------------------------------------------------- oturumlar
def sessions(d, max_session_h=3.0, close_open_at=None, split_on=0):
    """Oturum = 'Unlocked' olayından sonraki ilk 'Off' olayına kadar.
    close_open_at: veri sonunda açık kalan oturumu bu zamanda kapatır (sızıntı testi için).
    split_on=1: oturum açıkken 'On' olayı gelirse (ekran arada kapanmış, 'Off' kaydı kaçmış demektir) oturumu o anda kapatır."""
    s = d.loc[d.sensor_id == "Screen", ["ts_s", "Screen_Value"]]
    rows, start = [], None
    dup = dup_after_on = dup_le10 = dup_ge60 = on_open = n_on = 0
    on_flag, last_unl = False, None
    for t, v in zip(s.ts_s.values, s.Screen_Value.values):
        if v == "Unlocked":
            if start is None:
                start, n_on, on_flag = t, 0, False
            else:
                dup += 1                                  # oturum zaten açıkken gelen tekrarlı 'Unlocked'
                if on_flag:
                    dup_after_on += 1                     # arada 'On' görüldü: ekran kapanıp açılmış, 'Off' kaçmış
                g = t - last_unl
                if g <= 10:
                    dup_le10 += 1
                elif g >= 60:
                    dup_ge60 += 1
                on_flag = False
            last_unl = t
        elif v == "On":
            if start is not None:
                on_open += 1; n_on += 1; on_flag = True
                if split_on:
                    rows.append((start, t, n_on)); start, n_on, on_flag = None, 0, False
        elif v == "Off":
            if start is not None:
                rows.append((start, t, n_on)); start, n_on, on_flag = None, 0, False
    if start is not None and close_open_at is not None:
        rows.append((start, max(start, close_open_at), n_on))
    ss = pd.DataFrame(rows, columns=["start_s", "end_s", "n_on_inside"])
    ss.attrs.update(dup_unlocked=dup, dup_after_on=dup_after_on, dup_le10=dup_le10, dup_ge60=dup_ge60, on_while_open=on_open)
    ss["dur_s"] = ss.end_s - ss.start_s
    ss["valid"] = (ss.dur_s >= 0) & (ss.dur_s <= max_session_h * 3600)   # geçmişe bakarak temizlik (yalnızca etiket/EDA için)
    # ÖZNİTELİKLER için nedensel kural: 3 saatten uzun oturumda yalnızca ilk 3 saat sayılır (oturumun sonradan ne kadar
    # uzayacağı o an bilinemez; bu yüzden oturum atılmaz, süresi sınırlanır)
    ss["end_cap_s"] = np.minimum(ss.end_s, ss.start_s + max_session_h * 3600)
    ss["dur_cap_s"] = ss.end_cap_s - ss.start_s
    return ss


# ----------------------------------------------------------------------------- pencereler
def windows(d, ss, p, t_end=None):
    """30 dakikalık pencere tablosu: öznitelikler (pencere içi/öncesi bilgi) + etiketler + hedefler."""
    t0 = np.floor(d.ts_s.min() / WIN_S) * WIN_S
    t1 = np.ceil(max(d.ts_s.max(), t_end if t_end is not None else 0.0) / WIN_S) * WIN_S   # t_end: zaman çizelgesini bu ana kadar uzat
    ws = np.arange(t0, t1, WIN_S)
    n = len(ws)
    we = ws + WIN_S

    def count(times):
        if len(times) == 0:
            return np.zeros(n)
        i = ((np.asarray(times) - t0) // WIN_S).astype(int)
        i = i[(i >= 0) & (i < n)]
        return np.bincount(i, minlength=n)[:n].astype(float)

    unl = count(d.loc[(d.sensor_id == "Screen") & (d.Screen_Value == "Unlocked"), "ts_s"].values)
    hb = count(d.loc[d.sensor_id.isin(HEARTBEAT), "ts_s"].values) > 0
    nf = count(d.loc[(d.sensor_id == "Notif") & d.Notif_Action.isin(["posted", "changed"]), "ts_s"].values)

    a = d.loc[d.sensor_id == "App", ["ts_s", "App_Value"]].copy()
    a["App_Value"] = a.App_Value.astype(str)
    a = a[~a.App_Value.str.startswith(NON_APP_PREFIX)]
    if len(a) > 1:
        pkg = a.App_Value.values
        chg = np.r_[False, pkg[1:] != pkg[:-1]]
        sw = count(a.ts_s.values[chg])
    else:
        sw = np.zeros(n)

    a_all = ss[ss.dur_s >= 0]
    screen = np.zeros(n); clean = np.zeros(n); longest = np.zeros(n); lblmax = np.zeros(n); started = np.zeros(n)
    for st, en, du, ok in zip(a_all.start_s.values, a_all.end_cap_s.values, a_all.dur_s.values, a_all.valid.values):
        j = int((st - t0) // WIN_S)
        if 0 <= j < n:
            started[j] += 1                              # başlangıç anı canlı olarak bilinir
        i = max(j, 0)
        while i < n and ws[i] < en:
            ov = min(we[i], en) - max(ws[i], st)
            if ov > 0:
                screen[i] += ov                          # öznitelik: nedensel (sınırlı) ekran süresi
                longest[i] = max(longest[i], ov)
                if ok:                                   # etiket/EDA: yalnızca temiz (geçerli) oturumlar
                    clean[i] += ov
                    lblmax[i] = max(lblmax[i], du)
            i += 1

    if len(a_all):
        ends, durs = a_all.end_s.values, a_all.dur_cap_s.values
        k = np.searchsorted(ends, ws, side="right") - 1
        prev = np.where(k >= 0, durs[np.clip(k, 0, None)], 0.0)
    else:
        prev = np.zeros(n)

    W = pd.DataFrame({"w_start_s": ws})
    W["day_index"] = ((ws - t0) // 86400).astype(int)
    W["hour"] = ((ws % 86400) // 3600).astype(int)
    W["dow"] = (((ws // 86400) + 3) % 7).astype(int)       # Pazartesi = 0
    W["is_night"] = ((W.hour >= p["night_start"]) & (W.hour < p["night_end"])).astype(int)
    W["is_weekend"] = (W.dow >= 5).astype(int)
    W["valid"] = hb
    W["screen_s"], W["unlocks_raw"], W["sessions_started"] = screen, unl, started
    W["clean_screen_s"] = clean
    W["longest_run_s"], W["app_switches"], W["notifs"], W["prev_session_s"] = longest, sw, nf, prev
    W["lbl_max_session_s"] = lblmax

    # kişisel geçmiş öznitelikleri: yalnızca ÖNCEKİ pencerelerden (shift(1))
    for col, out in [("screen_s", "dev_screen"), ("sessions_started", "dev_unlocks")]:
        x = W[col].where(W.valid)
        mu = x.shift(1).rolling(7 * DAY_WINDOWS, min_periods=DAY_WINDOWS).mean()
        sd = x.shift(1).rolling(7 * DAY_WINDOWS, min_periods=DAY_WINDOWS).std()
        W[out] = ((x - mu) / (sd + 1e-6)).clip(-10, 10)
    W["hist_days"] = W.valid.cumsum() / DAY_WINDOWS         # bu pencereye kadar geçerli gün sayısı

    add_labels(W, p)
    return W


def add_labels(W, p):
    """Vekil etiketler (pencerenin kendisi) ve bir sonraki pencerenin hedefi."""
    W["lab_long"] = (W.lbl_max_session_s >= p["long_min"] * 60).astype(int)
    W["lab_night"] = ((W.is_night == 1) & (W.clean_screen_s >= p["night_min"] * 60)).astype(int)
    rapid_base = W.unlocks_raw if p.get("rapid_source", "sessions") == "unlocks" else W.sessions_started
    W["lab_rapid"] = (rapid_base >= p["rapid_n"]).astype(int)
    W["lab_any"] = ((W.lab_long + W.lab_night + W.lab_rapid) > 0).astype(int)
    W["target_ok"] = W.valid & W.valid.shift(-1, fill_value=False)
    for lab, tgt in zip(LABEL_COLS, TARGET_COLS):
        W[tgt] = W[lab].shift(-1).where(W.target_ok)


# ----------------------------------------------------------------------------- tek katılımcı
def process_one(args):
    path, uuid, tz, p, lo, hi = args
    d, n_dup, n_out = load_events(path, tz, lo, hi)
    if d.empty:
        return dict(uuid=uuid, empty=True)
    ss = sessions(d, p["max_session_h"], split_on=p["split_on"])
    W = windows(d, ss, p)
    vw = W[W.valid]
    n_valid = int(W.valid.sum())

    # envanter
    sc = d.sensor_id.value_counts()
    screen_vals = set(d.loc[d.sensor_id == "Screen", "Screen_Value"].dropna().unique())
    notif_vals = set(d.loc[d.sensor_id == "Notif", "Notif_Action"].dropna().unique())
    sv = ss[ss.valid]
    inv = dict(
        uuid=uuid, empty=False, rows=len(d), duplicates_removed=n_dup, events_outside_range=n_out,
        span_days=round((d.ts_s.max() - d.ts_s.min()) / 86400, 2),
        n_screen_events=int(sc.get("Screen", 0)),
        has_screen_events=bool(sc.get("Screen", 0) > 0),
        has_screen_On=("On" in screen_vals), has_screen_Unlocked=("Unlocked" in screen_vals),
        notif_actions="|".join(sorted(map(str, notif_vals))),
        n_app_events=int(sc.get("App", 0)),
        sessions_valid=int(ss.valid.sum()), sessions_invalid=int((~ss.valid).sum()),
        unlock_events=int(((d.sensor_id == "Screen") & (d.Screen_Value == "Unlocked")).sum()),
        unlock_in_open_session=int(ss.attrs.get("dup_unlocked", 0)),
        dup_after_on=int(ss.attrs.get("dup_after_on", 0)), dup_le10=int(ss.attrs.get("dup_le10", 0)), dup_ge60=int(ss.attrs.get("dup_ge60", 0)),
        on_while_open=int(ss.attrs.get("on_while_open", 0)),
        invalid_with_on_inside=int(((~ss.valid) & (ss.n_on_inside > 0)).sum()),
        max_session_h=round(float(ss.dur_s.max() / 3600), 2) if len(ss) else 0.0,
        median_session_s=float(sv.dur_s.median()) if len(sv) else np.nan,
        share_sessions_295_305s=float(((sv.dur_s >= 295) & (sv.dur_s <= 305)).mean()) if len(sv) else np.nan,
        windows_total=len(W), windows_valid=n_valid, valid_frac=round(n_valid / max(len(W), 1), 4),
        valid_days=round(n_valid / DAY_WINDOWS, 2),
        targets_ok=int(W.target_ok.sum()),
    )
    inv["unlock_open_share"] = inv["unlock_in_open_session"] / max(inv["unlock_events"], 1)
    inv["unlock_dup_removed_share"] = 1 - (inv["sessions_valid"] + inv["sessions_invalid"]) / max(inv["unlock_events"], 1)
    # geçersiz (uzun) oturumların dokunduğu geçerli pencereler: öznitelikte sayılıyor, etiketlerde dışarıda
    inv["windows_touched_by_invalid_sessions"] = int(((W.screen_s - W.clean_screen_s) > 1).__and__(W.valid).sum())
    for lab in LABEL_COLS:
        inv["prev_" + lab] = float(vw[lab].mean()) if n_valid else np.nan
    # günlük tablo
    day = W.groupby("day_index").agg(screen_min=("clean_screen_s", lambda x: x.sum() / 60.0),
                                      unlocks=("sessions_started", "sum"), unlocks_raw=("unlocks_raw", "sum"), valid_windows=("valid", "sum")).reset_index()
    day.insert(0, "uuid", uuid)
    day["screen_min"] = day.screen_min.round(2)
    inv["active_days_screen"] = int(((day.unlocks > 0) & (day.valid_windows >= 12)).sum())
    inv["mean_daily_screen_min"] = float(day.loc[day.valid_windows >= 24, "screen_min"].mean()) if (day.valid_windows >= 24).any() else np.nan
    inv["mean_daily_unlocks"] = float(day.loc[day.valid_windows >= 24, "unlocks"].mean()) if (day.valid_windows >= 24).any() else np.nan
    inv["mean_daily_unlocks_raw"] = float(day.loc[day.valid_windows >= 24, "unlocks_raw"].mean()) if (day.valid_windows >= 24).any() else np.nan
    # etiket duyarlılığı
    grid = []
    for t in GRID["long_min"]:
        grid.append(("long_session", f">={t} dk", int((vw.lbl_max_session_s >= t * 60).sum()), n_valid))
    for t in GRID["night_min"]:
        grid.append(("night_use", f">={t} dk (00-05)", int(((vw.is_night == 1) & (vw.clean_screen_s >= t * 60)).sum()), n_valid))
    for t in GRID["rapid_n"]:
        rb = vw.unlocks_raw if p.get("rapid_source", "sessions") == "unlocks" else vw.sessions_started
        grid.append(("rapid_unlock", f">={t} kilit açma", int((rb >= t).sum()), n_valid))
    hourly = vw.groupby("hour").clean_screen_s.mean().reindex(range(24)).values / 60.0 if n_valid else np.full(24, np.nan)

    W.insert(0, "uuid", uuid)
    keep = ["uuid", "w_start_s", "day_index", "valid", "target_ok"] + FEATURE_COLS + ["unlocks_raw", "clean_screen_s"] + LABEL_COLS + TARGET_COLS
    return dict(uuid=uuid, empty=False, inv=inv, windows=W[keep], daily=day,
                grid=grid, hourly=hourly, dur=sv.dur_s.values.astype("float32"),
                sensors=sc.to_dict())


# ----------------------------------------------------------------------------- sızıntı testi
def leak_test(path, tz, p, n=12, seed=0, lo=None, hi=None):
    """Pencere k'nın öznitelikleri, pencere sonrasındaki veriler silinse de DEĞİŞMEMELİ.
    Rastgele pencerelere ek olarak, 'geçersiz' (uzun) oturumlara denk gelen zor pencereler de denenir."""
    d, _, _ = load_events(path, tz, lo, hi)
    ss_full = sessions(d, p["max_session_h"], split_on=p["split_on"])
    W = windows(d, ss_full, p)
    rng = np.random.default_rng(seed)
    cand = np.flatnonzero(W.valid.values & (W.hist_days.values > 2))
    if len(cand) == 0:
        return None
    picks = set(rng.choice(cand, size=min(n, len(cand)), replace=False).tolist())
    inv_s = ss_full[~ss_full.valid]
    stress = []
    for st, en in zip(inv_s.start_s.values, inv_s.end_s.values):
        ks = np.flatnonzero((W.w_start_s.values < en) & (W.w_start_s.values + WIN_S > st))
        if len(ks):
            stress += ks[:4].tolist() + [int(min(ks[-1] + 1, len(W) - 1))]
    picks |= set(stress[:10])
    bad, details = 0, []
    for k in sorted(picks):
        end_s = W.w_start_s.iloc[k] + WIN_S
        d2 = d[d.ts_s < end_s]
        W2 = windows(d2, sessions(d2, p["max_session_h"], close_open_at=end_s, split_on=p["split_on"]), p, t_end=end_s)
        r = W2[W2.w_start_s == W.w_start_s.iloc[k]]
        if r.empty:
            bad += 1; details.append((int(k), "pencere bulunamadı", None, None))
            continue
        a = W.loc[k, FEATURE_COLS].astype(float)
        b = r.iloc[0][FEATURE_COLS].astype(float)
        diff = [c for c in FEATURE_COLS if not np.isclose(a[c], b[c], equal_nan=True, atol=1e-6)]
        if diff:
            bad += 1
            details.append((int(k), diff, [round(float(a[c]), 2) for c in diff], [round(float(b[c]), 2) for c in diff]))
    gaps = sum(1 for x in details if x[1] == "pencere bulunamadı")
    return len(picks), bad, details, gaps



# ----------------------------------------------------------------------------- elle kontrol
def inspect_participant(path, uuid, tz, p, lo, hi, out, n_windows=3, seed=1):
    """M3 için elle kontrol: rastgele pencerelerin ham olaylarını ve hesaplanan öznitelikleri yazar."""
    d, _, _ = load_events(path, tz, lo, hi)
    ss = sessions(d, p["max_session_h"], split_on=p["split_on"])
    W = windows(d, ss, p)
    cand = np.flatnonzero(W.valid.values & (W.unlocks_raw.values >= 1))
    if len(cand) == 0:
        print(f"{uuid}: kontrol edilecek etkin pencere yok"); return
    rng = np.random.default_rng(seed + sum(map(ord, uuid)))
    picks = sorted(rng.choice(cand, size=min(n_windows, len(cand)), replace=False).tolist())
    ts = lambda x: pd.to_datetime(x, unit="s").strftime("%Y-%m-%d %H:%M:%S")
    lines = [f"ELLE KONTROL - {uuid}  (yerel saat; pencere = 30 dk)", "=" * 70]
    for k in picks:
        ws, we = W.w_start_s.iloc[k], W.w_start_s.iloc[k] + WIN_S
        lines += [f"\nPENCERE {k}: {ts(ws)} - {ts(we)}", "-" * 70, "Ham Screen olayları (pencere ve çevresi):"]
        sc = d[(d.sensor_id == "Screen") & (d.ts_s >= ws - 600) & (d.ts_s < we + 600)]
        for t, v in zip(sc.ts_s, sc.Screen_Value):
            tag = "  <-- pencere içi" if ws <= t < we else ""
            lines.append(f"  {ts(t)}  {v}{tag}")
        lines.append("Pencereyle kesişen oturumlar (betiğin çıkardığı, TAM süreleriyle; listede görünmeyen başlangıçlar dahil):")
        ov = ss[(ss.start_s < we) & (ss.end_s > ws)]
        if ov.empty:
            lines.append("  (yok)")
        for st_, en_, du_, ok_, no_ in zip(ov.start_s, ov.end_s, ov.dur_s, ov.valid, ov.n_on_inside):
            note = "GEÇERLİ" if ok_ else "GEÇERSİZ (3 saatten uzun): özniteliklerde yalnızca ilk 3 saati sayılır, etiketlerde dışarıda"
            lines.append(f"  {ts(st_)} -> {ts(en_)}  süre={du_:.0f} sn  {note}  [içinde 'On' olayı: {int(no_)}]")
        ap = d[(d.sensor_id == "App") & (d.ts_s >= ws) & (d.ts_s < we)]
        lines.append(f"Pencere içi App olayı sayısı (ham): {len(ap)}")
        for t, v in list(zip(ap.ts_s, ap.App_Value))[:12]:
            lines.append(f"  {ts(t)}  {v}")
        r = W.iloc[k]
        lines.append("Hesaplanan öznitelikler:")
        for c in ["screen_s", "unlocks_raw", "sessions_started", "longest_run_s", "app_switches", "prev_session_s", "hour", "dow", "dev_screen", "hist_days"]:
            lines.append(f"  {c:18s} = {r[c]:.2f}" if isinstance(r[c], (float, np.floating)) else f"  {c:18s} = {r[c]}")
        lines.append(f"Etiketler: long={int(r.lab_long)} night={int(r.lab_night)} rapid={int(r.lab_rapid)} | sonraki pencere hedefi (any)={r.y_next_any}")
        lines.append("KONTROL: yukarıdaki Unlocked/Off sırasından oturum süresini elle hesaplayıp screen_s ile karşılaştırın.")
    path_out = Path(out) / f"elle_kontrol_{uuid}.txt"
    path_out.write_text("\n".join(lines), encoding="utf-8")
    print(f"  {uuid}: {len(picks)} pencere -> {path_out}")

# ----------------------------------------------------------------------------- şekiller
def make_figures(pinfo, inv, grid_df, hourly, durs, sensors, out):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig_dir = Path(out) / "figures"
    fig_dir.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})

    # 1) katılımcı profili (pinfo - tüm 342)
    fig, ax = plt.subplots(1, 3, figsize=(14, 4))
    for g, lab in [(0, "gender=0"), (1, "gender=1")]:
        ax[0].hist(pinfo.loc[pinfo.gender == g, "age"].dropna(), bins=range(15, 72, 5), alpha=0.65, label=lab)
    ax[0].set_title(f"Yaş dağılımı (n={len(pinfo)})"); ax[0].set_xlabel("yaş"); ax[0].legend()
    ax[1].hist(pinfo.active_days.dropna(), bins=range(0, 75, 3), color="#5B7DB1")
    for x in (14, 21):
        ax[1].axvline(x, color="#C00000", ls="--")
    ax[1].set_title("Aktif gün sayısı (pinfo)"); ax[1].set_xlabel("gün")
    miss = {"Big Five": pinfo["Big5Openness"].isna().mean(), "BPS": pinfo["BPSScore"].isna().mean(), "PHQ-8": pinfo["PHQ8"].isna().mean()}
    ax[2].bar(miss.keys(), [v * 100 for v in miss.values()], color="#C55A11")
    ax[2].set_ylim(0, 100); ax[2].set_title("Eksik anket verisi (%)")
    for i, v in enumerate(miss.values()):
        ax[2].text(i, v * 100 + 2, f"{v*100:.0f}%", ha="center")
    fig.tight_layout(); fig.savefig(fig_dir / "01_katilimci_profili.png", dpi=170); plt.close(fig)

    # 2) sensör kapsamı
    sc = pd.Series(sensors).sort_values()
    fig, ax = plt.subplots(figsize=(8, 6))
    ax.barh(sc.index, sc.values, color="#2E8B7A"); ax.set_xscale("log")
    ax.set_title("Sensör başına olay sayısı (işlenen katılımcılar, log ölçek)")
    fig.tight_layout(); fig.savefig(fig_dir / "02_sensor_kapsami.png", dpi=170); plt.close(fig)

    # 3) saatlik kullanım profili
    H = np.vstack(hourly)
    fig, ax = plt.subplots(figsize=(9, 4))
    med = np.nanmedian(H, axis=0)
    if H.shape[0] > 2:
        ax.fill_between(range(24), np.nanpercentile(H, 25, axis=0), np.nanpercentile(H, 75, axis=0), alpha=0.25, label="çeyreklikler arası (katılımcılar)")
    ax.plot(range(24), med, marker="o", label="medyan katılımcı")
    ax.set_xlabel("günün saati (yerel)"); ax.set_ylabel("30 dk pencerede ekran süresi (dk)")
    ax.set_title("Saatlik ekran kullanımı profili"); ax.legend(); ax.set_xticks(range(0, 24, 2))
    fig.tight_layout(); fig.savefig(fig_dir / "03_saatlik_profil.png", dpi=170); plt.close(fig)

    # 4) oturum süresi dağılımı
    fig, ax = plt.subplots(figsize=(9, 4))
    ax.hist(np.log10(np.clip(durs, 1, None)), bins=80, color="#5B7DB1")
    ticks = [1, 10, 60, 300, 1800, 10800]
    ax.set_xticks(np.log10(ticks)); ax.set_xticklabels(["1 sn", "10 sn", "1 dk", "5 dk", "30 dk", "3 sa"])
    ax.axvline(np.log10(300), color="#C00000", ls=":", label="300 sn (olası ekran zaman aşımı)")
    ax.set_title(f"Oturum süresi dağılımı (geçerli oturum sayısı={len(durs):,})"); ax.legend()
    fig.tight_layout(); fig.savefig(fig_dir / "04_oturum_suresi.png", dpi=170); plt.close(fig)

    # 5) kişiler arası farklılık
    u = inv.dropna(subset=["mean_daily_screen_min", "mean_daily_unlocks"])
    fig, ax = plt.subplots(1, 2, figsize=(12, 4.4))
    sc_ = ax[0].scatter(u.mean_daily_unlocks, u.mean_daily_screen_min, c=u.age, cmap="viridis", s=26)
    ax[0].set_xlabel("günlük ortalama kilit açma"); ax[0].set_ylabel("günlük ekran süresi (dk)")
    ax[0].set_title("Kişiler arası farklılık"); plt.colorbar(sc_, ax=ax[0], label="yaş")
    ax[1].hist(u.prev_lab_any * 100, bins=30, color="#C55A11")
    ax[1].set_xlabel("vekil etiketli pencere oranı (%)"); ax[1].set_ylabel("katılımcı sayısı")
    ax[1].set_title("Vekil etiket yaygınlığı (kişi bazında)")
    fig.tight_layout(); fig.savefig(fig_dir / "05_kisiler_arasi_farklilik.png", dpi=170); plt.close(fig)

    # 6) etiket eşik duyarlılığı
    fig, ax = plt.subplots(1, 3, figsize=(13, 4), sharey=True)
    for a_, rule in zip(ax, ["long_session", "night_use", "rapid_unlock"]):
        g = grid_df[grid_df.rule == rule]
        a_.bar(g.threshold, g.pct_windows, color="#5B7DB1"); a_.set_title(rule)
        for i, v in enumerate(g.pct_windows):
            a_.text(i, v + 0.1, f"{v:.1f}%", ha="center")
        a_.tick_params(axis="x", rotation=15)
    ax[0].set_ylabel("pozitif pencere oranı (%)")
    fig.suptitle("Etiket eşik duyarlılığı (geçerli pencereler)")
    fig.tight_layout(); fig.savefig(fig_dir / "06_etiket_duyarliligi.png", dpi=170); plt.close(fig)

    # 7) veri kalitesi
    fig, ax = plt.subplots(1, 2, figsize=(12, 4))
    ax[0].hist(inv.valid_frac * 100, bins=30, color="#2E8B7A"); ax[0].set_xlabel("geçerli pencere oranı (%)")
    ax[0].set_ylabel("katılımcı sayısı"); ax[0].set_title("Kayıt kapsamı")
    ax[1].hist(inv.valid_days, bins=range(0, 75, 3), color="#5B7DB1")
    for x in (14, 21):
        ax[1].axvline(x, color="#C00000", ls="--")
    ax[1].set_xlabel("geçerli gün sayısı"); ax[1].set_title("Katılımcı başına geçerli gün")
    fig.tight_layout(); fig.savefig(fig_dir / "07_veri_kalitesi.png", dpi=170); plt.close(fig)


# ----------------------------------------------------------------------------- ana akış
def find_files(root):
    root = Path(root)
    data = root / "data"
    files = sorted((data if data.is_dir() else root).glob("*.csv"))
    return [f for f in files if f.stem != "pinfo"]


def main():
    try:  # Windows konsolunda Türkçe karakter hatasını önler
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    ap = argparse.ArgumentParser(description="Grup 8 veri hattı")
    ap.add_argument("--root", required=True, help="pinfo.csv ve data/ klasörünün bulunduğu klasör")
    ap.add_argument("--out", default="cikti")
    ap.add_argument("--jobs", type=int, default=2)
    ap.add_argument("--limit", type=int, default=0, help="yalnızca ilk N dosya (deneme için)")
    ap.add_argument("--leak-test", type=int, default=0, help="N katılımcıda sızıntı testi çalıştır")
    ap.add_argument("--full", action="store_true", help="--inspect ile birlikte tam çalıştırmayı da yap")
    ap.add_argument("--inspect", nargs="+", default=None, help="elle kontrol için katılımcı kimlikleri (örn. u0cba7d u1a14d5)")
    for k, v in DEFAULTS.items():
        ap.add_argument("--" + k.replace("_", "-"), type=type(v), default=v)
    a = ap.parse_args()
    p = {k: getattr(a, k) for k in DEFAULTS}

    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    pinfo_path = Path(a.root) / "pinfo.csv"
    pinfo = pd.read_csv(pinfo_path)
    tz = dict(zip(pinfo.uuid, pinfo.timezone.fillna(1)))
    # makul zaman aralığı: pinfo.start_date .. last_active, iki gün pay (bozuk telefon saatlerini ayıklamak için)
    def _epoch(x):
        t = pd.to_datetime(x, errors="coerce")
        return None if pd.isna(t) else float((t - pd.Timestamp("1970-01-01")).total_seconds())
    bounds = {}
    for u, a_, b_ in zip(pinfo.uuid, pinfo.start_date, pinfo.last_active):
        lo_, hi_ = _epoch(a_), _epoch(b_)
        bounds[u] = (lo_ - 2 * 86400, hi_ + 2 * 86400) if (lo_ is not None and hi_ is not None) else (None, None)
    files = find_files(a.root)
    files = [f for f in files if f.stem in tz]
    if a.limit:
        files = files[: a.limit]
    print(f"{len(files)} katılımcı dosyası işlenecek (pinfo: {len(pinfo)} satır).", flush=True)
    if not files:
        sys.exit("Hiç CSV bulunamadı: --root yolunu kontrol edin.")

    if a.inspect:
        print("\nElle kontrol dosyaları:")
        fmap = {f.stem: f for f in find_files(a.root)}
        for u in a.inspect:
            if u in fmap:
                inspect_participant(fmap[u], u, tz[u], p, bounds[u][0], bounds[u][1], out)
            else:
                print(f"  {u}: dosya bulunamadı")
        print()
        if not a.limit and not a.leak_test and not a.full:
            sys.exit(0)

    if a.leak_test:
        print("\nSızıntı testi:")
        tot = bad = 0
        log = []
        for f in files[: a.leak_test]:
            r = leak_test(f, tz[f.stem], p, lo=bounds[f.stem][0], hi=bounds[f.stem][1])
            if r:
                tot += r[0]; bad += r[1]
                line = f"  {f.stem}: {r[0]} pencere denendi, {r[1]} uyuşmazlık"
                print(line); log.append(line)
                for det in r[2][:5]:
                    line = f"     ayrıntı (pencere, uyuşmayan özellikler, tam veri, kesik veri): {det}"
                    print(line); log.append(line)
        verdict = "GEÇTİ (gelecek verisi kullanılmıyor)" if bad == 0 else f"KALDI ({bad}/{tot} uyuşmazlık)"
        print("  SONUÇ:", verdict, "\n")
        log.append(f"SONUÇ: {verdict}  ({tot} pencere denendi)")
        (out / "sizinti_testi.txt").write_text("\n".join(log), encoding="utf-8")

    t0 = time.time()
    jobs = [(str(f), f.stem, tz[f.stem], p, bounds[f.stem][0], bounds[f.stem][1]) for f in files]
    results = []
    if a.jobs > 1:
        with ProcessPoolExecutor(max_workers=a.jobs) as ex:
            for i, r in enumerate(ex.map(process_one, jobs, chunksize=2), 1):
                results.append(r)
                if i % 20 == 0 or i == len(jobs):
                    print(f"  {i}/{len(jobs)} tamamlandı ({time.time()-t0:.0f} sn)", flush=True)
    else:
        for i, j in enumerate(jobs, 1):
            results.append(process_one(j))
            if i % 20 == 0 or i == len(jobs):
                print(f"  {i}/{len(jobs)} tamamlandı ({time.time()-t0:.0f} sn)", flush=True)
    ok = [r for r in results if not r.get("empty")]
    print(f"{len(ok)} katılımcı işlendi, {len(results)-len(ok)} boş dosya.", flush=True)

    # ---- birleştir
    inv = pd.DataFrame([r["inv"] for r in ok])
    keep_info = ["uuid", "age", "gender", "timezone", "sdk", "active_days", "esm_response_rate"]
    inv = inv.merge(pinfo[keep_info], on="uuid", how="left")
    inv["usable_for_modeling"] = (inv.has_screen_events & (inv.valid_days >= 7) & (inv.active_days_screen >= 7)
                                 & (inv.mean_daily_unlocks >= 1))
    inv.to_csv(out / "participant_inventory.csv", index=False)

    W = pd.concat([r["windows"] for r in ok], ignore_index=True)
    W["w_start_s"] = W.w_start_s.astype("int64")
    W.to_csv(out / "windows.csv.gz", index=False, compression="gzip")
    pd.concat([r["daily"] for r in ok], ignore_index=True).to_csv(out / "daily.csv.gz", index=False, compression="gzip")

    g = pd.DataFrame([x for r in ok for x in r["grid"]], columns=["rule", "threshold", "n_pos", "n_valid"])
    grid_df = g.groupby(["rule", "threshold"], sort=False).sum().reset_index()
    grid_df["pct_windows"] = grid_df.n_pos / grid_df.n_valid * 100
    grid_df.to_csv(out / "label_sensitivity.csv", index=False)

    sensors = {}
    for r in ok:
        for k, v in r["sensors"].items():
            sensors[k] = sensors.get(k, 0) + v
    durs = np.concatenate([r["dur"] for r in ok]) if ok else np.array([1.0])

    # ---- özet
    vW = W[W.valid]
    tgt = W[W.target_ok]
    use = inv[inv.usable_for_modeling]
    persist = {}
    for lab, tg in zip(LABEL_COLS, TARGET_COLS):
        tp = float(((tgt[lab] == 1) & (tgt[tg] == 1)).sum())
        pred, pos = float((tgt[lab] == 1).sum()), float((tgt[tg] == 1).sum())
        pr, rc = tp / max(pred, 1), tp / max(pos, 1)
        persist[lab] = {"precision": round(pr, 3), "recall": round(rc, 3), "f1": round(2 * pr * rc / max(pr + rc, 1e-9), 3)}
    script_hash = hashlib.sha256(Path(__file__).read_bytes()).hexdigest()[:12]
    summary = {
        "script_sha256_12": script_hash, "run_time": time.strftime("%Y-%m-%d %H:%M:%S"),
        "persistence_baseline": persist,
        "participants_processed": int(len(inv)),
        "participants_usable_for_modeling": int(len(use)),
        "participants_without_screen_events": int((~inv.has_screen_events).sum()),
        "participants_valid_days_ge_14": int((inv.valid_days >= 14).sum()),
        "participants_valid_days_ge_21": int((inv.valid_days >= 21).sum()),
        "windows_total": int(len(W)), "windows_valid": int(len(vW)),
        "windows_valid_pct": round(len(vW) / len(W) * 100, 1),
        "windows_with_target": int(len(tgt)),
        "median_valid_days": float(inv.valid_days.median()),
        "label_prevalence_pct_valid_windows": {c: round(vW[c].mean() * 100, 2) for c in LABEL_COLS},
        "target_prevalence_pct": {c: round(tgt[c].mean() * 100, 2) for c in TARGET_COLS},
        "per_user_label_any_pct_median_iqr": [round(float(inv.prev_lab_any.quantile(q) * 100), 2) for q in (0.25, 0.5, 0.75)],
        "participants_screen_On_present": int(inv.has_screen_On.sum()),
        "participants_screen_On_absent": int((~inv.has_screen_On).sum()),
        "notif_action_variants": inv.notif_actions.value_counts().to_dict(),
        "sessions_valid": int(inv.sessions_valid.sum()), "sessions_invalid": int(inv.sessions_invalid.sum()),
        "median_share_sessions_295_305s": round(float(inv.share_sessions_295_305s.median()), 3),
        "duplicates_removed_total": int(inv.duplicates_removed.sum()),
        "windows_touched_by_invalid_sessions": int(inv.windows_touched_by_invalid_sessions.sum()),
        "windows_touched_by_invalid_sessions_pct": round(float(inv.windows_touched_by_invalid_sessions.sum() / max(len(vW), 1) * 100), 2),
        "participants_touched_by_invalid_sessions": int((inv.windows_touched_by_invalid_sessions > 0).sum()),
        "on_while_open_total": int(inv.on_while_open.sum()),
        "dup_after_on_pct": round(float(inv.dup_after_on.sum() / max(inv.unlock_in_open_session.sum(), 1) * 100), 1),
        "dup_le10_pct": round(float(inv.dup_le10.sum() / max(inv.unlock_in_open_session.sum(), 1) * 100), 1),
        "dup_ge60_pct": round(float(inv.dup_ge60.sum() / max(inv.unlock_in_open_session.sum(), 1) * 100), 1),
        "invalid_sessions_with_on_inside_pct": round(float(inv.invalid_with_on_inside.sum() / max(inv.sessions_invalid.sum(), 1) * 100), 1),
        "unlock_events_total": int(inv.unlock_events.sum()),
        "unlock_session_starts_total": int(inv.sessions_valid.sum() + inv.sessions_invalid.sum()),
        "unlock_dup_removed_pct": round(float(1 - (inv.sessions_valid.sum() + inv.sessions_invalid.sum()) / max(inv.unlock_events.sum(), 1)) * 100, 2),
        "participants_dup_removed_gt20pct": int((inv.unlock_dup_removed_share > 0.20).sum()),
        "median_daily_unlocks_raw_vs_sessions": [round(float(inv.mean_daily_unlocks_raw.median()), 1), round(float(inv.mean_daily_unlocks.median()), 1)],
        "unlock_in_open_session_total": int(inv.unlock_in_open_session.sum()),
        "unlock_in_open_session_pct": round(float(inv.unlock_in_open_session.sum() / max(inv.unlock_events.sum(), 1) * 100), 2),
        "events_outside_range_total": int(inv.events_outside_range.sum()),
        "participants_with_events_outside_range": int((inv.events_outside_range > 0).sum()),
        "parameters": p,
    }
    (out / "summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")

    ozet = [
        "GRUP 8 - VERİ HAZIRLAMA ÖZETİ",
        f"İşlenen katılımcı: {summary['participants_processed']} (modellemeye uygun: {summary['participants_usable_for_modeling']}, ekran olayı olmayan: {summary['participants_without_screen_events']})",
        f"En az 14 geçerli günü olan: {summary['participants_valid_days_ge_14']}; en az 21 günü olan: {summary['participants_valid_days_ge_21']}; medyan geçerli gün: {summary['median_valid_days']}",
        f"Pencere: toplam {summary['windows_total']:,}; geçerli {summary['windows_valid']:,} (%{summary['windows_valid_pct']}); hedefi olan {summary['windows_with_target']:,}",
        "Vekil etiket yaygınlığı (geçerli pencerelerde, %): " + ", ".join(f"{k}={v}" for k, v in summary["label_prevalence_pct_valid_windows"].items()),
        "Hedef (bir sonraki pencere) yaygınlığı (%): " + ", ".join(f"{k}={v}" for k, v in summary["target_prevalence_pct"].items()),
        f"Kişi bazında 'lab_any' yüzdesi (Q1, medyan, Q3): {summary['per_user_label_any_pct_median_iqr']}",
        f"Ekran 'On' olayı olan/olmayan katılımcı: {summary['participants_screen_On_present']}/{summary['participants_screen_On_absent']}",
        f"Oturumlar: geçerli {summary['sessions_valid']:,}, geçersiz (>{p['max_session_h']} sa) {summary['sessions_invalid']:,}; ~300 sn oturum payı (medyan katılımcı): {summary['median_share_sessions_295_305s']}",
        f"3 saatten uzun (geçersiz) oturumların dokunduğu geçerli pencere: {summary['windows_touched_by_invalid_sessions']:,} (%{summary['windows_touched_by_invalid_sessions_pct']}; {summary['participants_touched_by_invalid_sessions']} katılımcıda)",
        f"Oturum zaten açıkken gelen tekrarlı 'Unlocked' olayı: {summary['unlock_in_open_session_total']:,} / {summary['unlock_events_total']:,} (%{summary['unlock_in_open_session_pct']})",
        f"  - bu tekrarların %{summary['dup_after_on_pct']}'i arada 'On' görüldükten sonra (ekran kapanıp açılmış, Off kaçmış), %{summary['dup_le10_pct']}'i öncekinden <=10 sn sonra, %{summary['dup_ge60_pct']}'i >=60 sn sonra",
        f"Oturum açıkken gelen 'On' olayı: {summary['on_while_open_total']:,}; geçersiz oturumların %{summary['invalid_sessions_with_on_inside_pct']}'i içinde 'On' olayı barındırıyor (kaçan Off kanıtı)",
        f"Kilit açma sayısı (oturum başlangıcı): ham Unlocked {summary['unlock_events_total']:,} -> oturum başlangıcı {summary['unlock_session_starts_total']:,} (%{summary['unlock_dup_removed_pct']} tekrar çıkarıldı; %20'den fazlası çıkarılan katılımcı: {summary['participants_dup_removed_gt20pct']}); günlük medyan ham/oturum: {summary['median_daily_unlocks_raw_vs_sessions']}",
        f"Silinen yinelenen satır: {summary['duplicates_removed_total']:,}; bozuk zaman damgalı (aralık dışı) olay: {summary['events_outside_range_total']:,} ({summary['participants_with_events_outside_range']} katılımcıda)",
        "Kalıcılık temel yöntemi (sonraki pencere = şimdiki pencere) F1: " + ", ".join(f"{k}={v['f1']}" for k, v in summary["persistence_baseline"].items())
        + "  | any için kesinlik/duyarlılık: " + f"{summary['persistence_baseline']['lab_any']['precision']}/{summary['persistence_baseline']['lab_any']['recall']}",
        f"Betik izi (sha256 ilk 12): {summary['script_sha256_12']}  |  çalıştırma: {summary['run_time']}",
        f"Kullanılan eşikler: {p}",
    ]
    (out / "ozet_tr.txt").write_text("\n".join(ozet), encoding="utf-8")

    cols_doc = [
        "# windows.csv.gz sütunları",
        "- **uuid, w_start_s, day_index**: katılımcı, pencere başlangıcı (yerel saat, epoch sn), katılımcının kayıt gün sırası",
        "- **valid**: pencerede düzenli sensör kaydı var (kayıt boşluğu değil); **target_ok**: bu pencere ve sonraki pencere geçerli",
        "- **Öznitelikler (modele girebilir)**: " + ", ".join(FEATURE_COLS),
        "- **sessions_started** = pencerede başlayan oturum sayısı = kilit açma sayısı. Oturum zaten açıkken gelen tekrarlı Unlocked olayları (bazı cihazlarda çift kayıt) sayılmaz. **unlocks_raw** ham Unlocked sayısıdır; modele GİRDİ OLARAK KOYMAYIN.",
        "- **Etiketler (pencerenin kendisi; MODELE GİRDİ OLARAK KOYMAYIN)**: " + ", ".join(LABEL_COLS),
        "- **Hedefler (bir sonraki pencerenin etiketi)**: " + ", ".join(TARGET_COLS) + "  (target_ok=False ise boş)",
        "",
        "Kurallar: öznitelikler yalnızca pencere içi veya öncesi bilgiyle üretilir (sızıntı testi ile doğrulanır). "
        "hist_days = o ana kadar biriken geçerli gün sayısı (kişiselleştirme eğrileri için).",
        "notifs: 63 katılımcıda bildirim sensörü farklı sürümde (yalnızca 'changed' olayı) olduğundan katılımcılar arasında karşılaştırılamaz; çekirdek özellik kümesine KOYMAYIN (isteğe bağlı deney).",
        "clean_screen_s = geçerli (3 saatten kısa) oturumlardan hesaplanan ekran süresi; yalnızca etiket ve EDA için, MODELE GİRDİ OLARAK KOYMAYIN.",
        "Oturum kuralı (öznitelikler): 3 saatten uzun oturumda yalnızca ilk 3 saat sayılır (nedensel kural). Etiketlerde ise 3 saatten uzun oturumlar hatalı kabul edilip dışarıda bırakılır.",
    ]
    (out / "windows_sutunlari.md").write_text("\n".join(cols_doc), encoding="utf-8")

    try:
        make_figures(pinfo, inv, grid_df, [r["hourly"] for r in ok], durs, sensors, out)
    except Exception as e:  # şekil hatası tabloları engellemesin
        print("Şekil üretiminde hata:", repr(e))

    print("\n" + "\n".join(ozet))
    print(f"\nBitti ({time.time()-t0:.0f} sn). Çıktılar: {out.resolve()}")


if __name__ == "__main__":
    main()
