# Android uygulaması

**Ana sorumlu:** Murat Gürel. **Entegrasyon desteği:** Yiğit Kayalı.

Bu klasör, Adaptive Digital Wellbeing Coach projesinin tek modüllü Kotlin ve Jetpack Compose tabanlı Android uygulamasını içerir.

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

## İzinler ve gizlilik sınırları

- `android.permission.PACKAGE_USAGE_STATS` manifestte tanımlıdır; ancak tek başına erişim vermez, M02 ile sistem ayarlarından kullanıcı izni ve ayrı onam akışı eklenecektir.
- `android:allowBackup="false"` ayarı ile yerel verilerin Android otomatik yedekleme üzerinden buluta taşınması engellenmiştir.
- Onam alınmadan kullanım geçmişi sorgulanmaz; gerçek kullanım kayıtları, geri bildirimler, `local.properties`, derleme çıktıları ve imzalama anahtarları depoya eklenmez.
- Model dosyası bağlı olmadığından arayüzde sahte risk skoru gösterilmez. ONNX Runtime Mobile veya alternatif mobil format seçimi M11 aşamasında yapılacaktır.

## Çalışma ve doğrulama kaydı

| Tarih | Sürüm / Dal | Görev | Uygulanan kontroller | Durum ve notlar |
| --- | --- | --- | --- | --- |
| 2026-11-24 | `0.1.0-m01` (`feat/android-m01-setup`) | M01 Proje kurulumu | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`AppConfigTest`), emülatör (`Medium Phone API 37.0`) | Temiz Gradle derlemesi, birim testi ve emülatörde ilk ekran + elle deneme akışı doğrulandı. Gerçek Android cihaz kurulumu (T01 gerçek cihaz kabulü) fiziksel telefon testine bırakıldı. |
| 2026-11-24 | `0.2.0-m02` (`feat/android-m01-setup`) | M02 Onam ve izin akışları | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`CollectionStateResolverTest`, `MainViewModelTest` — 11/11 test geçti) | Onam ekranı, sistem Kullanım Erişimi ayar yönlendirmesi ve dönüşte yeniden denetim, isteğe bağlı `POST_NOTIFICATIONS` izni, duraklat/yeniden başlat ve tüm yerel verileri sil akışı eklendi. T02, T03, T04, T22 ve T23 birim testleri geçti; emülatör/cihaz üzeri akış kontrolü bekleniyor. |
| 2026-11-24 | `0.3.0-m03` (`feat/android-m01-setup`) | M03 Olay okuma, normalizasyon ve Koyu/Açık tema | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`UsageEventNormalizerTest`, `MainViewModelTest` — 17/17 test geçti) | `UsageStatsManager.queryEvents` tabanlı kısa aralık okuma (`30 dk / 2 saat / 6 saat`), 5 aday normalize olay türü (`SCREEN_INTERACTIVE`, `KEYGUARD_HIDDEN`, `KEYGUARD_SHOWN`, `SCREEN_NON_INTERACTIVE`, `APP_FOREGROUND`), klavye/launcher sınıflandırması, paket tokenizasyonu, reboot sonrası kilit kontrolü, Olay İnceleyici sekmesi ve kalıcı Koyu/Açık tema geçişi eklendi. Gerçek cihaz olay dizisi doğrulaması fiziksel telefon testine bırakıldı. |
| 2026-11-24 | `0.4.0-m04` (`feat/android-m01-setup`) | M04 Room yerel veri modeli ve tekrar güvenliği | `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` (`EventFingerprintTest`, `MainViewModelTest` — 19/19 test geçti) | 7 ayrı Room tablosu (`raw_usage_events`, `collection_batches`, `feature_windows`, `predictions`, `reminder_events`, `feedbacks`, `app_settings`), SHA-256 `EventFingerprint` + benzersiz indekslerle tekrar koruması (T05), `withTransaction` atomik kayıt ve checkpoint güncellemesi (T06), 7 günlük ham olay temizliği ve dışa aktarılmış şema (`schemas/.../1.json`) eklendi. |
