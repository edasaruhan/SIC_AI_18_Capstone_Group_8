# Adaptive Digital Wellbeing Coach

Samsung ve Yeşilay Innovation Campus AI programı kapsamında geliştirilen Grup 8 bitirme projesi.

Yetişkin kullanıcıların Android telefon kullanım örüntülerini öğrenerek yaklaşan yoğun kullanım dönemlerini tahmin eden, kısa ve kapatılabilir hatırlatmalar sunan bir uygulama geliştirmeyi amaçlıyoruz. Temel araştırma sorusu, çok kullanıcılı veriden öğrenilen bilginin kişiselleştirmenin ilk günlerinde ne kadar fayda sağladığıdır.

**Durum (29 Ekim 2026):** Proje planlama aşamasında; veri işleme, model eğitimi ve Android uygulaması henüz geliştirilmedi. Bu repo başlangıç çalışma yapısını içerir. **Hedef teslim/final sunum: 27 Kasım 2026.**

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

Klasörlerdeki `.gitkeep` dosyaları boş dizinlerin Git'te tutulmasını sağlar. Çalışan eğitim veya uygulama komutu henüz yoktur.

## Başlangıç

Python 3 ile repo kökünde sanal ortam oluşturun:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Windows PowerShell'de etkinleştirme komutu: `.venv\Scripts\Activate.ps1`.

Bağımlılık listesi plandaki araçları içerir; doğrulanmış ve sürümleri sabitlenmiş bir çalışma ortamı henüz yoktur. İlk çalışan ortam doğrulandığında sürümler sabitlenecektir. Android kurulumu için [Android çalışma notuna](android/README.md) bakın.

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
| [İlk proje önerisi](documents/idea_proposal.pdf) | İlk fikir metni |
| [İlk öneri sunumu](documents/idea_proposal_presentation.pdf) | İlk fikir sunumu |

Başlangıç yapısı, iki ana inceleme/uygulama belgesindeki kapsamı izler. İlk öneri ve sunumdaki daha geniş fikirler güncel plana göre değerlendirilir.

## Birlikte çalışma

Her iş için kısa, açıklayıcı bir dal açın ve değişikliği pull request ile paylaşın. Açıklamaya yapılan işi ve nasıl kontrol edildiğini yazın. Ortak veri biçimi, özellik sırası veya etiket tanımı değişirse ilgili çalışma notunu da güncelleyin. Tekrar kullanılacak kodu `src/` altında, keşif çalışmalarını `notebooks/` altında tutun.
