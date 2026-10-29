# Veri çalışma alanı

- `raw/`: İndirilen özgün veri dosyaları. Kaynak dosyalar burada değiştirilmeden tutulur.
- `processed/`: Yeniden oluşturulan oturumlar, pencere/özellik/etiket tabloları ve deney bölmeleri.

Bu klasörlerdeki veriler `.gitignore` kapsamındadır; yalnızca klasör yer tutucuları izlenir. Sentetik ve küçük ortak test girdileri `tests/fixtures/` altında tutulur. Gerçek pilot kayıtları veya kişisel veri test örneği olarak kullanılmaz.

## Kaynak ve ilk işler

Başlangıç kaynağı **Mobile Phone Use Dataset**'tir. Kaynak bilgisi ve DOI [uygulama planının](../documents/concept_note_implementation_plan.pdf) kaynakçasında bulunur. Veri henüz bu repoya indirilmemiştir; bu iskelet indirme veya işleme yapmaz.

İsmail; yeniden kullanım koşullarını, gerekli alanları, zaman damgalarını, olay türlerini ve katılımcı bazlı kayıt kapsamını doğrular. İlk örnek tablo İsmail, Kaan, Hamza ve Yiğit tarafından ortak biçim açısından değerlendirilir.

Kaynak/sürüm, indirme tarihi, yeniden kullanım koşulları, temizleme kararları ve veri kalitesi bulguları `reports/` altında kaydedilir. Veri paylaşımı yeniden kullanım koşullarına ve pilot onamına göre ayrıca değerlendirilir; açık indirme bağlantısı sınırsız yeniden dağıtım izni sayılmaz.
