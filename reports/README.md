# Raporlar ve sonuçlar

Bu klasör çalışmalar ilerledikçe üretilecek paylaşılabilir çıktılar içindir:

- Veri envanteri, yeniden kullanım koşulları ve veri kalitesi raporu; proxy etiket yaygınlığı ve eşik duyarlılığı.
- Başlangıç yöntemleri ve model karşılaştırmaları; 1/3/7/14 gün koşulları, kullanıcı bazlı metrikler, günlük yanlış uyarılar ve SHAP görselleri.
- Android olay toplama, özellik eşliği, gecikme/bellek/pil ve uçtan uca test bulguları.
- Yetişkin pilotunun uygulanabilirliği, katılımcı/etiket sayıları ve proxy–geri bildirim uyumu.
- Ekip raporu ve sunum dosyaları.

Her deney raporunda kullanılan veri sürümü, bölme, etiket tanımı, rastgelelik tohumu ve model ayarları belirtilir. Ham kullanım verileri, kişisel kayıtlar ve kimlik eşlemeleri buraya konmaz.

## Mevcut model araştırmaları

- `model_exploration/`: ilk 12 kullanıcı deneyi.
- `model_exploration_all_342/`: önceki ham veri araştırma adaptörüyle tüm kullanıcılar.
- `model_exploration_thresholds/`: önceki tablo üzerinde ayrı doğrulama kullanıcılarında karar eşiği seçimi.
- `model_refinement_dataset_v1/`: İsmail'in yeni tablosunda uygun 330 kullanıcıyla kural, Logistic Regression, Random Forest ve XGBoost karşılaştırması; sınırlı ayar araması, eğitim/doğrulama/test metrikleri ve görseller.

Her klasör toplu JSON/görseller içerir. Yeni deneyin veri, oturum ve proxy tanımı eskilerden farklıdır; klasörler arasındaki skor farkı tek başına model iyileşmesini ölçmez. Aynı yeni deneydeki başlangıç/seçilmiş ayar karşılaştırması esas alınır. Yerel ödev taslakları Git dışında tutulur.

Herkes kendi alanının rapor/sunum bölümünü hazırlar; belgelerin birleştirilmesi ortak sorumluluktur.
