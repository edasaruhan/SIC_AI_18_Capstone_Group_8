# Veri çalışma alanı

- `raw/`: İndirilen özgün veri dosyaları. Kaynak dosyalar burada değiştirilmeden tutulur.
- `processed/`: Yeniden oluşturulan oturumlar, pencere/özellik/etiket tabloları ve deney bölmeleri.

Bu klasörlerdeki veriler `.gitignore` kapsamındadır; yalnızca README ve klasör yer tutucuları izlenir. Sentetik ve küçük ortak test girdileri `tests/fixtures/` altında tutulur. Gerçek pilot kayıtları veya kişisel veri test örneği olarak kullanılmaz.

## Kaynak ve ilk işler

Başlangıç kaynağı **Mobile Phone Use Dataset**'tir. Kaynak bilgisi ve DOI [uygulama planının](../documents/concept_note_implementation_plan.pdf) kaynakçasında bulunur. Yerel veri konumları aşağıdadır; veri indirme bu reponun model kodunun görevi değildir.

İsmail; yeniden kullanım koşullarını, gerekli alanları, zaman damgalarını, olay türlerini ve katılımcı bazlı kayıt kapsamını doğrular. İlk örnek tablo İsmail, Kaan, Hamza ve Yiğit tarafından ortak biçim açısından değerlendirilir.

Kaynak/sürüm, indirme tarihi, yeniden kullanım koşulları, temizleme kararları ve veri kalitesi bulguları `reports/` altında kaydedilir. Veri paylaşımı yeniden kullanım koşullarına ve pilot onamına göre ayrıca değerlendirilir; açık indirme bağlantısı sınırsız yeniden dağıtım izni sayılmaz.

## Mevcut yerel veriler

- Ham dataset kullanıcının yerel kopyasında `src/data/mobile_phone_use/` altında; ayrı bir Git dışlama kuralıyla korunuyor.
- İsmail'in gönderdiği hazırlanmış paket `data/processed/dataset_v1/` altında: `windows.csv.gz`, `participant_inventory.csv`, `summary.json`, `daily.csv.gz`, sütun açıklamaları ve kalite raporları. Aynı üretim izine sahip `mpu_pipeline.py` burada yalnızca yerel kaynak doğrulaması için korunuyor; ortak veri koduna taşınmadı.
- Yeni model deneyi, paketteki `usable_for_modeling` işaretine ve `target_ok` koşuluna uyar. Zaman alanı üretici betiğin yerel saate kaydırılmış epoch saniyesidir; UTC zaman damgası olarak yeniden adlandırılmaz.
- Araştırma satır tahminleri/bölmeleri `data/processed/model_refinement_dataset_v1/` altında kalır. Paylaşılabilir toplu kanıtlar `reports/model_refinement_dataset_v1/` altındadır.
