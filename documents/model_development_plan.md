# Model geliştirme ve kişiselleştirme planı

**Ana sorumlu:** Kaan Özsoy. Bu dosya model çalışmasının oturumlar arası ana takip belgesidir.

## Mevcut çalışma durumu

| Alan | Güncel kayıt |
| --- | --- |
| Current Stage | Aşama 3 tamamlandı: RF/XGBoost model API'si ve örnek ağırlığı desteği doğrulandı. Aşama 4 ve 8 devam ediyor; Aşama 5 henüz başlamadı. Aşama 1'in ortak sözleşme kararları açık. |
| Current Task | İsmail'in `dataset_v1` tablosunda kural, LR, RF ve XGBoost ile genel model iyileştirme araştırması tamamlandı. Model Refinement / Test Submission başlıklarının İngilizce içeriği yerel taslakta hazır; nihai Word teslimine aktarım yapılmadı. |
| Last Completed Task | [Yeni deney](../src/models/refinement_exploration.py), [ağaç model API'si](../src/models/tree_models.py) ve [toplu sonuçlar](../reports/model_refinement_dataset_v1/results.json): 330 uygun kullanıcı, 362.255 çift, sınırlı doğrulama araması, 86 başarılı test, 9 modelde kaydet/yükle eşliği. Önceki raporlar korundu. |
| Next Action | Yeni ödev içeriğini/görselleri kullanıcıyla gözden geçirip teslim biçimine aktar. Ardından İsmail/Hamza ile kişiselleştirme öncesi sınırlı hedef veya etiketin erişilebilirlik zamanı kararını netleştir; açık veride 1/3/7/14 kayıt günü uygunluğunu incele. Yeni pilot verisini beklemek çevrimdışı kişiselleştirmenin önkoşulu değil. |
| Current Blockers | Kaynak v1'in uzun oturum etiketi pencere dışındaki oturum bitişine ve geçmişe dönük temizliğe bağlı; tek pencere boşluğu kişiselleştirmede tek başına yeterli olmayabilir. Sabit `timezone+1` saat yaklaşımı, bilinmeyen önceki oturumun sıfırla gösterilmesi, nihai ekip şeması, Hamza'nın kronolojik protokolü ve Yiğit'in mobil arayüzü açık. Kullanıcı geri bildirimi/canlı veri ve mobil doğrulama yok; genel araştırma başarıları bunların yerine geçmez. |

## Kaynaklar ve kapsam

- [README](../README.md): Araştırma sorusu, proje kapsamı ve başlangıç yapısı.
- [Görev dağılımı](task_distribution.md): Kaan'ın model sorumluluğu ve ekip destek ilişkileri.
- [Geliştirme notları](development_notes.md): Veri, etiket, değerlendirme ve mobil kısıtlar.
- [Takvim](schedule.md): Ekip teslim bağlamı; bu dosyadaki ilerleme tamamlanmış iş ve doğrulamaya göre tutulur.
- [Veri](../data/README.md), [kaynak kod](../src/README.md), [raporlar](../reports/README.md) ve [bağımlılıklar](../requirements.txt): Mevcut çalışma alanları ve başlangıç araçları.
- [Literatür, veri ve teknoloji incelemesi](literature_data_technology_review.pdf) ile [uygulama planı](concept_note_implementation_plan.pdf): Ayrıntılı yöntem ve araştırma sınırlılıkları.

Araştırma sorusu: Çok kullanıcılı veriden öğrenilen bilgi, kişiselleştirmenin ilk günlerinde yalnızca kişisel veriye göre fayda sağlıyor mu; bu katkı kişisel geçmiş arttıkça nasıl değişiyor?

İlk kaynak 342 katılımcılı Mobile Phone Use Dataset'tir; ilişkili yayın 337 kullanıcıyı analiz eder. Veri setinde doğrudan istenmeyen kullanım etiketi yoktur. Tamamlanan 30 dakikalık pencerenin özellikleri sonraki pencerenin yoğun kullanım proxy sonucunu tahmin eder. Kullanıcı geri bildirimi ayrı bir etiket kaynağıdır. Proje farkındalık desteğidir; tanı, tedavi veya bağımlılık azalması iddiası üretmez.

Başlangıç yöntemleri sabit kural ve Logistic Regression; ana modeller Random Forest ve XGBoost'tur. Kişiselleştirme, kişisel geçmişe daha yüksek örnek ağırlığı vererek ortak veride yeniden eğitimle sınanır. LSTM, yeni model aileleri, backend, ekran görüntüsü veya kaydırma analizi bu planın ilk kapsamına eklenmez. Mobil model formatı henüz seçilmemiştir; cihazda yeniden eğitim hazır kabul edilmez.

İlk repo incelemesinde yalnızca README ve yer tutucular vardı. Bu başlangıç gözlemidir; güncel kod/deney kanıtları üst takip tablosunda ve günlükte yer alır. 11 Kasım'da İsmail'in gönderdiği hazırlanmış veri ve üretim betiği de incelendi; diğer ekip bileşenlerinin tamamlandığı varsayılmadı.

## Takip ve oturum düzeni

Durumlar: `Not Started` = başlanmadı; `In Progress` = somut çalışma sürüyor; `Blocked` = sıradaki iş kaydedilmiş bir bağımlılık nedeniyle ilerleyemiyor; `Completed` = bütün tamamlanma kriterleri kanıtla karşılandı.

Bir aşamanın bazı işleri bağımsız ilerleyebilir; tek bir dış bağımlılık bütün aşamaları otomatik olarak bloke etmez. Veri hazır olmadan yapılan sentetik test, yalnızca kod/arayüz doğrulamasıdır; gerçek veri deneyi veya model başarısı değildir. Tek sınıf, yetersiz kişisel geçmiş veya hesaplanamayan metrikler gizlenmez; gerekçesi ve geçerli örnek sayısı kaydedilir.

Her oturumda ilgili belgeleri ve Git durumunu oku, planı mevcut dosya ve sonuçlarla karşılaştır, uygun tek bir iş parçası seç, uygula ve doğrula. Ardından checkbox'ları, üstteki beş takip alanını ve günlüğü güncelle. Kod işi için çalıştırılan komut ve test sonucu; deney için veri/bölme/ayar ve çıktı konumu kanıt olarak kaydedilir. Hedef dosyaların aşağıda anılması, mevcut veya oluşturulmuş oldukları anlamına gelmez.

İlk planlama oturumunda yalnızca `AGENTS.md` ve bu plan oluşturuldu. Kullanıcının devam talebiyle Aşama 1'in ilk sözleşme çalışmasına geçildi. Çalışma küçük, doğrulanabilir iş parçaları halinde sürer; commit/push için ayrıca onay gerekir.

## Aşama 1 — Problem ve veri sözleşmesi

**Durum:** `In Progress`

**Amaç:** Tahmin problemini, modelin tükettiği tabloyu ve ürettiği çıktıyı ortak ve sınanabilir biçimde tanımlamak.

**Yapılacak işler**

- [x] Tamamlanan pencere → sonraki pencere hedefini; özellik, tahmin ve sonuç zamanlarını örnek zaman çizelgesiyle doğrula. Pencere sınırı, saat dilimi ve kayıt günü tanımındaki açık kararları ayır. Kanıt: [zaman sözleşmesi taslağı](model_data_contract.md); sentetik doğrulama kaydı aşağıdaki günlükte.
- [x] İsmail'in üreteceği özelliklerin ad, sıra, veri tipi, birim, eksiklik ve geçersiz pencere kurallarını taslaklaştır; sadece Python/Android'de ortak hesaplanabilir özellikleri esas al. Kanıt: [aday özellik şeması](model_data_contract.md#7-aday-özellik-şeması). Taslak tamamlandı; gerçek veri ve Android eşliği henüz doğrulanmadı.
- [ ] Kaan–İsmail işbirliğiyle proxy göstergelerini, eğitimde seçilecek eşikleri, gece aralığını ve etiket sürümünü netleştir; proxy ile gerçek istenmeyen kullanım arasındaki sınırlılığı kaydet.
- [ ] Tahmin olasılığı/skoru, karar etiketi, karar eşiği ve model sürümü için Yiğit'le girdi/çıktı sözleşmesini netleştir; sabit kuralın çıktı anlamını ayrıca belirt.
- [ ] Hamza'nın bölmeleri için kullanıcı kimliği, özellik/sonuç aralıkları ve geçmiş uygunluğu metaverisini tanımla; kimlik ve hedef sütunlarının yanlışlıkla özellik yapılmasını önle.
- [ ] Aynı pencere etiketi, gelecekten kişisel ortalama/geri bildirim, ön işlemenin tüm veriye uygulanması ve sınırdaki örtüşmeler gibi sızıntı risklerini kaydet.
- [ ] İsmail, Hamza ve Yiğit'ten beklenen çıktıları ve kesinleşmemiş kararları bu plana işle; sentetik sözleşme örnekleriyle bağımsız ilerlenebilecek kısmı belirle.

**Bağımlılıklar:** İsmail'in örnek tablosu ve veri kalitesi bilgisi; Hamza'nın bölme gereksinimleri; Yiğit'in ortak format koordinasyonu; Murat'ın özelliklerin cihazda üretilebilirliğine dair girdisi. Taslak hazırlanabilir; ekip çıktıları olmadan nihai şema onaylanmış sayılmaz.

**İlgili dosyalar:** [Geliştirme notları](development_notes.md), [veri notu](../data/README.md), [model veri sözleşmesi taslağı](model_data_contract.md), bu plan; ileride gerekirse `tests/fixtures/` altında sentetik örnekler. İsmail'in `src/data/`, Hamza'nın `src/evaluation/` ve Yiğit'in `src/integration/` alanları bağımlılıktır; değişiklikleri ayrıca kapsamlandırılır.

**Tamamlanma kriterleri:** Zaman ilişkisi, özellik/çıktı şeması, proxy tanımı ve sürümü, eksik/geçersiz kayıt kuralları ve bölme metaverisi belgelenmiş; ilgili ekip girdileriyle doğrulanmış; uygulamayı etkileyen açık kararlar çözümlenmiş olmalı. Yalnızca taslak üretmek aşamayı tamamlamaz.

**Doğrulama/test yöntemi:** Küçük sentetik pencere örnekleriyle özellik/sonuç ayrımı, sınır ve eksiklik senaryolarını kontrol et; örnek tablo varsa şemaya karşı incele. Her kararı kaynak belgeye veya kaydedilmiş ekip/kullanıcı kararına dayandır. Bu aşama eğitim veya performans deneyi gerektirmez.

## Aşama 2 — Baseline modeller

**Durum:** `Completed`

Kullanıcı önceliğiyle, Aşama 1'in nihai ekip doğrulaması beklenmeden yapılandırılabilir model prototipleri geliştirildi. Tamamlanma kapsamı sentetik kod/arayüz doğrulamasıdır; gerçek veri başarısı veya kesinleşmiş etiket/entegrasyon sözleşmesi değildir. Diğer ekiplerin veri işleme, değerlendirme ve entegrasyon kodu değiştirilmedi.

**Amaç:** Ana modellerin karşılaştırılacağı sabit kural ve Logistic Regression yöntemlerini çalıştırılabilir, ortak arayüzlü bir başlangıca dönüştürmek.

**Yapılacak işler**

- [x] İlk çalışan Python ortamını ve kullanılan bağımlılık sürümlerini doğrula; ortamla ilgili gereken değişikliği ayrıca kapsamlandır. Baseline için yerel `.venv`, Python 3.11.15; sürümler günlükte kayıtlı, `pip check` başarılı.
- [x] Aşama 1 sözleşmesine göre eğitim ve tahmin arayüzlerini kur; hedef/kimlik sütunları, özellik sırası ve uyumsuz girdiler için açık kontroller ekle. `fit`, `predict_scores`, `predict`; adla seçim ve kontrollü sıralama, açık girdi/etiket hataları.
- [x] Geçmiş/girdi bilgisinden gelecek pencere kararı veren sabit kuralı tanımla; gelecekteki gerçek hedefi kullanan bir “baseline” kurma. Kural ile proxy hedefinin farkını açıkla. Kural eşiği kullanıcı parametresi; model sonraki pencere etiketini üretmez/girdi yapmaz, kural skoru olasılık değildir.
- [x] Logistic Regression için gereken eksik değer işleme ve ölçeklemeyi eğitimde öğrenen pipeline'ı kur; dönüşümleri doğrulama/test verisinde yeniden öğrenme. Eğitim medyanı, eksiklik göstergeleri, ölçekleyici ve sınıflandırıcı aynı pipeline'da.
- [x] Eğitim ve tahmini küçük sentetik tabloyla çalıştır; model bazında olasılık/skor ve karar çıktısını doğrula. Demo: 48 eğitim satırı, 8 yeni sentetik tahmin; sonraki pencere oyuncak etiketi ayrı üretiliyor.
- [x] Özellik sırası, eksik/yanlış tip, sınır değerleri ve tek sınıflı eğitim durumu için anlamlı testler ekle; elde edilen sonucu bu plana kaydet. `37 passed`; yalnızca model testleri, katılımcı/zaman değerlendirmesi değil.

**Bağımlılıklar:** Kullanılabilir sözleşme taslağı; gerçek veri deneyi için İsmail'in uygun özellik/etiket tablosu ve Hamza'nın bölmeleri. Kesinleşmeyen sözleşmeyle sentetik çalışma yapılırsa geçici kararlar işaretlenir; nihai entegrasyon gibi sunulmaz.

**İlgili dosyalar:** `src/models/`, model testleri için `tests/`, sentetik girdiler için `tests/fixtures/`, keşif gerekiyorsa `notebooks/`; [requirements.txt](../requirements.txt) ortam doğrulamasının kaynağıdır.

**Tamamlanma kriterleri:** İki baseline ortak arayüzden eğitilip tahmin üretebilmeli; ön işleme yalnızca eğitimde öğrenilmeli; uygun sentetik testler geçmeli; çalıştırma komutları ve sonuçları kaydedilmiş olmalı. Gerçek veri başarısı bu aşamayı tamamlamak için uydurulmaz.

**Doğrulama/test yöntemi:** Deterministik küçük sentetik örnekler, ön işlemenin eğitim verisine bağlılığı, tahmin boyutu/değer aralığı, karar eşiği ve beklenen hata durumları. Gerçek veriyle çalıştırma varsa ayrı deney kaydı tutulur.

## Aşama 3 — Ana makine öğrenmesi modelleri

**Durum:** `Completed`

Tamamlanma model tarafındaki ortak API, örnek ağırlığı, test ve gerçek genel-model deneyi kapsamındadır; nihai ekip veri sözleşmesi, kişiselleştirme veya mobil uygunluk tamamlanmış sayılmaz.

**Amaç:** Random Forest ve XGBoost'u baseline'larla aynı veri ve arayüz düzeninde tekrar üretilebilir biçimde eğitmek.

**Yapılacak işler**

- [x] Model Exploration için Random Forest'ı mevcut iki baseline ile aynı gerçek veri araştırmasında çalıştır; ayarları ve kanıtları kaydet. Kanıt: [deney kodu](../src/models/real_data_exploration.py) ve [sonuçlar](../reports/model_exploration/results.json). XGBoost ve ana modellerin nihai ortak API'si bu işlemde tamamlanmadı.
- [x] Random Forest ve XGBoost'u ortak `fit`, `predict_proba`, `predict_scores`, `predict` arayüzüne bağla. Kanıt: `src/models/tree_models.py`; yapılandırılabilir özellikler, eğitim medyanı/eksiklik göstergeleri ve ayrı karar eşiği.
- [x] Başlangıç ayarlarını ve tohumunu kaydet. RF: 100 ağaç, derinlik 8, minimum yaprak 5, balanced; XGBoost: 150 ağaç, derinlik 3, learning_rate=0.05, min_child_weight=5, subsample=0.8, reg_lambda=1, hist, seed=42. Sınırlı arama ve seçim gerekçeleri aşağıdaki günlükte/deney JSON'unda.
- [x] Aynı veri/bölme/etiket sürümünü ve sıralı yedi özelliği yeni deney JSON'unda kaydet; kaynak SHA256'larını koru. V1 sütun anlamları eski taslakla birebir aynı varsayılmadı.
- [x] Örnek ağırlığının iki modelin öğrenmesini değiştirdiğini, sıra/boyut/geçerlilik kontrollerini sentetik testlerle doğrula. Gerçek kişiselleştirme ağırlığı seçilmedi.
- [x] RF sınıf ağırlığı yalnızca fitting etiketlerinden; XGBoost scale_pos_weight=1. Tuning yalnızca ayrı doğrulama kullanıcılarında; test skorundan ayar seçimi ve seçim sonrası refit yok.
- [x] Çalışan komut, 21 sınırlı model fit'i, 86 test, sürümler, toplu sonuçlar ve yerel model/satır çıktıları kaydedildi. Kanıt: `reports/model_refinement_dataset_v1/` ve aşağıdaki günlük.

**Bağımlılıklar:** Aşama 2 arayüzü ve ortamı; gerçek veri eğitimi için İsmail'in tablo/etiket sürümü ve Hamza'nın doğrulanmış bölmeleri.

**İlgili dosyalar:** `src/models/`, `tests/`, yerel model çıktıları için `models/`, paylaşılabilir deney kayıtları için `reports/`.

**Tamamlanma kriterleri:** İki model ortak arayüzde eğitim/tahmin ve örnek ağırlığı desteğiyle çalışmalı; ayarlar ve sürümler kayıtlı, uygun testler başarılı olmalı. Gerçek veri eğitiminin yürütülüp yürütülmediği açıkça belirtilmeli; henüz yürütülmediyse performans iddiası olmamalı.

**Doğrulama/test yöntemi:** Sabit tohumla sentetik çalışma, çıktı biçimi ve olasılık aralığı kontrolleri, örnek ağırlığının eğitim arayüzüne doğru aktarılması. Gerçek veri çalışmasında veri/bölme/ayar kaydıyla yeniden çalıştırılabilirlik kontrolü.

## Aşama 4 — Model değerlendirme ve karşılaştırma

**Durum:** `In Progress`

**Amaç:** Hamza'nın değerlendirme düzeninde bütün modelleri aynı koşullarla karşılaştırmak; genelleme ve sızıntı risklerini görünür kılmak.

**Yapılacak işler**

- [x] Kullanıcı onayıyla, Hamza'nın nihai düzeni yerine geçmeyen ilk araştırma karşılaştırmasını üret: 12 kullanıcı, 3 katlı GroupKFold, eğitim-katı proxy eşikleri, üç model, metrikler ve OOF görselleri. Kanıtlar `reports/model_exploration/` altında; kullanıcı bazlı ve satır düzeyindeki çıktılar yerel `data/processed/model_exploration/` altında.
- [x] Aynı araştırmayı mevcut 342 kullanıcının tamamına genişlet; kapsam/dışlama sayılarını, kullanıcı ayrımını ve model sonuçlarını doğrula. Kanıt: [tam veri raporu](../reports/model_exploration_all_342/results.json), `data/processed/model_exploration_all_342/` yerel çıktıları ve 53 başarılı test. Önceki alt küme raporlarının üzerine yazılmadı.
- [x] LR/RF karar eşiklerini ayrı doğrulama kullanıcılarında seç; aynı eğitilmiş modelin 0,50 kontrolü ile seçilen eşiğini dış test kullanıcılarında karşılaştır. Kanıt: [eşik çalışması](../reports/model_exploration_thresholds/results.json), 63 test, yerel bölme ve doğrulama kayıtları. Proxy/kalite duyarlılığı veya nihai bildirim bütçesi çözülmüş sayılmaz.
- [x] Kullanıcının yeni veri/XGBoost talebiyle v1 üzerinde dört yöntemi aynı kullanıcı ayrımlarıyla karşılaştır; ilk ayar/0,50 kontrolünü doğrulamada seçilen ayarla aynı deney içinde karşılaştır. Eğitim/doğrulama/test, kullanıcı metrikleri ve toplu görseller mevcut. Bu model araştırması Hamza'nın nihai protokolünün yerine geçmez; yaş kırılımı yapılmadı.
- [ ] Hamza'nın katılımcıya göre gruplanmış ve zaman temelli bölmelerini kullan; alternatif bir değerlendirme altyapısını izinsiz kurma.
- [ ] Eğitim/doğrulama/test kullanıcılarını ve özellik/sonuç aralıklarını kontrol et; genel modelin test kullanıcısını eğitimde görmediğini doğrula.
- [ ] Precision, recall, F1, PR-AUC ve confusion matrix çıktıları ile gerekli karşılaştırma görsellerini Hamza'nın metrik düzeninden elde et.
- [ ] Kullanıcı bazlı sonuçları, ortalamaları, günlük yanlış uyarıları ve geçerli örnek/gün sayılarını raporla; yanlış uyarı tanımını ve gün paydasını Hamza ile netleştir.
- [ ] Baseline, Random Forest ve XGBoost için aynı hedef, veri bölmesi ve eşik seçme politikasını kullan; modelin kendi eşiğini doğrulamada seçebilmesine ilişkin politikayı kaydet.
- [ ] Eğitim/doğrulama farkını, aşırı öğrenmeyi ve sınıf dengesizliğini incele; test verisinden ayar seçilmediğini doğrula.
- [ ] Gerçek veride karşılaştırma tablosunu, başarısız koşulları ve negatif sonuçları kaydet; yaş grubu kırılımlarını veri elverdiği ölçüde raporlamaya dahil et.

**Bağımlılıklar:** Aşama 2–3 modelleri; İsmail'in doğrulanmış gerçek veri tablosu; Hamza'nın bölme, sızıntı kontrolü ve metrik çıktıları. Değerlendirmenin ana sorumlusu Hamza'dır; Kaan model girdileri/çıktıları ve yorumlamayla destek verir.

**İlgili dosyalar:** `src/models/`, Hamza'nın `src/evaluation/` çıktıları, ilgili `tests/`, karşılaştırmalar için `reports/`; yerel bölmeler `data/processed/` altında tutulabilir.

**Tamamlanma kriterleri:** Dört yöntem gerçek veride aynı protokolle karşılaştırılmış; sızıntı kontrolleri geçmiş; kullanıcı bazlı ve toplu metrikler ile karıştırma matrisi kayıtlı olmalı. Hesaplanamayan koşullar gerekçeli olmalı. Yalnızca sentetik metrik hesabı aşamayı tamamlamaz.

**Doğrulama/test yöntemi:** Hamza'nın bölme/sızıntı testleri ve elle kontrol edilebilir küçük tahminler üzerindeki metrik kontrolleri; ardından kayıtlı gerçek veri karşılaştırması. PR-AUC hesaplama yöntemi ve tek sınıflı test durumunun nasıl raporlanacağı açıkça belirtilir.

## Aşama 5 — Kişiselleştirme deneyleri

**Durum:** `Not Started`

**Amaç:** Çok kullanıcılı bilginin cold-start koşullarında kişisel veriye katkısını ölçmek.

**Yapılacak işler**

- [ ] Global/genel modeli değerlendirme katılımcısı hariç diğer katılımcılarda eğit.
- [ ] Personal-only modeli yalnızca değerlendirme kullanıcısının önceki kayıtlarında eğit.
- [ ] Kişiselleştirilmiş modeli diğer katılımcılar ve kullanıcının önceki kayıtlarıyla, kişisel örneklere daha yüksek ağırlık vererek yeniden eğit; bunu sinir ağı fine-tuning'i olarak adlandırma.
- [ ] Kişisel örnek ağırlığını yalnızca doğrulama katılımcılarında seç; kişisel geçmiş özelliklerini yalnızca geçmişten hesapla.
- [ ] Kayıt bulunan ilk N = 1, 3, 7, 14 günü geçmiş, sonraki yedi günü test olarak kullan; sınırda bir pencere boşluk ve aralıkların örtüşmemesi kuralını Hamza'nın bölmeleriyle doğrula.
- [ ] N + 7 kayıt gününden az verisi olan katılımcıları ilgili koşuldan çıkar; pencere boşluğu ve geçerli veri kapsamından doğan ek dışlamaları da raporla.
- [ ] Aynı koşuldaki üç yaklaşımı aynı test verisinde karşılaştır; geçmiş uzunluğuna göre değişen katılımcı gruplarını ve etiket sayılarını belirt.
- [ ] Soğuk başlangıç katkısını ve kişiselleştirmenin genel modele göre farkını kullanıcı bazında ve toplu olarak analiz et; yetersiz/tek sınıflı kişisel eğitim koşullarını gizleme.
- [ ] Proxy ve geri bildirim sonuçlarını ayrı tut; pilot çıktısı varsa Hamza'yla uyumu incele. Yaklaşık bir haftalık pilotu 14 günlük kişiselleştirmenin kanıtı sayma.

**Bağımlılıklar:** Aşama 4 protokolü ve gerçek veri; yeterli kayıt günü olan katılımcılar; Hamza'nın geçmiş/test ayrımları; Aşama 3 örnek ağırlığı desteği. Geri bildirim doğrulaması, ayrıca yetişkin pilotunun izinli çıktısına bağlıdır.

**İlgili dosyalar:** `src/models/`, model testleri, Hamza'nın `src/evaluation/` çıktıları, yerel `data/processed/` ve `models/`, sonuçlar için `reports/`.

**Tamamlanma kriterleri:** 1/3/7/14 gün koşullarında üç yaklaşım gerçek veride değerlendirilmiş; ağırlık seçimi, uygun katılımcı/etiket sayıları ve katkı analizleri kayıtlı olmalı. Veri yetersizliğinin olduğu koşullar açık raporlanmalı; “tamamlandı” başarılı kişiselleştirme anlamına gelmez.

**Doğrulama/test yöntemi:** Gelecek veri erişimini, kullanıcı ayrımını, geçmiş uzunluğunu ve ağırlık atamasını sentetik zaman çizelgesiyle sınama; gerçek veride kaydedilmiş bölmelerle aynı test satırlarında üçlü karşılaştırma. Geri bildirim azsa yalnızca uygulanabilirlik bulgusu sunulur.

## Aşama 6 — Açıklanabilirlik ve model seçimi

**Durum:** `Not Started`

**Amaç:** Tahminlere katkı veren özellikleri incelemek ve performans, yanlış uyarı yükü ve mobil uygulanabilirlik açısından model seçimini gerekçelendirmek.

**Yapılacak işler**

- [ ] Random Forest/XGBoost için uygun SHAP TreeExplainer analizini kur; kullanılan örneklem ve açıklama ayarlarını kaydet.
- [ ] Özellik katkılarını toplu ve gerekli kullanıcı örnekleri düzeyinde incele; ilişkili özellikler ve kişisel geçmiş yetersizliği gibi yorum sınırlarını belirt.
- [ ] Karar eşiğini doğrulama verisinde değerlendir; precision/recall ve günlük yanlış uyarı dengesini Hamza'yla yorumla. Eşik için test verisinde ayar seçme.
- [ ] Tahmin kararı ile uygulamanın hatırlatma sınırı/sessiz saat politikasını ayır; model yanlış uyarıları ve gösterilen bildirimleri birbirine karıştırma.
- [ ] Baseline'ları da içeren sonuçlara göre seçimi veya ML üstünlük sağlamadıysa plandaki alternatif yaklaşımı gerekçelendir; SHAP'ı nedensellik veya zarar kanıtı gibi sunma.
- [ ] Yiğit'in mobil format/boyut/gecikme bulgularını seçim için mevcutsa değerlendir; yoksa seçimi mobil uygunluk açısından koşullu olarak kaydet.

**Bağımlılıklar:** Aşama 4–5 gerçek veri sonuçları; doğrulama/test ayrımı; SHAP ortam uyumluluğu; mobil uygulanabilirlik için Yiğit ve Murat'ın teknik bulguları.

**İlgili dosyalar:** `src/models/`, ilgili `tests/`, yerel `models/`; açıklamalar, eşik analizi ve seçim gerekçesi için `reports/`, keşif gerekiyorsa `notebooks/`.

**Tamamlanma kriterleri:** Gerçek model ve veri üzerinde açıklamalar, doğrulamada seçilmiş eşik ve test değerlendirmesi, sınırlılıklar ve seçim gerekçesi mevcut olmalı. Mobil uygunluk doğrulanmadıysa teslim için açık koşul olarak kalmalı.

**Doğrulama/test yöntemi:** Açıklanan tahmin, özellik sırası ve seçilen model sürümünü karşılaştır; açıklama/çıktı biçimi için uygun küçük testler uygula. Eşik karşılaştırmasını kayıtlı doğrulama bölmesinde, nihai raporlamayı ayrılmış testte yap.

## Aşama 7 — Model teslimi ve entegrasyon desteği

**Durum:** `Not Started`

**Amaç:** Seçilen modeli, beklediği girdiler ve çıktı anlamıyla birlikte Yiğit'e doğrulanabilir biçimde teslim etmek.

**Yapılacak işler**

- [ ] Seçilen modeli ve gerekli ön işlemeyi kaydet; sürüm, eğitim verisi/etiket/bölme bilgilerini ilişkilendir.
- [ ] Özellik adları ve sırası, veri tipleri, birimleri ve eksik/geçersiz girdi davranışını belgele.
- [ ] Tahmin olasılığı/skoru, karar etiketi, karar eşiği ve model sürümü çıktısını tanımla; tahmin olasılığını doğrulanmış bir klinik risk gibi sunma.
- [ ] Kaydedilen model yeniden yüklendiğinde aynı örneklerde tutarlı tahmin ürettiğini doğrula.
- [ ] Yiğit ile entegrasyon formatını ve örnek girdilerin beklenen çıktılarını doğrula; sentetik ortak örnekleri gerçek kullanıcı kayıtlarından ayrı tut.
- [ ] Murat ve Yiğit'in Python/Android özellik eşliği ve mobil çalıştırma testlerine model tarafında destek ver.
- [ ] ONNX Runtime Mobile adayı veya dışa aktarılmış karar kuralları alternatifini ekip testlerine göre değerlendir; formatı ve cihazda uyarlamayı peşinen kesinleştirme. Yapılamayan kontrolü teslim kaydında belirt.

**Bağımlılıklar:** Aşama 6 model seçimi ve gerekiyorsa koşulları; Aşama 1 sözleşmesi; Yiğit'in entegrasyon çalışması; Murat'ın Android özellik çıkarımı ve test cihazları. Model teslimi Kaan'ın, uygulamaya bağlama/sistem testi Yiğit'in, Android geliştirme Murat'ın alanıdır.

**İlgili dosyalar:** Model tarafında `src/models/` ve `tests/`, yerel model dosyaları için `models/`, teslim açıklaması için `reports/`; `src/integration/` ve `android/` ekip bağımlılıklarıdır.

**Tamamlanma kriterleri:** Yeniden yüklenebilen model, girdi/çıktı belgesi ve sürüm bilgisi mevcut; Yiğit ile format doğrulanmış; model teslim ve mobil test sonuçları veya açık sınırlılıkları kaydedilmiş olmalı. Cihazda denenmemiş çıktı mobilde çalışıyor diye sunulmaz.

**Doğrulama/test yöntemi:** Kaydetme/yükleme tahmin eşliği, sıra/tip/eksiklik kontrolleri ve ortak örneklerde entegrasyon karşılaştırması. Yapılabildiğinde cihazda uyumluluk, boyut, gecikme ve ekip ölçümleriyle bellek/pil bulguları kaydedilir.

## Aşama 8 — Model dokümantasyonu ve raporlama

**Durum:** `In Progress`

**Amaç:** Model Exploration ve final rapor/sunumunun model bölümünü yeniden izlenebilir deney kanıtlarıyla hazırlamak. Raporlama önceki aşamalarla paralel yürütülür; bütün deneylerin bitmesini beklemez.

**Yapılacak işler**

- [x] Kullanıcının paylaştığı Model Exploration gereksinimlerini incele: Model Selection (gerekçe, güçlü/zayıf yönler), Model Training (ayarlar ve çapraz doğrulama), Model Evaluation (metrikler ve görseller), Code Implementation (açıklamalı veri/özellik ve model kodu). Özel teslim dosya biçimi veya ek puanlama ölçütleri belirtilmedi.
- [x] Model Exploration için problem, yöntem, baseline ve mevcut gerçek veri deney çıktılarını içeren dört bölümlü içerik taslağını yerelde hazırla. Kullanıcı tercihiyle taslak Git dışında tutuluyor. Gerekçeler, hiperparametreler/bölmeler, metrikler/görseller ve gerçek kod alıntıları eklendi; nihai teslim biçimi kullanıcı değerlendirmesine açık.
- [x] Kullanıcının yeni Model Refinement şablonundaki altı iyileştirme ve altı Test Submission başlığı, sonuç ve kaynakça için doğrulanmış İngilizce içerik hazırla. `reports/model_refinement_submission.txt` yalnızca yerel/Git dışı taslaktır; Word şablonu doldurulmadı. Dağıtım ve kişiselleştirme uygulanmış gibi sunulmadı.
- [ ] Veri/etiket/bölme sürümleri, bağımlılık sürümleri, tohumlar, eğitim ayarları ve çalıştırma komutlarını raporla ilişkilendir.
- [ ] Aynı koşullu model karşılaştırma tablolarını, PR/confusion matrix gibi gerekli görselleri ve kişiselleştirme eğrilerini gerçek çıktılardan üret.
- [ ] SHAP bulgularını, eşik ve model seçimi gerekçesini, başarısız koşulları ve negatif sonuçları belgeye aktar.
- [ ] Tarihsel veri aktarımı, proxy geçerliliği, seyrek geri bildirim ve kısa pilot sınırlılıklarını; kapsamı büyütmeden sonraki çalışmaları açıkla.
- [ ] Final raporu ve sunumun model geliştirme bölümünü hazırla; Hamza'nın değerlendirme ve Yiğit'in teslim sonuçlarıyla tutarlılığı kontrol et.

**Bağımlılıklar:** İlgili önceki aşamaların doğrulanmış çıktıları; Model Exploration'a özgü ek gereksinimler için kullanıcının sağlayacağı yönerge; final belgelerinin birleştirilmesi için ekip koordinasyonu. Yönerge yokluğu deney kayıtlarını tutmaya engel değildir.

**İlgili dosyalar:** `reports/`, gerektiğinde `notebooks/`, bu plan ve ekip final belgeleri. İleride oluşacak rapor isimleri henüz kararlaştırılmadı.

**Tamamlanma kriterleri:** Model Exploration'ın doğrulanabilen gereksinimleri ve final model bölümü hazırlanmış; her performans iddiası gerçek bir deney kaydına bağlı; yöntem, ayar, sonuç ve sınırlılıklar tutarlı olmalı. Gerçek veri karşılaştırması yoksa bu aşamanın sonuç bölümü tamamlandı sayılmaz.

**Doğrulama/test yöntemi:** Tablo ve grafik sayılarını kayıtlı metriklerle karşılaştır; model/veri sürümlerini ve dosya bağlantılarını kontrol et; kullanıcıyla ödev beklentisini, ekiple bölüm tutarlılığını doğrula. Belge işi için gereksiz model kodu veya yeni altyapı ekleme.

## İlerleme ve karar günlüğü

| Kayıt | Yapılan iş / karar ve gerekçesi | Kanıt / durum | Sonraki adım |
| --- | --- | --- | --- |
| 2026-10-30 — Planlama | İstenen sekiz kaynak dosya ve mevcut Git/dizin durumu incelendi. Kaan'ın model işleri ile İsmail/Hamza/Murat/Yiğit bağımlılıkları ayrıldı. | Başlangıç commit'i `f4c1483`; kaynak, veri, model ve test klasörlerinde uygulama çıktısı yok. Bu kayıt gerçek veri deneyi değildir. | Kullanıcı onayı sonrası Aşama 1'in ilk sözleşme taslağı. |
| 2026-10-30 — Çalışma düzeni | Yalnızca `AGENTS.md` ve bu plan oluşturuldu. İlerleme, takvim yerine doğrulanabilir iş/çıktıya bağlı; bütün geliştirme aşamaları `Not Started`. Kesin şema, eşikler, hiperparametreler ve mobil format açık bırakıldı çünkü doğrulanmış ekip kararları yok. | Belge tutarlılık ve kapsam kontrolü bu oturumun doğrulamasıdır; model kodu/testi veya performans sonucu üretilmedi. | İlk uygulama için kullanıcı onayını bekle; commit/push ayrıca onay gerektirir. |
| 2026-10-30 — Belge doğrulaması | Kaynak belgelerle yöntem ve görev dağılımı karşılaştırıldı; tutarsızlık bulunmadı. Sekiz aşamanın zorunlu alanları, 55 açık görev, üst takip alanları, yerel bağlantılar ve iki dosyalık değişiklik kapsamı kontrol edildi. | Oturum içi Python belge kontrolü başarılı: 8 aşama, 55 işaretlenmemiş görev, yalnızca istenen iki yeni dosya, mevcut izlenen dosyalarda değişiklik yok. Model testi değildir. | Aşama 1'in tahmin hedefi ve pencere/zaman sözleşmesi taslağı için kullanıcı onayını bekle. |
| 2026-10-30 — Aşama 1 / zaman sözleşmesi | Kullanıcı devam talebiyle ilk uygulama adımını yetkilendirdi. Başlangıçta çalışma alanı temizdi; `kaan`, `origin/kaan` dalının bir commit önündeydi (`fa4a306`). Önceki planlama tekrar üretilmeden [zaman sözleşmesi taslağı](model_data_contract.md) eklendi. Yarı açık aralık yalnızca taslak öneri olarak ayrıldı; saat dilimi, hizalama, kayıt günü ve eksik pencere kararları kesinleştirilmedi. | Oturum içi `python3` doğrulaması belge tablosundaki W0/W1/W2 aralıklarını okuyarak 20 kontrolü geçti: süreler, hedef hizası, sınır olayları, bitişik örnek sızıntısı ve bir pencere atlama örneği. Yerel bağlantı, boşluk, aşama/checkbox kontrolleri ve `git diff --check` geçti. Yalnızca zaman taslağı doğrulandı; gerçek veri/model/Android testi yapılmadı. | Aday özelliklerin şema taslağı: ad/sıra/tip/birim ve eksik/geçersiz değer kuralları. Aşama 1 devam ediyor; kalan işler ve ekip bağımlılıkları açık. |
| 2026-10-30 — Aşama 1 / aday özellik şeması | Önceki oturumun commit edilmemiş iki dosyalık çalışması korundu. Sözleşmenin 7. bölümünde kaynak plandaki sekiz aday grup için ad/sıra/tip/birim, geçerlilik ve eksiklik önerileri eklendi. Sayı/süre sıfırı ile bilinmeyen kayıt; önceki tam oturum ile 30 dakikalık ekran süresi; geçmişten kişisel sapma ile cold-start eksikliği ayrıldı. Alanlar, formüller ve birimler ekip tarafından kesinleştirilmiş gibi sunulmadı. | Oturum içi `python3` kontrolü Markdown şema tablosunu ve JSON örneklerini okudu: 8 sıralı aday alan, 2 sentetik satır, fark hesabı, cold-start null ayrımı, 10 geçersiz değer örneği, eksik/yanlış sıralı alanlar ve uzun önceki oturum/negatif sapma örnekleri kontrol edildi. Kaynak notlarla kapsam, yerel bağlantılar ve `git diff --check` doğrulandı. Gerçek veri çıkarımı, Android eşliği veya model performansı test edilmedi. | Proxy karar taslağı: İsmail'le netleştirilecek göstergeleri ve eğitimde eşik/duyarlılık yaklaşımını hazırlamak. Nihai eşik seçimi veri bağımlılığı olarak açık kalır; Aşama 1 devam eder. |
| 2026-10-30 — Baseline ortamı | Kullanıcı çalışan model kodunu önceliklendirdi; yalnızca mevcut gereksinimlerdeki baseline/test bağımlılıkları yerel `.venv` içine kuruldu. Python 3.11.15, NumPy 2.4.6, pandas 3.1.0, scikit-learn 1.9.1, pytest 9.2.0 doğrulandı; diğer model ailelerinin ortamı doğrulanmış sayılmaz. | `.venv/bin/python -m pip check`: `No broken requirements found.` Aynı doğrudan sürümler `pip install numpy==2.4.6 pandas==3.1.0 scikit-learn==1.9.1 pytest==9.2.0` ile kurulabilir; bu kayıt tam bağımlılık kilidi değildir. | Çalışan ortamı baseline incelemesi için koru. |
| 2026-10-30 — Baseline uygulaması ve kararları | `src/models/` altında yapılandırılabilir `FeatureSpec`, `ThresholdRule`, `RuleBaseline`, `LogisticRegressionBaseline` ve tekrar kullanılabilir pipeline eklendi. Etiketler çağıran koddan ayrı `y` olarak alınır; proxy etiket üretimi yapılmaz. Özellikler adla seçilip sıralanır; ek kimlik/hedef sütunları girdiye katılmaz. | Kural eşikleri zorunlu parametre; skor eşleşen kural oranı, olasılık değil. Kural girdisi eksikse negatif varsaymak yerine açık hata verir. Logistic Regression karar eşiği ayrı parametre; eğitim medyanı ve eksiklik göstergeleri kullanılır. Tamamen eksik eğitim sütunu sayısal sıfır yer tutucusu ve eksiklik göstergesiyle korunur; bu gözlenen sıfır kullanım değildir. Tek sınıflı LR eğitimi açık hata verir. | Bu davranışları kullanıcıyla değerlendir; geçerli pencereyi belirlemek veri/değerlendirme sorumlularında kalır. |
| 2026-10-30 — Baseline testleri ve demo | İlk çalıştırmada özel model sarmalayıcılarının sklearn eğitim-durumu kontrolüne uyumsuzluğu görüldü; sarmalayıcılarda açık `NotFittedError` kontrolüyle giderildi. Sentetik testler ve demo tekrar başarıyla çalıştı. Önceki veri sözleşmesi korundu; yeni Markdown dosyası veya diğer ekip alanlarına kod eklenmedi. | `.venv/bin/python -m pytest -q --tb=short`: `37 passed in 0.81s`. `.venv/bin/python -m src.models.synthetic_demo`: 48 satırla eğitim, 8 yeni satırda iki modelden de `[0, 1, 0, 1, 0, 1, 0, 1]`; LR skorları `[0.0423, 0.9895, 0.0333, 0.9840, 0.0305, 0.9499, 0.0093, 0.9910]`. Oyuncak eşikler yalnızca demo/test içindir; performans sonucu değildir. | Kullanıcı değerlendirmesini bekle; Random Forest/XGBoost veya sonraki aşamaya geçme. Commit/push yapılmadı. |
| 2026-10-30 — Yerel dataset ve Git dışlama | Datasetin `src/data/mobile_phone_use/` altında bulunduğu doğrulandı. `.gitignore` içine yalnızca bu klasörü dışlayan kural eklendi; kök veri kuralı `/data/**` olarak açıkça sınırlandı. Veri taşınmadı veya değiştirilmedi. | Dataset yerelde mevcut, Git tarafından takip edilmiyor ve yeni dosya listesine girmiyor. `git check-ignore` ile dataset dışlaması; `src/data/` Python dosyalarının ve kök veri README/yer tutucularının takip edilebilirliği doğrulandı. | Yerel veri okunabilir; veri içeriği/özellik çıkarımı bu işlemde incelenmedi. Model aşaması ve önceki değerlendirme bekleme durumu değişmedi. |
| 2026-10-30 — Dataset incelemesi | Dataset PDF'sinin 3 sayfası ve Sensors.xlsx'in iki sayfası okundu. 342 CSV'nin bütün başlıkları ve tüm satırlarındaki modelle ilgili 8 alan salt okunur, parçalı tarandı. CSV'ler 4.490.195.916 bayt, 31.562.291 satır ve tek ortak 80 sütunlu şema içeriyor; kimlik eşleşmesi tam, dosya okuma hatası yok. | Yerel `data/processed/dataset_audit.json`: tüm dosyalarda ham sıra bozulması; 339 kullanıcıda On/Off/Unlocked, 3'ünde Off/Unlocked; fiilen 24 sensor_id. 642 olayın yılı 1980/2012/2015; Screen/App olaylarının tamamı 2016. PHQ8 yalnızca 64 kullanıcıda var. Anket/ESM özetleri hedef veya ileriye dönük özellik olarak alınmadı. | Ham olaylardan hazır model tablosu varsayma; sensör kapsamı, sıralama ve saat politikasını gözet. |
| 2026-10-30 — Gerçek veri araştırma kapsamı | İsmail'den hazır çıktı olmadığı kullanıcı tarafından bildirildi; mevcut ham datasetle ilerleme yetkilendirildi. `src/models/research_data.py` sınırlı araştırma adaptörü olarak eklendi; ortak `src/data/`, `src/evaluation/`, Android veya entegrasyon kodu yazılmadı. | İlk 12 alfabetik CSV önceden tanımlı örneklem; temsil gücü iddiası yok. 30 dakikalık yarı açık UTC pencereleri; >30 dakika kayıt boşluğu, kesik/belirsiz oturum ve >120 dakika oturum için açık araştırma dışlamaları. On olayı tek başına unlock-to-Off oturumunu bitirmiyor. Boşluktan sonra ekran durumu yeniden gözlenmeden kullanım yok varsayılmıyor. Önceki oturum süresi yalnızca tahmin sınırından önce biten, boşluk kesmeyen oturumlardan geliyor. | Oturum ve kayıt boşluğu eşiklerinin duyarlılığını sınamadan nihai veri kalitesi kuralı sayma. |
| 2026-10-30 — Özellik, proxy ve bölme kararları | İlk deney yalnızca ekran aktif süresi, unlock sayısı, filtrelenmiş uygulama geçişi ve önceki tamamlanmış oturum süresini kullanıyor. Launcher (+), klavye (=), sistem (-) ve boş uygulama olayları dışlandı. Yerel saat/gece ve kişisel sapma belirsizlikleri bu deneye taşınmadı. | Proxy: sonraki penceredeki en uzun kesintisiz unlocked kullanım parçası VEYA unlock sayısı, eğitim kullanıcılarının ilgili 90. yüzdelik eşiğini geçiyorsa pozitif; sıfır kullanımın pozitif olmaması için alt sınırlar 1 saniye/1 unlock. Üç GroupKFold katında 8 eğitim / 4 test kullanıcısı; eşikler sadece eğitimden öğrenildi. Bu nihai istenmeyen kullanım etiketi değil, sınırlı operasyonel proxy. | Proxy ve kapsam duyarlılığını önceden belirtilmiş ayarlarla kontrol et; test sonuçlarından gizlice eşik seçme. |
| 2026-10-30 — Gerçek veri sonuçları | Seçilen dosyalarda 1.140.110 ham satır, 14.603 aday pencere, 13.092 geçerli pencere ve 12.830 geçerli ardışık pencere çifti üretildi. İlgili alt kümede dışlanan yıl-anomalili satır yok; tüm datasetin anomalileri ayrı auditte kayıtlı. Ayarlar, fold sonuçları, ortam, kaynak SHA256'ları ve kullanıcı metrikleri kaydedildi. | `.venv/bin/python -m src.models.real_data_exploration`: başarılı. Kat ortalaması F1 / AP: kural 0.488 / 0.347, LR 0.319 / 0.460, RF 0.489 / 0.449. LR C=1, max_iter=1000, karar=0.5; RF 100 ağaç, max_depth=8, min_samples_leaf=5, balanced, seed=42. Eşik araması yok. RF'nin F1'i kurala çok yakın, yanlış uyarısı daha yüksek; nihai üstünlük iddiası yok. | Model seçimini yalnızca bu ön deneye dayandırma; dört ödev başlığı için kanıtları kullanırken sınırlarını belirt. |
| 2026-10-30 — Doğrulama ve yerel çıktılar | Baseline testlerine audit/parça sınırı, zaman sırası, oturum sınırları, eksiklik, boşluk sonrası durum, sonraki pencere eşleme ve kullanıcı ayrımı testleri eklendi. Confusion matrix ve ROC/PR PNG'leri görsel kontrol edildi; PR basamaklı çizildi. Matplotlib önbelleği yazılabilir geçici dizine yönlendirildi. | `python -m pytest -q --tb=short`: 51 test başarılı. `pip check` başarılı. Audit ve özellik/tahmin satırları `data/processed/` altında Git dışı; `reports/model_exploration/` yalnızca toplu sonuç/grafik içeriyor. Ham dosyalar değiştirilmedi. pypdf/openpyxl inceleme için, matplotlib mevcut gereksinimdeki görseller için kuruldu. | İlerlemeyi gerçek ön deney olarak koru; nihai kişiselleştirme/pilot doğrulaması gibi sunma. Commit/push yapılmadı. |
| 2026-10-31 — Git paylaşımı öncesi kontrol | Kullanıcı mevcut çalışmayı commit/push ve main birleştirmesi için yetkilendirdi. Önce `git fetch origin` çalıştırıldı; uzak kaan ve main dallarında yeni commit olmadığı doğrulandı. | `.venv/bin/python -m pytest -q --tb=short`: 51 test başarılı. Ham dataset, türetilmiş satır verileri ve datasetle taşınan referans PDF Git dışında tutuluyor; kod/testler, sözleşme ve toplu deney sonuçları paylaşım kapsamına alındı. Araştırma aşaması bu işlemle değişmedi. | Kaan dalını senkronize edip commit/push yap; ardından main dalına birleştir ve uzak dal durumunu doğrula. |
| 2026-10-31 — Tam veri kapsamı | Kullanıcının devam talebi üzerine mevcut üç model ve dört özellik korunarak 342 kullanıcıda deney çalıştırıldı. Model/etiket/kalite ayarları değiştirilmedi; yalnızca örneklem kapsamı genişletildi. Kapsam raporu seçilen/değerlendirilen kullanıcıları ve geçerli/geçersiz pencereleri ayıracak şekilde eklendi; eğitim başlamadan yerel hazırlık çıktıları kaydediliyor. | `.venv/bin/python -m src.models.real_data_exploration --participants 342 --folds 3 --output reports/model_exploration_all_342 --workdir data/processed/model_exploration_all_342`: başarılı. Önceki 12 kullanıcı raporlarının üç dosyası SHA256 ile değişmemiş olarak doğrulandı. | Sonuçları tam kullanıcı kapsamlı operasyonel proxy araştırması olarak yorumla. |
| 2026-10-31 — Kapsam ve dışlamalar | 31.562.291 olaydan 642 yıl-anomalili olay dışlandı. 444.012 aday pencereden 334.165'i kalite kurallarına göre geçerli; 109.847'si geçersiz. Geçerli pencerelerin 14.551'inin geçerli komşu hedefi yok veya son pencere; 319.614 ardışık çift kaldı. Tüm kullanıcılar en az bir çiftle dahil; tamamen dışlanan kullanıcı yok. | 342 kaynak dosyası ve SHA256 manifesti yerelde kayıtlı. Kullanıcı başına 37–3770 pencere çifti. Fold eğitim/test kullanıcı sayıları: 229/113, 227/115, 228/114; test satırları 106.525, 106.560, 106.529. Kullanıcılar ayrımlar arasında çakışmıyor, her kullanıcı/model yalnızca bir test katında. | Kalite eşiği duyarlılığı yapılmadığını ve geçmişe dönük kalite seçiminin canlı kapsam garantisi vermediğini belirt. |
| 2026-10-31 — Tam veri model sonuçları | Kat ortalaması precision/recall/F1/AP: kural 0.510/0.540/0.525/0.383; LR 0.636/0.319/0.425/0.529; RF 0.425/0.727/0.536/0.547. Günlük yanlış uyarı metriği aynı sırayla 3.811, 1.337, 7.244. RF'nin F1 artışı kurala göre küçük ve yanlış uyarısı daha yüksek; düşük yanlış uyarı isteyen kullanımda LR'nin recall kaybı var. | Proxy eşikleri her katın eğitim kullanıcılarında öğrenildi: en uzun kullanım parçası 457.057 / 427.051 / 473.6352 saniye; unlock eşiği her katta 3. Bunlar nihai/klinik eşikler değil. Gece bileşeni yok. Sayılar 12 kullanıcı deneyiyle doğrudan bir öğrenme eğrisi gibi karşılaştırılmaz; kullanıcı/etiket dağılımları değişti. | Nihai modeli bu tek çalışma sonucuna göre kesinleştirme; karar eşiği doğrulamasını testten ayrı tut. |
| 2026-10-31 — Tam veri doğrulaması | İki yeni test kapsam/dışlama özetini ve tüm seçilmiş kullanıcıların raporlanmasını doğruluyor. Tüm testler 53 başarılı. Gerçek çıktılarda kullanıcı ayrımı, zaman aralıkları, 958.842 OOF tahmini, confusion matrix toplamları ve kat AP/F1 değerleri yeniden hesaplanarak doğrulandı. Grafikler görsel kontrol edildi. | Dört kullanıcıda yalnızca negatif proxy etiketi var; kullanıcı bazlı ROC-AUC/AP tanımsız olduğunda null tutuluyor, bu kullanıcılar gizlice dışlanmadı. Satır verileri/manifest/loglar Git dışında; toplu sonuç ve görseller `reports/model_exploration_all_342/` altında. Kod, testler ve bu plan dışında önceki çalışma değiştirilmedi. | Kullanıcıyla sonuç değerlendirmesi; commit/push için yeni onay gerekir. |
| 2026-11-01 — Ödev odağı ve karar eşiği | Kullanıcı Model Exploration'ın dört başlığına içerik üretmeyi önceliklendirdi; XGBoost planlanan çalışma olarak bırakıldı. Mevcut 342 kullanıcı pencere tablosu kullanıldı, 342 kaynak dosyanın SHA256 kayıtları doğrulandı; ham olaylar yeniden işlenmedi. Yeni model ailesi, Android/backend veya ortak veri/değerlendirme altyapısı eklenmedi. | `src/models/threshold_exploration.py`: aynı 3 dış GroupKFold içinde geliştirme kullanıcılarının %20'si GroupShuffleSplit ile doğrulamaya ayrıldı (seed=42). Eğitim/doğrulama/test sayıları 183/46/113, 181/46/115, 182/46/114. Proxy eşikleri de yalnızca iç eğitimden öğrenildi. | Bu çalışma içindeki 0,50 kontrolüyle karşılaştır; eski tam veri çalışmasındaki farklı eğitim/etiket dağılımını eşik etkisi sanma. |
| 2026-11-01 — Eşik seçim yöntemi | LR ve RF için 0,05–0,95 arası 0,05 adımlı önceden belirlenmiş gridde doğrulama F1'i en yüksek eşik seçildi; eşitlikte yüksek eşik. Seçim sonrasında yeniden eğitim yapılmadı; dış testte kontrol/seçilen politika aynı model, skor ve etiketleri kullanıyor. Kural baseline değişmedi. | LR eşikleri 0,20/0,15/0,20; RF eşikleri 0,65/0,55/0,60. Dış test etiketleri seçim fonksiyonuna girmiyor. F1 ölçütü bir günlük uyarı bütçesi sağlamaz. | Uygulamaya tek bir küresel eşik atanmış veya olasılıklar kalibre edilmiş gibi sunma. |
| 2026-11-01 — Kontrollü sonuçlar | Bu çalışma içinde LR F1 0,422→0,537, yanlış uyarı metriği 1,332→6,193; RF F1 0,532→0,544, yanlış uyarı metriği 7,458→5,202. Seçilen eşikle RF precision/recall/AP 0,477/0,640/0,544; LR 0,446/0,677/0,527. Kural F1/AP 0,523/0,381. AP/ROC-AUC eşik değiştirmekle değişmedi. | `.venv/bin/python -m src.models.threshold_exploration`: başarılı; toplu rapor/grafikler `reports/model_exploration_thresholds/`, bölme/validation grid ve kullanıcı metrikleri yerel `data/processed/model_exploration_thresholds/` altında. Bu gerçek istenmeyen kullanım veya kullanıcıya gösterilen bildirim sayısı değil, operasyonel proxy değerlendirmesi. | RF'yi ana araştırma adayı olarak gerekçelendir; küçük avantajı nihai/istatistiksel üstünlük veya mobil uygunluk diye sunma. |
| 2026-11-01 — Ödev içeriği ve doğrulama | `reports/model_exploration_submission.txt` içinde İngilizce dört bölüm hazırlandı: Model Selection, Model Training, Model Evaluation, Code Implementation. Tam veri ve yeni eşik deneyleri ayrı anlatıldı; hiperparametreler, kullanıcı/satır bölmeleri, confusion matrix ve ROC/PR görselleri, test edilmiş kaynaklardan açıklamalı kod alıntıları, sınırlılıklar ve çalıştırma komutları eklendi. Yeni Markdown dosyası oluşturulmadı. | 63 test başarılı. Üçlü kullanıcı ayrımı, doğrulama gridinden eşik seçimi, 1.598.070 politika-tahmin satırı, aynı test skorları ve confusion matrix/F1 yeniden hesaplamaları doğrulandı. Önceki 6 rapor/görsel dosyasının SHA256'ları değişmedi; yeni grafikler görsel kontrol edildi. Kaynak/bölme/satır verileri Git dışında. | Kullanıcı incelemesi ve teslim biçimine aktarım. Commit/push yapılmadı; XGBoost, SHAP, kişiselleştirme ve proxy/kalite duyarlılığı yapılmış gibi işaretlenmedi. |
| 2026-11-01 — Paylaşım kapsamı | Kullanıcı mevcut kod/test/deney çalışmalarının commit ve push edilmesini, ödev taslağının paylaşım dışında tutulmasını istedi. Taslak silinmeden yalnızca ilgili dosya .gitignore kapsamına alındı; plandaki taslak bağlantıları yerel/Git dışı notuna çevrildi. | Fetch sonrası uzak kaan dalında yeni commit yok; 63 test başarılı. Paylaşım kapsamı: model kodu, testler, tam veri ve eşik deneylerinin toplu JSON/görselleri, ilerleme planı ve dışlama kuralı. Ham/türetilmiş satır verileri ve taslak yerelde kalıyor. | Bu işlemde kaan dalına commit/push; main birleştirmesi kapsamda değil. |
| 2026-11-02 — PDF paylaşımı | Kullanıcı yeni veri hazırlama/model araştırması PDF'sinin commit/push edilmesini ve main dalına birleştirilmesini istedi. PDF değiştirilmeden eklendi; README belge dizinine bağlantı kondu. Model aşamaları ve yerel taslak dışlaması korundu. | PDF 14 sayfa, şifresiz; bütün sayfalar pypdf ile okunabildi. Bu işlem içerik/deney doğrulaması değildir; kod değişmediği için model testleri yeniden çalıştırılmadı. | kaan ve main dallarında paylaşımı doğrula; mevcut model araştırması ve ekip bağımlılıkları açık kalır. |
| 2026-11-11 — Yeni oturum ve veri teslimi | Temiz `kaan` dalı, son commit `95aa5c1`; önceki kod/testler, plan, görev dağılımı, araştırma/uygulama ve son veri-model raporu okundu. Başlangıçta 63 test başarılı. Kullanıcı İsmail'in `Downloads/dataset_v1` paketini ve ardından `Downloads/mpu_pipeline.py` yolunu verdi; yeni veriyle LR/RF ve XGBoost çalışmasını yetkilendirdi. | Paket değişmeden `data/processed/dataset_v1/` altına kopyalandı; her dosyanın kaynak/kopya SHA256'sı eşleşti. Betiğin tam SHA256'sı `f12cd4501922bf0d7fc040ccbc811eeb6d513c9a09784b5231eb6903c2eb03b5`, üretici özetindeki önekle aynı. Betik yalnızca yerel kopyada korundu; İsmail'in ortak kodu değiştirilmedi. | Hazır tabloyu model tarafında doğrula; eski ham veri adaptörünü yeniden çalıştırma veya v1 sonuçlarını eski skorlarla doğrudan iyileşme sayma. |
| 2026-11-11 — V1 hedef/uygunluk kontrolü | 443.781 pencerenin tümünde sıralı 1.800 saniyelik kullanıcı ızgarası, benzersiz anahtar, valid/target_ok, dört hedefin doğru komşu etiketi, etiket birleşimi, gece/rapid kuralları ve envanter/özet sayıları doğrulandı. 342 kullanıcıdan 330'u üreticinin modelleme uygunluk kuralını geçiyor; 12 kullanıcı ve 4.107 hedefli satır dışarıda. | Deney: 362.255 çift, 41.968 pozitif (%11,59), hedef eşleme hatası 0. İlk beş kullanıcının 6.464 penceresi özgün ham kayıtlardan kaynak betikle eşleşti; 70 örnek/stres penceresinde gelecekten kesme kontrolü 0 uyuşmazlık. Örnek doğrulamasıdır; tüm kullanıcı/kenar durumları için nedensellik kanıtı değildir. Yerel `independent_validation.json` kayıtlı. | Kaynak belirsizliklerini koru: valid bir heartbeat olayı demektir, kesintisiz gözlem garantisi değil; prev_session_s başlangıç sınırındadır ve geçmiş yokluğu sıfır olabilir. |
| 2026-11-11 — Etiket ve saat sınırları | V1 proxy sabit 20 dk tam oturum / 00–05'te 5 dk temiz ekran / 6 oturum başlangıcı birleşimidir. Uzun bileşen pencereye temas eden geçerli oturumun tam süresini kullanır; sonuç pencere dışındaki bitişe ve 3 saatlik geçmişe dönük kabul kuralına bağlıdır. Bu çalışmada eşikler fitting yüzdeliklerinden öğrenilmedi; kaynaktaki sabit tanım korundu ve testten optimize edilmedi. | Kaynak saat `ts_raw/1000 + (timezone+1)*3600`; saat/gün alanları kendi kaydırılmış zamanı ile tutarlı ama gerçek DST geçmişi doğrulanmadı. Yeni yanlış uyarı paydası yerel tahmin tarihi, eski raporda UTC idi. Eski ve yeni deney farkı yalnızca model değişikliğine atfedilemez. Kaynak lab_any kalıcılığı gelecekte biten oturumu bildiğinden nedensel rakip olarak kullanılmadı. | Kronolojik kişiselleştirmeden önce sınırlı proxy veya etiket erişilebilirlik/sonuç bitiş metaverisini İsmail/Hamza ile netleştir. Tek pencere boşluğunun yeterli olduğunu mevcut v1 uzun etiketi için varsayma. |
| 2026-11-11 — Model API ve ortam | RF/XGBoost sarmalayıcıları ortak eğitim/skor/karar API'si, yapılandırılabilir özellikler, fitting-only medyan/göstergeler ve örnek ağırlığı desteğiyle eklendi. Ağırlığın gerçekten öğrenmeyi değiştirdiği ve indeks/şekil/sonluluk/etkin sınıf kontrolleri test edildi. | XGBoost 3.2.0 ve macOS'te gereken libomp kuruldu; requirements içinde XGBoost sürümü sabitlendi. Deney Python 3.11.15, NumPy 2.4.6, pandas 3.1.0, sklearn 1.9.1, matplotlib 3.11.2, joblib 1.6.0. `pip check` başarılı. Önceki baseline çalışması/raporları korundu. | Örnek ağırlığı API'sini kişiselleştirme sonucuyla karıştırma; gerçek kişisel ağırlık seçimi ve adaptasyon henüz yapılmadı. |
| 2026-11-11 — Sınırlı genel model iyileştirmesi | `.venv/bin/python -m src.models.refinement_exploration` başarıyla tamamlandı. Ortak yedi kaynak özelliği: screen_s, sessions_started, longest_run_s, app_switches, prev_session_s, hour, dow. Notifs, geçmiş sapmaları/hist_days, etiketler/retrospektif temiz süre ve kimlikler dışarıda. Otomatik özellik seçimi/ablation yok. | 3 dış GroupKFold; her katta 176 fitting / 44 validation / 110 test kullanıcısı. Satırlar: 191.668/49.823/120.764; 194.517/47.011/120.727; 190.767/50.724/120.764. Aynı eski kullanıcı bazlı bölme/0,05–0,95 eşik politikası; inner tek holdout, inner k-fold değil. LR 2, RF 2, XGBoost 3 önceden belirtilmiş konfigürasyon, toplam 21 fit. Maksimum validation F1; test seçimde kullanılmadı, refit/early stopping yok. | Aday sayısını test sonuçlarına bakarak genişletme. Hamza'nın nihai altyapısına veya Android/entegrasyon alanlarına kod eklenmedi. |
| 2026-11-11 — Ayar ve sonuçlar | LR C=1 tüm katlarda; eşik 0,25. RF depth=12/leaf=10 tüm katlarda; eşikler 0,70/0,65/0,75. XGBoost depth=5/150 ağaç, depth=3/300, depth=5/150; eşikler 0,25/0,25/0,30. Diğer sabit ayarlar deney JSON'unda; sınıf ağırlıkları yalnızca fitting'de. | Ortalama dış test precision/recall/F1/AP: kural 0,520/0,396/0,450/0,278; LR 0,488/0,520/0,504/0,488; RF 0,519/0,533/0,524/0,541; XGBoost 0,520/0,536/0,527/0,542. Günlük yanlış pozitif aynı sırayla 1,740/2,595/2,386/2,350. LR ilk/seçili F1 0,438→0,504; RF 0,456→0,524; XGBoost 0,453→0,527. Bu farklar ortak veri içinde ayar+eşik etkisidir; yeni veri etkisinden ayrılmamış eski skor karşılaştırması değildir. | XGBoost'un RF'ye yaklaşık 0,0027 F1 avantajı küçük; kesin/istatistiksel üstünlük veya nihai tek model seçimi iddiası yok. |
| 2026-11-11 — Doğrulama, çıktı ve ödev | 86 test başarılı (`3.53s`); kullanıcı ayrımı ve 1.449.020 test politika tahmininin kapsamı, validation konfigürasyon/eşik seçimleri, confusion/F1/AP/ROC-AUC bağımsız hesaplarla doğrulandı. Dokuz seçili modelin tüm test skorları/kararları joblib kaydet/yükle sonrası birebir aynı. Yeni üç PNG görsel kontrol edildi. | Toplu JSON/grafikler `reports/model_refinement_dataset_v1/`; satır/bölme/aday eğrileri `data/processed/model_refinement_dataset_v1/`; modeller `models/refinement_dataset_v1/`. Model Refinement/Test Submission içeriği ve açıklamalı kod parçaları `reports/model_refinement_submission.txt` yerel/Git dışı taslakta. Kaynak Word şablonu değiştirilmedi. README'lerdeki eski “henüz kod/sonuç yok” ifadeleri güncellendi; yeni Markdown yok. | İçeriği kullanıcıyla değerlendir ve teslim biçimine aktar. Ardından açık veride kişiselleştirme protokolünü netleştir; hist_days/valid_days 48-pencere eşdeğeridir, farklı kayıt günü sayısıyla karıştırma. Bu oturumda commit/push yapılmadı. |

| 2026-11-11 — Paylaşım öncesi tutarlılık | Kullanıcı commit/push ve main birleştirmesini yetkilendirdi. Fetch yapıldı; Markdown belgelerinde hedef protokol, eski tarihli başlangıç kayıtları ve uygulanan v1 araştırması açıkça ayrıldı. | Veri/modeller ve iki yerel ödev taslağı Git dışında; paylaşım kod, test, toplu sonuç/görseller ve güncel belgelerle sınırlı. Test ve bağlantı kontrolleri paylaşım öncesinde doğrulanıyor. | kaan commit/push, main birleştirmesi ve uzak dal eşliğini doğrula. |

| 2026-11-11 — İsmail’in kaynak betiği | Kullanıcı talebiyle İsmail Hakkı Sarıoğlan’ın `mpu_pipeline.py` dosyası değiştirilmeden `src/data/` altına eklendi; model CLI varsayılan kaynak yolu ve README’ler güncellendi. Önceki yerel tutma kayıtları tarihsel kayıttır. | Kaynak SHA256 aynı: `f12cd4501922bf0d7fc040ccbc811eeb6d513c9a09784b5231eb6903c2eb03b5`. Commit mesajı kodun İsmail’e ait olduğunu açıkça belirtiyor. | Model aşamaları değişmedi; sonraki iş hedef zaman sınırı ve kişiselleştirme protokolü. |
