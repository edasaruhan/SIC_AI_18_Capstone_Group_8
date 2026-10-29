# Görev dağılımı

Kaynak: Ekip tarafından paylaşılan görev tablosu ve [uygulama planındaki](concept_note_implementation_plan.pdf) görev matrisi (Tablo 5). Kişi numaraları, paylaşılan görev tablosundaki sıradır.

## Ana sorumluluklar

| Kişi | Ekip üyesi | Ana sorumluluk | Yapılacak işler | Çalışma alanı |
| --- | --- | --- | --- | --- |
| 1 | İsmail Hakkı Sarıoğlan | Veri hazırlama ve özellik çıkarımı | Veri setinin incelenmesi ve temizlenmesi, kullanım oturumlarının çıkarılması, modelde kullanılacak özellik tablosunun hazırlanması | `src/data/`, `data/`, `notebooks/` |
| 2 | Kaan Özsoy | Model geliştirme ve kişiselleştirme | Tahmin hedefinin ve etiketleme yaklaşımının veri sorumlusuyla netleştirilmesi; başlangıç modelleri, Random Forest/XGBoost ve kişiselleştirme deneyleri | `src/models/`, `models/`, `notebooks/` |
| 3 | Hamza Kaya | Model değerlendirme ve deney düzeni | Kullanıcı ve zaman temelli eğitim/doğrulama/test ayrımları, veri sızıntısı kontrolleri, değerlendirme metrikleri, karşılaştırma tabloları ve pilot sonuçlarının analizi | `src/evaluation/`, `tests/`, `reports/` |
| 4 | Murat Gürel | Android uygulaması | Kullanım verilerine erişim izinleri, telefonda veri toplama ve saklama, temel ekranlar, uyarı ve kullanıcı geri bildirimi arayüzü | `android/` |
| 5 | Yiğit Kayalı | Model entegrasyonu ve sistem testleri | Uygulama verilerinin model girdisine dönüştürülmesi, tahminlerin uygulamaya bağlanması, uçtan uca testler; gerekirse backend geliştirme | `src/integration/`, `tests/`, `android/` |

Backend gereksinimi henüz kesinleşmemiştir; başlangıç mimarisi cihazda işleme ve tahmini esas alır.

## Destek ilişkileri

| İş alanı | Ana sorumlu | Destek verenler |
| --- | --- | --- |
| Veri hazırlama ve özellik çıkarımı | İsmail | Kaan, Yiğit, Hamza |
| Model geliştirme ve kişiselleştirme | Kaan | İsmail, Hamza |
| Model değerlendirme ve deney düzeni | Hamza | Kaan, İsmail |
| Android: izinler, veri toplama ve arayüz | Murat | Yiğit |
| Model entegrasyonu ve sistem testleri | Yiğit | Kaan, Murat |
| Etik, pilot organizasyonu, belgeler ve sunumlar | Tüm ekip | Ortak sorumluluk |

## İlk iki haftada paralel çalışma

Kaynak planda bu çalışmaların 19 Ekim'de paralel başlaması öngörülmüştür. 29 Ekim itibarıyla uygulama başlamadığından aşağıdaki işler başlangıç işlerini tarif eder; tamamlanmış kabul edilmez.

- **İsmail:** Küçük bir veri örneğini işleyip ilk özellik tablosunu hazırlar.
- **Kaan:** Hedef ve etiket tanımını İsmail ile netleştirir, eğitim kodunu kurar ve ilk örnekle model denemelerine başlar.
- **Hamza:** Veri bölme kurallarını, metrik kodlarını ve değerlendirme kontrollerini hazırlar.
- **Murat:** Android projesini kurar; izinler, veri toplama ve temel ekranlar üzerinde çalışır.
- **Yiğit:** Ortak veri formatını belirler; örnek girdiler ve geçici tahmin çıktılarıyla entegrasyon altyapısını hazırlar.

Her alanın bir ana sorumlusu vardır; bağlantılı işlerde birlikte çalışılır. Diğer kişilerin başlaması için veri hazırlığının tamamen bitmesi beklenmez. Herkes rapor ve sunumlarda kendi bölümünü hazırlar; pilot organizasyonu ve belgelerin birleştirilmesi ayrıca paylaşılır.
