# Model geliştirme ve kişiselleştirme planı

**Ana sorumlu:** Kaan Özsoy. Bu dosya model geliştirme hedeflerini, doğrulanmış sonuçları ve açık teknik işleri izler.

## Mevcut çalışma durumu

| Alan | Güncel kayıt |
| --- | --- |
| Current Stage | Aşama 3 tamamlandı: RF/XGBoost model API'si ve örnek ağırlığı desteği doğrulandı. Aşama 4 ve 8 devam ediyor; Aşama 5 henüz başlamadı. Aşama 1'in ortak sözleşme kararları açık. |
| Current Task | İsmail'in `dataset_v1` tablosunda genel model iyileştirme araştırması tamamlandı; yöntem ve sonuçlar [Model Refinement and Test Submission raporunda](model_refinement_test_submission.pdf) sunuluyor. |
| Last Completed Task | [Yeni deney](../src/models/refinement_exploration.py), [ağaç model API'si](../src/models/tree_models.py) ve [toplu sonuçlar](../reports/model_refinement_dataset_v1/results.json): 330 uygun kullanıcı, 362.255 çift, sınırlı doğrulama araması, 86 başarılı test, 9 modelde kaydet/yükle eşliği. Önceki raporlar korundu. |
| Next Action | İsmail/Hamza ile kişiselleştirme öncesi sınırlı hedef veya etiketin erişilebilirlik zamanı kararını netleştir; açık veride 1/3/7/14 kayıt günü uygunluğunu incele. Yeni pilot verisini beklemek çevrimdışı kişiselleştirmenin önkoşulu değil. |
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

## İlerleme ölçütleri

Bir iş, ilgili kod, test veya doğrulanmış çıktı mevcut olduğunda tamamlanır. Sentetik kontroller gerçek veri performansından; proxy etiketler kullanıcı geri bildiriminden ayrı değerlendirilir. İlerleme kayıtları deney amacı, veri/bölme/ayarlar, sonuçlar, önemli kararlar ve açık bağımlılıklarla sınırlı tutulur. Ayrıntılı sayılar ve ortam sürümleri ilgili deney JSON dosyalarında yer alır.

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

- [x] Genel modellerin veri hazırlama, eğitim ve değerlendirme sonuçlarını [Model Exploration raporunda](data_preparation_model_exploration.pdf) sun.
- [x] V1 genel model iyileştirmesini, doğrulamada ayar seçimini ve test sonuçlarını [Model Refinement and Test Submission raporunda](model_refinement_test_submission.pdf) sun. Kişiselleştirme ve Android dağıtımı gelecek çalışma olarak ayrıldı.
- [ ] Veri/etiket/bölme sürümleri, bağımlılık sürümleri, tohumlar, eğitim ayarları ve çalıştırma komutlarını raporla ilişkilendir.
- [ ] Aynı koşullu model karşılaştırma tablolarını, PR/confusion matrix gibi gerekli görselleri ve kişiselleştirme eğrilerini gerçek çıktılardan üret.
- [ ] SHAP bulgularını, eşik ve model seçimi gerekçesini, başarısız koşulları ve negatif sonuçları belgeye aktar.
- [ ] Tarihsel veri aktarımı, proxy geçerliliği, seyrek geri bildirim ve kısa pilot sınırlılıklarını; kapsamı büyütmeden sonraki çalışmaları açıkla.
- [ ] Final raporu ve sunumun model geliştirme bölümünü hazırla; Hamza'nın değerlendirme ve Yiğit'in teslim sonuçlarıyla tutarlılığı kontrol et.

**Bağımlılıklar:** İlgili önceki aşamaların doğrulanmış çıktıları; Model Exploration'a özgü ek gereksinimler için kullanıcının sağlayacağı yönerge; final belgelerinin birleştirilmesi için ekip koordinasyonu. Yönerge yokluğu deney kayıtlarını tutmaya engel değildir.

**İlgili dosyalar:** `reports/`, gerektiğinde `notebooks/`, bu plan ve ekip final belgeleri. Mevcut raporlar yukarıda bağlantılıdır; final rapor ve sunum ayrıca hazırlanacaktır.

**Tamamlanma kriterleri:** Model Exploration'ın doğrulanabilen gereksinimleri ve final model bölümü hazırlanmış; her performans iddiası gerçek bir deney kaydına bağlı; yöntem, ayar, sonuç ve sınırlılıklar tutarlı olmalı. Gerçek veri karşılaştırması yoksa bu aşamanın sonuç bölümü tamamlandı sayılmaz.

**Doğrulama/test yöntemi:** Tablo ve grafik sayılarını kayıtlı metriklerle karşılaştır; model/veri sürümlerini ve dosya bağlantılarını kontrol et; kullanıcıyla ödev beklentisini, ekiple bölüm tutarlılığını doğrula. Belge işi için gereksiz model kodu veya yeni altyapı ekleme.

## Deney ve karar özeti

| Tarih | Çalışma ve temel karar | Kanıt / sonuç |
| --- | --- | --- |
| 30 Ekim 2026 | Tamamlanan 30 dakikalık pencereyle sonraki pencereyi hedefleme ve aday özellik sözleşmesi hazırlandı. Ortak Android şeması henüz kesinleşmedi. | [Veri sözleşmesi](model_data_contract.md). |
| 30 Ekim 2026 | Yapılandırılabilir kural ve LR baseline'ları geliştirildi. Eğitimde öğrenilen medyan/göstergeler, LR ölçekleme, açık özellik seçimi ve eksik/geçersiz girdi kontrolleri eklendi. | `src/models/baselines.py`, sentetik demo ve 37 baseline testi. Sentetik başarı gerçek kullanım performansı değildir. |
| 30 Ekim 2026 | Ham dataset auditi: 342 dosya, 31.562.291 olay, ortak 80 sütun; modelle ilgili alanlar tarandı. Olay sırası, kayıt boşluğu ve anket eksiklikleri belirlendi. | `src/models/dataset_audit.py`; satır verileri yerel tutulur. |
| 30 Ekim 2026 | İlk araştırma: alfabetik ilk 12 kullanıcı, 4 özellik, UTC pencereleri, 3 katlı GroupKFold. Proxy eşikleri yalnızca eğitim kullanıcılarının 90. yüzdeliklerinden öğrenildi. Kalite varsayımları: >30 dk boşluk, belirsiz/kesik ve >120 dk oturum dışlamaları. | [İlk deney](../reports/model_exploration/results.json): F1 kural 0,488, LR 0,319, RF 0,489. Bu alt kümenin temsil gücü varsayılmadı. |
| 31 Ekim 2026 | Aynı araştırma 342 kullanıcıya genişletildi. 642 yıl-anomalili olay dışlandı; 444.012 aday pencereden 334.165 geçerli pencere ve 319.614 ardışık çift üretildi. Tüm kullanıcılar en az bir çiftle dahil edildi. | [Tam veri deneyi](../reports/model_exploration_all_342/results.json): F1/AP kural 0,525/0,383, LR 0,425/0,529, RF 0,536/0,547. Dört kullanıcıda yalnızca negatif hedef vardı; tanımsız kullanıcı metrikleri null tutuldu. |
| 1 Kasım 2026 | LR/RF karar eşikleri ayrı doğrulama kullanıcılarında 0,05–0,95 gridinden F1 ile seçildi. Üç dış kullanıcı katı ve iç %20 kullanıcı holdout'u; proxy eşikleri iç eğitimden öğrenildi. Seçim sonrası refit yok. | [Eşik deneyi](../reports/model_exploration_thresholds/results.json): LR F1 0,422→0,537 ve FP/gün 1,332→6,193; RF F1 0,532→0,544 ve FP/gün 7,458→5,202. AP/ROC-AUC aynı modelin yalnızca karar eşiğini değiştirmekle değişmedi. |
| 11 Kasım 2026 | İsmail Hakkı Sarıoğlan'ın v1 paketi ve üretim betiği doğrulandı. `src/data/mpu_pipeline.py` gönderilen kodla aynı SHA256'ya sahip. Uygunluk: ekran olayları, ≥7 geçerli-pencere eşdeğer günü, ≥7 aktif ekran günü ve günlük ortalama ≥1 oturum başlangıcı. | 342 kullanıcı / 443.781 pencere; 372.349 geçerli, 366.362 hedefli. Uygun 330 kullanıcıda 362.255 çift ve 41.968 pozitif. 12 kullanıcı / 4.107 hedefli satır dışarıda. Tüm hedef kaydırmaları doğrulandı; örnek doğrulamada 5 kullanıcının 6.464 satırı eşleşti, 70 kesme testinde özellik farkı yok. |
| 11 Kasım 2026 | RF/XGBoost ortak model API'si ve örnek ağırlığı desteği geliştirildi. Yedi v1 özelliği tüm modellerde aynı: screen_s, sessions_started, longest_run_s, app_switches, prev_session_s, hour, dow. Bildirim, kimlik, etiket, retrospektif temiz süre ve kişisel geçmiş alanları girdiye alınmadı. | `src/models/tree_models.py`, `dataset_v1.py`, `refinement_exploration.py`. Python 3.11.15 ve XGBoost 3.2.0; tüm ortam/ayarlar deney JSON'unda. Otomatik özellik seçimi veya ablation yapılmadı. |
| 11 Kasım 2026 | V1 deneyi: 3 dış kullanıcı katı; her katta 176 eğitim / 44 doğrulama / 110 test kullanıcısı. LR 2, RF 2, XGBoost 3 aday; toplam 21 fit. Konfigürasyon ve eşik yalnızca doğrulama F1'iyle seçildi; refit/early stopping yok. | [V1 sonuçları](../reports/model_refinement_dataset_v1/results.json): ortalama test F1/AP kural 0,450/0,278, LR 0,504/0,488, RF 0,524/0,541, XGBoost 0,527/0,542. FP/yerel gün sırasıyla 1,740/2,595/2,386/2,350. XGBoost'un küçük avantajı kesin üstünlük sayılmaz. |
| 11 Kasım 2026 | V1 seçimi: LR C=1 ve eşik 0,25; RF depth=12/leaf=10, eşikler 0,70/0,65/0,75; XGBoost depth/tree 5/150, 3/300, 5/150 ve eşikler 0,25/0,25/0,30. RF sınıf ağırlığı yalnızca eğitimde; XGBoost scale_pos_weight=1. | Aynı v1 deneyinde ilk/seçili F1: LR 0,438→0,504, RF 0,456→0,524, XGBoost 0,453→0,527. Ayar ve eşik etkisi birlikte değerlendirilir; eski veri deneyiyle doğrudan iyileşme karşılaştırması yapılmaz. |
| 11 Kasım 2026 | Doğrulama: 86 test; 1.449.020 test tahmininin kapsamı ve confusion/F1/AP/ROC-AUC kontrolü; 9 seçili modelin kaydet/yükle sonrası tüm test skor ve kararları aynı. | Toplu sonuç/görseller `reports/model_refinement_dataset_v1/`; satır çıktıları `data/processed/`, eğitilmiş modeller `models/` altında yerel tutulur. |
| 12 Kasım 2026 | Genel model iyileştirmesinin yöntem, ayar, test sonuçları ve sınırlılıkları raporlandı. | [Model Refinement and Test Submission](model_refinement_test_submission.pdf). Yeni deney veya kişiselleştirme sonucu içermez. |

### Kişiselleştirme öncesi açık kararlar

- V1 uzun oturum etiketi, sonraki pencereye temas eden oturumun tam süresini ve geçmişe dönük ≤3 saat kabulünü kullanır. Hedef penceresinin dışındaki bilgiye erişebilir; kronolojik ayrımdan önce sınırlı proxy veya etiket erişilebilirlik/sonuç bitiş zamanı belirlenmelidir.
- V1 sabit proxy eşiklerini korur; önceki araştırma eğitim yüzdeliklerini kullanmıştır. V1 saatleri `ts_raw/1000 + (timezone+1)*3600` ile üretilir; gerçek DST geçmişi doğrulanmadı. Yeni FP/gün paydası yerel tarih, eski deneylerde UTC'dir.
- `prev_session_s` pencere başlangıcında bitmiş oturumu kullanır; bilinmeyen geçmiş sıfır olabilir. Heartbeat geçerliliği kesintisiz gözlem garantisi değildir. Uygunluk tam kayıt üzerinden geçmişe dönük seçilir; canlı kapsam doğrulaması sayılmaz.
- `hist_days` ve `valid_days` 48-pencere eşdeğeridir; 1/3/7/14 farklı kayıt günüyle karıştırılmaz. Kişiselleştirme genel/personal-only/ağırlıklı modelleri aynı gelecek test döneminde karşılaştıracaktır.
- Kullanıcı geri bildirimi ayrı etikettir; proxy ile doğrudan birleştirilmez. SHAP, kalibrasyon, kişiselleştirme, nihai mobil format ve cihaz testleri tamamlanmadı.
