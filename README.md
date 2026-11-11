# Adaptive Digital Wellbeing Coach

Samsung ve Yeşilay Innovation Campus AI programı kapsamında geliştirilen Grup 8 bitirme projesi.

Yetişkin kullanıcıların Android telefon kullanım örüntülerini öğrenerek yaklaşan yoğun kullanım dönemlerini tahmin eden, kısa ve kapatılabilir hatırlatmalar sunan bir uygulama geliştirmeyi amaçlıyoruz. Temel araştırma sorusu, çok kullanıcılı veriden öğrenilen bilginin kişiselleştirmenin ilk günlerinde ne kadar fayda sağladığıdır.

**Durum (11 Kasım 2026):** Baseline kodları, önceki genel model deneyleri ve İsmail'in hazırladığı `dataset_v1` üzerinde model iyileştirme çalışması mevcut. Kişiselleştirme ve Android entegrasyonu henüz doğrulanmadı. Güncel iş, kanıtlar ve bağımlılıklar [model geliştirme planında](documents/model_development_plan.md) tutulur. **Hedef teslim/final sunum: 27 Kasım 2026.**

## Proje kapsamı

- İlk veri kaynağı, 342 katılımcının olay kayıtlarını içeren Mobile Phone Use Dataset'tir. Veri setinde doğrudan “istenmeyen kullanım” etiketi bulunmaz.
- Tamamlanan 30 dakikalık pencerenin özellikleriyle sonraki pencerenin yoğun kullanım proxy etiketi tahmin edilecektir. Proxy, kullanıcı geri bildirimiyle aynı etiket olarak kabul edilmez.
- Sabit kural ve lojistik regresyon başlangıç yöntemleri; Random Forest ve XGBoost ana model adaylarıdır.
- Genel, kişiselleştirilmiş ve yalnızca kişisel veriyle eğitilen modeller; 1, 3, 7 ve 14 günlük kişisel geçmiş koşullarında karşılaştırılacaktır.
- Android prototipi izinli veri toplama, cihazda saklama ve tahmin, hatırlatma ve kısa kullanıcı geri bildirimi akışını kapsar.

Proje farkındalık ve öz düzenlemeyi destekler; bağımlılık tanısı veya tedavisi sunmaz. Kişiselleştirmenin faydası ve proxy tahminlerinin kullanıcı değerlendirmeleriyle uyumu deneylerle incelenecektir.

## Repo yapısı

```text
.
├── android/                 # Kotlin Android projesinin kurulacağı yer
├── data/
│   ├── raw/                 # Yerel ham veri
│   └── processed/           # Yerel oturum, pencere ve özellik tabloları
├── documents/              # Mevcut PDF'ler ve ekip çalışma notları
├── models/                 # Yerel eğitilmiş modeller ve dışa aktarımlar
├── notebooks/              # Veri inceleme ve deney defterleri
├── reports/                # Veri kalitesi, deney sonuçları, pilot ve sunum çıktıları
├── src/
│   ├── data/               # Veri temizleme, oturumlar, özellikler ve proxy etiketler
│   ├── models/             # Başlangıç modelleri, eğitim ve kişiselleştirme
│   ├── evaluation/         # Veri bölme, metrikler ve deney karşılaştırmaları
│   └── integration/        # Model dışa aktarımı ve uygulama ile ortak veri dönüşümleri
├── tests/
│   └── fixtures/           # Küçük, sentetik ortak test örnekleri
├── requirements.txt        # Python başlangıç bağımlılıkları
└── .gitignore
```

Klasörlerdeki `.gitkeep` dosyaları boş dizinlerin Git'te tutulmasını sağlar. Python model kodları `src/models/`, paylaşılabilir toplu deney çıktıları `reports/` altındadır.

## Başlangıç

Python 3 ile repo kökünde sanal ortam oluşturun:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Windows PowerShell'de etkinleştirme komutu: `.venv\Scripts\Activate.ps1`.

Bağımlılık listesi henüz kullanılmayan planlı araçları da içerir; tam bir ortam kilidi değildir. Kullanılan sürümler deneylerin `results.json` dosyalarında kayıtlıdır. macOS'te XGBoost için OpenMP çalışma zamanı da gerekir (`brew install libomp`). Android kurulumu için [Android çalışma notuna](android/README.md) bakın.

Model kodunu ve testleri repo kökünde çalıştırın:

```bash
python -m pytest -q --tb=short
python -m src.models.synthetic_demo
# Yerel dataset_v1 paketi ve aynı sürümü üreten betik gereklidir:
python -m src.models.refinement_exploration
```

Son komut `data/processed/dataset_v1/` altındaki tabloları ve İsmail Hakkı Sarıoğlan tarafından hazırlanan `src/data/mpu_pipeline.py` dosyasını okur. Veriler, satır düzeyindeki tahminler ve `models/` altındaki Python model dosyaları Git dışında tutulur. Çalışma, sonraki pencerenin geçmişe dönük proxy etiketini tahmin eder; kullanıcı geri bildirimi veya mobil çalışma kanıtı değildir.

1. [Görev dağılımında](documents/task_distribution.md) kendi sorumluluğunuzu ve ilk paralel işi inceleyin.
2. Kodlamadan önce [geliştirme notlarındaki](documents/development_notes.md) veri, etiket ve değerlendirme kurallarını okuyun.
3. Yiğit koordinasyonunda ortak özellik/girdi/çıktı biçimini netleştirin; küçük veri örneği ve sentetik test girdileriyle paralel başlayın.
4. [Veri yerleşimi](data/README.md) ve [çıktı düzenine](reports/README.md) göre çalışın. Ham veri ve kişisel kayıtlar Git'e eklenmez.

## Belgeler

| Belge | İçerik |
| --- | --- |
| [Görev dağılımı](documents/task_distribution.md) | Beş kişinin ana işleri, destek ilişkileri ve ilk paralel çalışmalar |
| [Geliştirme notları](documents/development_notes.md) | Yöntem, değerlendirme ve netleştirilecek teknik kararlar |
| [Takvim](documents/schedule.md) | Kaynak plandaki tarihler ve teslim hedefi |
| [Literatür, veri ve teknoloji incelemesi](documents/literature_data_technology_review.pdf) | Araştırma dayanakları, veri sınırlılıkları ve teknoloji değerlendirmesi |
| [Kavram notu ve uygulama planı](documents/concept_note_implementation_plan.pdf) | Güncel kapsam, mimari, deney protokolü, takvim ve görev matrisi |
| [Veri hazırlama ve model araştırması](documents/data_preparation_model_exploration.pdf) | Veri hazırlama ve mevcut çevrimdışı model araştırması raporu |
| [İlk proje önerisi](documents/idea_proposal.pdf) | İlk fikir metni |
| [İlk öneri sunumu](documents/idea_proposal_presentation.pdf) | İlk fikir sunumu |

Başlangıç yapısı, iki ana inceleme/uygulama belgesindeki kapsamı izler. İlk öneri ve sunumdaki daha geniş fikirler güncel plana göre değerlendirilir.

## Birlikte çalışma

Her iş için kısa, açıklayıcı bir dal açın ve değişikliği pull request ile paylaşın. Açıklamaya yapılan işi ve nasıl kontrol edildiğini yazın. Ortak veri biçimi, özellik sırası veya etiket tanımı değişirse ilgili çalışma notunu da güncelleyin. Tekrar kullanılacak kodu `src/` altında, keşif çalışmalarını `notebooks/` altında tutun.
