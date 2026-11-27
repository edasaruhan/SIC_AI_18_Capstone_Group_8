# Android uygulaması

**Ana sorumlu:** Murat Gürel. **Entegrasyon desteği:** Yiğit Kayalı.

Bu klasör, Adaptive Digital Wellbeing Coach projesinin tek modüllü Kotlin ve Jetpack Compose tabanlı Android uygulamasını içerir. Aşamalı geliştirme planı (`M01–M15`), ortak özellik eşlik örnekleri (`F01–F12`) ve test matrisi (`T01–T25`) için [Android Yol Haritası](ROADMAP.md) belgesine bakabilirsiniz.

## Uyumlu araç ve sürüm bilgileri

| Bileşen | Sürüm |
| --- | --- |
| Android Studio | 2026.2.2 (Rabbit 2 Patch 2, AI-262.10968.63.2622.16975389) |
| JDK (Android Studio JBR) | OpenJDK 25.0.4 |
| Gradle Wrapper | 9.3.1 |
| Android Gradle Plugin (AGP) | 9.1.0 |
| Kotlin / Compose Compiler | 2.3.0 |
| KSP | 2.3.4 |
| Room | 2.8.4 (`exportSchema = true`) |
| WorkManager | 2.10.1 |
| Jetpack Compose BOM | 2025.05.00 |
| `minSdk` | 28 (Android 9) |
| `compileSdk` / `targetSdk` | 37 (Android SDK Platform 17 / `android-37.0`) |
| Build Tools | 36.0.0 |

## Paket yapısı (`com.group8.wellbeing`)

- `ui`: Jetpack Compose ekranları, durum modeli ve ViewModel (`MainActivity`, `MainScreen`, `MainViewModel`, `MainUiState`)
- `data`: Tek merkezli başlangıç yapılandırması (`AppConfig`) ve ileride Room varlıkları ile repository katmanı
- `usage`: Kullanım erişimi yetki denetimi (`UsageAccessChecker`) ve ileride `UsageStatsManager` olay normalizasyonu
- `domain`: Uygulama toplama durumları (`CollectionStatus`) ve ileride saf Kotlin oturum/özellik hesaplayıcıları
- `worker`: Arka plan zamanlama arayüz sınırı (`CollectionScheduler`) ve ileride WorkManager işi
- `inference`: Model adaptörü ve ortak 7 özellik vektörü sınırı (`FeatureVector`, `Predictor`)

## Kurulum ve derleme adımları

1. Depoyu klonlayın ve `android/` klasörünü Android Studio ile açın.
2. Android SDK yolu `android/local.properties` dosyasında yerel olarak tanımlanır ve Git deposuna eklenmez.
3. Komut satırından debug APK derlemek ve birim testlerini çalıştırmak için (`android/` klasöründe):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug testDebugUnitTest --no-daemon
```

4. Üretilen debug APK konumu: `android/app/build/outputs/apk/debug/app-debug.apk`.
5. Gerçek cihazda veya emülatörde kurulum için Android Studio'dan **Run** kullanın veya `adb install -r app\build\outputs\apk\debug\app-debug.apk` komutunu çalıştırın.
6. **Android 13+ APK kurulum notu (Kısıtlı Ayarlar):** APK doğrudan telefona kurulduğunda *Kullanım Erişimi* anahtarı kilitli görünürse, uygulamadaki **Uygulama Bilgilerini Aç** butonundan (veya Ayarlar → Uygulamalar → Adaptive Digital Wellbeing Coach → menü/en alt) **"Kısıtlı ayarlara izin ver"** seçeneğini onaylayıp Kullanım Erişimi ayarını açın.

### Genel model ve T20 testi

Gradle, repo kökündeki `models/mobile/dataset_v1-xgboost-fold-1-v1/` yerel paketinden model, manifest ve özellik şemasını APK'ya ekler. Üretim komutları ve sözleşme [model teslim notunda](../documents/mobile_model_export.md). Paket bulunamadığında durum ekranı modeli kapalı gösterir ve veri toplama çalışmaya devam eder.

Pixel 8 ARM64 emülatörü açıkken `android/` dizininden çalıştırın:

```bash
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest
bash ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.group8.wellbeing.inference.OnnxPredictorT20Test
```

T20, sentetik golden örnekleri yalnızca test APK'sına alır. Python skorlarıyla eşlik, özellik şeması ve kalite kontrolleri, hedef pencere kaydı ve tekrarlı sorgu davranışı sınanır.

### Kullanım hatırlatmaları

`0.8.0-m11-m12` sürümünde **Durum → Kullanım hatırlatmaları** varsayılan olarak kapalıdır. Kullanıcı açtığında, yalnızca modelin hedef pencerenin ilk beş dakikasında ürettiği pozitif tahmin mevcut izin, kalite, sessiz saat, günlük sınır ve bekleme kontrollerini geçerse Android bildirimi çıkar. Arka plan toplama gecikirse o pencerenin bildirimi bastırılır. Tamamlanan dönemlerin Evet/Hayır geri bildirimi **Dönemler** ekranında verilir.

Pixel 8 API 37 ARM64 üzerinde 40 birim ve 5 emülatör testi geçti. [Bildirim entegrasyon testi](app/src/androidTest/java/com/group8/wellbeing/inference/ReminderDeliveryTest.kt), Kullanım Erişimi ve bildirim izni verilmiş test profiliyle çalışır. Gerçek ONNX modelinin zamanında pozitif tahmini Android bildirim yöneticisinde tek bildirim oluşturdu. Gecikmeli ve kapalı ayar senaryolarında bildirim bastırıldı. Aynı imzalı önceki XGBoost APK'sı üzerine güncellemede onam, Kullanım Erişimi ve Room kayıtları korundu.

## İzinler ve gizlilik sınırları

- `android.permission.PACKAGE_USAGE_STATS` manifestte tanımlıdır; ancak tek başına erişim vermez, M02 ile sistem ayarlarından kullanıcı izni ve ayrı onam akışı eklenecektir.
- `android:allowBackup="false"` ayarı ile yerel verilerin Android otomatik yedekleme üzerinden buluta taşınması engellenmiştir.
- Onam alınmadan kullanım geçmişi sorgulanmaz; gerçek kullanım kayıtları, geri bildirimler, `local.properties`, derleme çıktıları ve imzalama anahtarları depoya eklenmez.
- Genel model yüklendiğinde skor Android CPU'sunda hesaplanır. Uygulama, mevcut kalite kontrollerini geçen pencerelerin skorunu Room'da saklar ve son hedef aralığını durum ekranında gösterir.

## Çalışma ve doğrulama kaydı

| Tarih | Sürüm / Dal | Görev | Uygulanan kontroller | Durum ve notlar |
| --- | --- | --- | --- | --- |
| 2026-11-24 | `0.1.0-m01` (`feat/android-m01-setup`) | M01 Proje kurulumu | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`AppConfigTest`), emülatör (`Medium Phone API 37.0`) | Temiz Gradle derlemesi, birim testi ve emülatörde ilk ekran + elle deneme akışı doğrulandı. Gerçek Android cihaz kurulumu (T01 gerçek cihaz kabulü) fiziksel telefon testine bırakıldı. |
| 2026-11-24 | `0.2.0-m02` (`feat/android-m01-setup`) | M02 Onam ve izin akışları | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`CollectionStateResolverTest`, `MainViewModelTest` — 11/11 test geçti) | Onam ekranı, sistem Kullanım Erişimi ayar yönlendirmesi ve dönüşte yeniden denetim, isteğe bağlı `POST_NOTIFICATIONS` izni, duraklat/yeniden başlat ve tüm yerel verileri sil akışı eklendi. T02, T03, T04, T22 ve T23 birim testleri geçti; emülatör/cihaz üzeri akış kontrolü bekleniyor. |
| 2026-11-24 | `0.3.0-m03` (`feat/android-m01-setup`) | M03 Olay okuma, normalizasyon ve Koyu/Açık tema | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`UsageEventNormalizerTest`, `MainViewModelTest` — 17/17 test geçti) | `UsageStatsManager.queryEvents` tabanlı kısa aralık okuma (`30 dk / 2 saat / 6 saat`), 5 aday normalize olay türü (`SCREEN_INTERACTIVE`, `KEYGUARD_HIDDEN`, `KEYGUARD_SHOWN`, `SCREEN_NON_INTERACTIVE`, `APP_FOREGROUND`), klavye/launcher sınıflandırması, paket tokenizasyonu, reboot sonrası kilit kontrolü, Olay İnceleyici sekmesi ve kalıcı Koyu/Açık tema geçişi eklendi. Gerçek cihaz olay dizisi doğrulaması fiziksel telefon testine bırakıldı. |
| 2026-11-24 | `0.4.0-m04` (`feat/android-m01-setup`) | M04 Room yerel veri modeli ve tekrar güvenliği | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`EventFingerprintTest`, `MainViewModelTest` — 19/19 test geçti) | 7 ayrı Room tablosu (`raw_usage_events`, `collection_batches`, `feature_windows`, `predictions`, `reminder_events`, `feedbacks`, `app_settings`), SHA-256 `EventFingerprint` + benzersiz indekslerle tekrar koruması (T05), `withTransaction` atomik kayıt ve checkpoint güncellemesi (T06), 7 günlük ham olay temizliği ve dışa aktarılmış şema (`schemas/.../1.json`) eklendi. |
| 2026-11-24 | `0.5.0-m05-m10` (`feat/android-m01-setup`) | M05, M06, M07 ve M10 Oturum, 7 özellik, kalite ve Python eşliği | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`FeatureParityTest` F01–F12, T07–T14 — 31/31 test geçti) ve Python `src/data/mpu_pipeline.py` çapraz doğrulaması | Saf Kotlin `SessionReconstructor` (3 saat nedensel sınır, tekrarlı unlock filtresi, süreklilik sınırı, eksik başlangıç tespiti) ve `FeatureWindowCalculator` (`screen_s`, `sessions_started`, `longest_run_s`, `app_switches`, `prev_session_s`, `hour`, `dow`, `WindowQualityStatus`, UTC + yerel `ZoneId` ofset) tamamlandı. `mpu_pipeline.py` ile F01–F09 örneklerinde 0,000 sn farkla birebir eşleşti (Kapı C). |
| 2026-11-24 | `0.6.0-m09-m13` (`feat/android-m01-setup`) | M09 Dönem ayrıntısı / geri bildirim ve M13 dışa aktarım | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`ExportFormatterTest`, `MainViewModelTest` — 33/33 test geçti) | Tamamlanan 30 dakikalık dönemlerin Room kaydı, açık zaman aralıklı Evet/Hayır/Atla/Cevapsız geri bildirim ekranı (T17, çift tıklama korumalı), 30 günlük saklama temizliği ve depolama izni gerektirmeyen SAF ZIP (`manifest.json`, `feature_windows.csv`, `feedbacks.csv`, ham paket adları hariç) dışa aktarımı (T24) eklendi (Kapı B). |
| 2026-11-24 | `0.7.0-m08-m12` (`feat/android-m01-setup`) | M08 WorkManager, M11 model sınırı ve M12 hatırlatma politikası | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`BackgroundQueryWindowPlannerTest`, `ReminderAndInferencePolicyTest` — 40/40 test geçti) | 15 dakikalık tekil `WorkManager` işi (`UsageCollectionWorker`, `BackgroundQueryWindowPlanner`, duraklatmada/silmede iptal), sürüm ve kalite doğrulamalı `PredictionRunner` (`DisabledPredictor` varsayılan kapalı, şema uyumsuzluğu reddi T19, 5 dk sonrası gecikmiş mod T15) ve sınırlandırılmış `ReminderPolicy` (günlük 3 sınır, 60 dk bekleme, 23:00–08:00 sessiz saat, bastırma nedeni kaydı T21, ayrı kapatma eylemi T18) tamamlandı. Gerçek model eşliği (T20) Kaan/Yiğit model teslimine, 24 saatlik cihaz denemesi (T25, T16) fiziksel Android telefona bırakıldı. |
| 2026-11-24 | `0.7.1-device-check` (`feat/android-m01-setup`) | M14 İlk gerçek cihaz doğrulaması ve APK kurulum yönlendirmesi | Fiziksel Android cihazda debug APK kurulumu + canlı okuma, `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (40/40 test geçti) | Fiziksel Android telefonda kurulum, katılım onayı, Kısıtlı Ayarlar + Kullanım Erişimi, bildirim izni, canlı olay okuma (`149` olay, `3` sorgu), 30 dakikalık dönem üretimi (`4` pencere), kullanıcı geri bildirimi (`2` yanıt) ve 15 dk WorkManager planlaması doğrulandı (T01 gerçek cihaz kabulü geçti; 24 saatlik T25 denemesi başlatıldı). Uygulamaya Android 13+ "Uygulama Bilgilerini Aç" yönlendirmesi eklendi. |
