# Python kaynak kodu

| Klasör | İçerik | Ana sorumlu |
| --- | --- | --- |
| `data/` | Olay okuma/temizleme, oturum çıkarımı, 30 dakikalık pencereler, özellikler ve proxy etiketler | İsmail |
| `models/` | Sabit kural ve lojistik regresyon, Random Forest/XGBoost eğitimi, kişiselleştirme | Kaan |
| `evaluation/` | Katılımcı/zaman temelli bölmeler, sızıntı kontrolleri, metrikler ve deney karşılaştırmaları | Hamza |
| `integration/` | Model dışa aktarımı, ortak girdi/çıktı dönüşümleri ve Python/Android eşlik kontrolleri | Yiğit |

`models/` altında baseline modeller, Random Forest/XGBoost sarmalayıcıları, ham veriyle yapılmış araştırma deneyleri ve hazırlanmış `dataset_v1` tablosunu tüketen `refinement_exploration.py` bulunur. Sonuncusu veri hazırlama hattını değiştirmeden model tarafında şema/hedef kontrolü, eğitim, doğrulamada ayar seçimi ve araştırma sonuçları üretir; Hamza'nın nihai değerlendirme altyapısı değildir.

Tekrar kullanılacak kod burada, keşif defterleri `notebooks/` altında tutulur. Kaynak kod olan `src/models/` ile yerel üretilmiş modellerin konumu `models/` ayrıdır. Güncel ilerleme [model geliştirme planında](../documents/model_development_plan.md), ortak yöntem kararları [geliştirme notlarında](../documents/development_notes.md) tutulur.

Veri hazırlama betiği `src/data/mpu_pipeline.py`, İsmail Hakkı Sarıoğlan tarafından hazırlanmıştır ve gönderilen dosyadan değiştirilmeden eklenmiştir. Ham veriler bu betikle birlikte paylaşılmaz.
