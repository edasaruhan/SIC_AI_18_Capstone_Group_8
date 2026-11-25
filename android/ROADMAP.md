# Android Uygulaması Geliştirme Yol Haritası

**Ana sorumlu:** Murat Gürel · **Entegrasyon desteği:** Yiğit Kayalı

Bu belge, Adaptive Digital Wellbeing Coach Android uygulamasının aşamalı geliştirme planını, görev kodlarını (`M01–M15`), ortak özellik eşlik örneklerini (`F01–F12`) ve kabul test matrisini (`T01–T25`) özetler.

## 1. Kapsam ve Bağımsız İlerleme
- **Hedef:** İzinli kullanım metaverisi toplama, yerel saklama, 30 dakikalık dönem özellik hesaplaması, dönem bazlı kullanıcı geri bildirimi, isteğe bağlı dışa aktarım ve hazır olduğunda mobil model/hatırlatma entegrasyonu.
- **Kapsam dışı (v1):** Hesap sistemi, bulut senkronizasyonu, sohbet botu, ekran görüntüsü, konum/mesaj içeriği ve uygulama engelleme.
- **Başlangıç teknik seçimleri:** Kotlin, Jetpack Compose, tek Android uygulama modülü, `minSdk 28` (Android 9+), Room yerel veritabanı (`allowBackup="false"`), UTC epoch ms + yerel `ZoneId` ofseti, 15 dk tekil `WorkManager` işi, 7 gün ham / 30 gün türetilmiş saklama, SAF ile UTF-8 CSV + JSON manifest dışa aktarımı (ham paket adları hariç).

## 2. Görev Listesi ve Güncel Durum (`M01–M15`)

| Kod | Görev | Durum | Özet Çıktı / Not |
| --- | --- | --- | --- |
| M01 | Proje kurulumu | Tamamlandı | Tek modüllü Compose iskeleti, 6 paket, fiziksel cihaz T01 doğrulandı |
| M02 | Katılım onayı ve izin akışları | Tamamlandı | Ayrı onay ve Kullanım Erişimi akışı, isteğe bağlı bildirim izni, duraklat/sil |
| M03 | Olay okuma ve normalizasyon | Tamamlandı | `UsageStatsManager` okuyucu, 5 aday olay türü, Olaylar inceleyici |
| M04 | Room yerel veri modeli | Tamamlandı | 7 tablo, dışa aktarılan şema (`1.json`), `EventFingerprint`, atomik transaction |
| M05 | Oturum çıkarma | Tamamlandı | Saf Kotlin `SessionReconstructor` (3 saat nedensel sınır, tekrarlı unlock filtresi) |
| M06 | 30 dk özellik hesaplama | Tamamlandı | 7 çekirdek özellik (`screen_s`, `sessions_started`, `longest_run_s`, `app_switches`, `prev_session_s`, `hour`, `dow`) |
| M07 | Veri kalitesi ve zaman yönetimi | Tamamlandı | `OBSERVATION_SUFFICIENT / UNCERTAIN / INVALID` ve yerel saat dilimi ofseti |
| M08 | Arka plan toplama (`WorkManager`) | Tamamlandı | 15 dk tekil periyodik iş, güvenli örtüşme, gecikmiş iş ayrımı |
| M09 | Dönem ayrıntısı ve geri bildirim | Tamamlandı | Açık zaman aralıklı Evet / Hayır / Atla / Cevapsız ayrımı |
| M10 | Python–Kotlin özellik eşliği | Tamamlandı | `src/data/mpu_pipeline.py` ile `F01–F12` üzerinde 0,000 sn farkla eşleşti |
| M11 | Mobil model adaptörü | Sınır hazır (model bekleniyor) | Sürüm/şema korumalı `Predictor` arayüzü hazır; gerçek model Kaan/Yiğit teslimine bağlı |
| M12 | Hatırlatma politikası | Politika hazır (model bekleniyor) | Günlük 3 sınır, 60 dk bekleme, 23:00–08:00 sessiz saat, bastırma nedeni kaydı |
| M13 | Dışa aktarım ve veri yönetimi | Tamamlandı | SAF ile UTF-8 CSV + JSON manifest ZIP dışa aktarımı |
| M14 | Gerçek cihaz denemesi ve pilot | Devam ediyor | İlk gerçek cihaz doğrulaması geçti; 24 saatlik deneme ve gönüllü testleri sürüyor |
| M15 | Teknik teslim ve gösterim | Planlandı | 24 saatlik deneme ve varsa model entegrasyonu sonrası final kanıt paketi |

## 3. Ortak Özellik Eşlik Örnekleri (`F01–F12`)
`FeatureParityTest` ile doğrulanmıştır (`0,001 sn` tolerans, sayımlar tam eşleşme):
- `F01`: Normal `unlock -> off` (600 sn, 1 oturum)
- `F02`: İki pencereye taşan oturum (600 sn + 600 sn; ikinci pencerede 0 yeni başlangıç)
- `F03`: Açık oturumda tekrarlı `unlock` yeni oturum sayılmaz
- `F04`: Tam `10:30` sınırı sonraki pencereye aittir (`[başlangıç, bitiş)`)
- `F05–F06`: `prev_session_s` yalnızca pencere başlangıcında veya öncesinde bitmiş son oturumdur
- `F07`: Klavye/launcher ve aynı paket tekrarları elenerek `app_switches` sayılır
- `F08`: Gelecekteki `off` olayı tahmin girdisine sızmaz (nedensellik)
- `F09`: 3 saati aşan oturumda öznitelik katkısı ilk 3 saat (`10.800 sn`) ile sınırlıdır
- `F10`: Hata/eksik başlangıç sıfır kullanım değil kalite nedeni üretir
- `F11`: Gece yarısı ve hafta başı (`Pazartesi=0 .. Pazar=6`) yerel saatle doğru hesaplanır
- `F12`: Aynı verinin tekrar okunması olay ve pencere sayısını çoğaltmaz

## 4. Kabul Test Matrisi (`T01–T25`)

| Test | Konu | Durum |
| --- | --- | --- |
| T01 | Temiz derleme ve gerçek cihaz kurulumu | Geçti (fiziksel cihaz + Gradle) |
| T02–T04 | Onay yok, izin reddi, bildirim reddi | Geçti (birim testi + cihaz) |
| T05–T06 | Tekrar sorgulama çoğalmaması ve atomik transaction | Geçti (birim testi) |
| T07–T14 | Oturum, pencere, kalite, nedensellik ve Python–Kotlin eşliği | Geçti (birim testi + `mpu_pipeline.py`) |
| T15 | 2 saat gecikmiş iş ve eski hedefe bildirim bastırma | Geçti (birim testi) |
| T16 | Reboot, ilk kilit açılışı ve force stop davranışı | Fiziksel cihaz denemesinde (M14) |
| T17–T18 | Evet/Hayır/Atla/Cevapsız ve bildirim kapatma ayrımı | Geçti (birim testi + cihaz) |
| T19 | Model/şema sürüm uyumsuzluğu reddi | Geçti (birim testi) |
| T20 | Gerçek Python–mobil model skor karşılaştırması | Kaan/Yiğit model teslimi bekleniyor |
| T21 | Sessiz saat, günlük sınır ve bekleme süresi | Geçti (birim testi) |
| T22–T24 | Duraklat/devam, tüm verileri sil ve dışa aktarım doğrulaması | Geçti (birim testi) |
| T25 | 24 saatlik gerçek cihaz çalışması ve ölçüm raporu | Devam ediyor (M14) |

## 5. Ekip Bağımlılıkları
- **İsmail (`src/data/mpu_pipeline.py`):** Oturum ve 7 özellik referansı (eşlik doğrulandı).
- **Kaan ve Yiğit (`M11`):** Mobil model dosyası, ön işleme parametreleri, karar eşiği ve örnek skorlar.
- **Hamza (`M14`):** Pilot verisi ve geri bildirim/proxy uyum değerlendirmesi.
