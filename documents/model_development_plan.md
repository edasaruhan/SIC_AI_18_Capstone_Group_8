# Model geliştirme ve kişiselleştirme planı

**Ana sorumlu:** Kaan Özsoy. Bu dosya model çalışmasının oturumlar arası ana takip belgesidir.

## Mevcut çalışma durumu

| Alan | Güncel kayıt |
| --- | --- |
| Current Stage | Aşama 3–4 — İlk gerçek veri araştırması (`In Progress`); Aşama 2 tamamlandı. Aşama 1 ve 8 devam ediyor; nihai protokol tamamlanmış değil. |
| Current Task | Dataset incelemesi ve 12 katılımcıda ilk kural/LR/RF karşılaştırması tamamlandı; sonuçların ve araştırma varsayımlarının değerlendirilmesi. |
| Last Completed Task | [Salt-okunur audit](../src/models/dataset_audit.py) ile 342 CSV'nin ilgili alanları tarandı. [Araştırma adaptörü](../src/models/research_data.py) ve [deney](../src/models/real_data_exploration.py) 12.830 pencere çiftiyle çalıştı; [gerçek veri sonuçları](../reports/model_exploration/results.json), grafikler ve 51 başarılı test mevcut. |
| Next Action | İlk sonuçları kullanıcıyla değerlendir; proxy yüzdeliği ve oturum/kayıt boşluğu politikalarının duyarlılığını kontrol ederek Model Exploration'ın dört başlığına aktar. Nihai model seçimini mevcut küçük alt kümeye göre kesinleştirme. |
| Current Blockers | Ortak ekip şeması, nihai proxy tanımı, yerel saat/gece eşlemesi, Hamza'nın nihai bölmeleri ve Yiğit'in arayüzü açık. Kullanıcı İsmail'den çıktı olmadığını bildirdi ve mevcut ham veriden ilerlemeyi yetkilendirdi; bu nedenle sınırlı araştırma adaptörü kullanıldı. Bu çıktı nihai veri hattı veya 342 kullanıcının tamamında performans doğrulaması değildir. |

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

Repo incelemesinde çalışma alanlarında yalnızca README ve yer tutucular bulundu; veri, model, kod, test veya deney çıktısı yoktu. Ekip üyelerinin repo dışındaki çalışmalarının durumu bilinmiyor. Başlangıç bağımlılıklarının sürümleri ve çalışan ortam henüz doğrulanmış değil.

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

**Durum:** `In Progress`

**Amaç:** Random Forest ve XGBoost'u baseline'larla aynı veri ve arayüz düzeninde tekrar üretilebilir biçimde eğitmek.

**Yapılacak işler**

- [x] Model Exploration için Random Forest'ı mevcut iki baseline ile aynı gerçek veri araştırmasında çalıştır; ayarları ve kanıtları kaydet. Kanıt: [deney kodu](../src/models/real_data_exploration.py) ve [sonuçlar](../reports/model_exploration/results.json). XGBoost ve ana modellerin nihai ortak API'si bu işlemde tamamlanmadı.
- [ ] Random Forest ve XGBoost'u ortak eğitim/tahmin arayüzüne bağla.
- [ ] Başlangıç hiperparametrelerini, gerekçelerini ve rastgelelik tohumunu kaydet; sayısal değerler henüz seçilmiş değildir.
- [ ] Aynı veri/bölme/etiket sürümünün kullanımını ve modelin beklediği özellik sırasını kaydet.
- [ ] Sample weight aktarımını iki model için doğrula; kişiselleştirme deneyinin ağırlık seçimini Aşama 5'e bırak.
- [ ] Gerekli sınıf ağırlığı/yeniden örnekleme seçeneklerini yalnızca eğitimde uygula; tuning yapılırsa doğrulama verisiyle sınırla ve test kümesini ayır.
- [ ] Eğitim ayarları, sürümler ve çıktılarla tekrar üretilebilir bir çalışma kaydı oluştur; sınırsız hiperparametre araması ekleme.

**Bağımlılıklar:** Aşama 2 arayüzü ve ortamı; gerçek veri eğitimi için İsmail'in tablo/etiket sürümü ve Hamza'nın doğrulanmış bölmeleri.

**İlgili dosyalar:** `src/models/`, `tests/`, yerel model çıktıları için `models/`, paylaşılabilir deney kayıtları için `reports/`.

**Tamamlanma kriterleri:** İki model ortak arayüzde eğitim/tahmin ve örnek ağırlığı desteğiyle çalışmalı; ayarlar ve sürümler kayıtlı, uygun testler başarılı olmalı. Gerçek veri eğitiminin yürütülüp yürütülmediği açıkça belirtilmeli; henüz yürütülmediyse performans iddiası olmamalı.

**Doğrulama/test yöntemi:** Sabit tohumla sentetik çalışma, çıktı biçimi ve olasılık aralığı kontrolleri, örnek ağırlığının eğitim arayüzüne doğru aktarılması. Gerçek veri çalışmasında veri/bölme/ayar kaydıyla yeniden çalıştırılabilirlik kontrolü.

## Aşama 4 — Model değerlendirme ve karşılaştırma

**Durum:** `In Progress`

**Amaç:** Hamza'nın değerlendirme düzeninde bütün modelleri aynı koşullarla karşılaştırmak; genelleme ve sızıntı risklerini görünür kılmak.

**Yapılacak işler**

- [x] Kullanıcı onayıyla, Hamza'nın nihai düzeni yerine geçmeyen ilk araştırma karşılaştırmasını üret: 12 kullanıcı, 3 katlı GroupKFold, eğitim-katı proxy eşikleri, üç model, metrikler ve OOF görselleri. Kanıtlar `reports/model_exploration/` altında; kullanıcı bazlı ve satır düzeyindeki çıktılar yerel `data/processed/model_exploration/` altında.
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
- [ ] Model Exploration için problem, yöntem, baseline ve mevcut ilk deney çıktılarını hazırla; yalnızca sentetik çalışma varsa bunu açıkça belirt, gerçek veri sonuçlarının eksikliğini kaydet.
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
