# Model geliştirme ve kişiselleştirme planı

**Ana sorumlu:** Kaan Özsoy. Bu dosya model çalışmasının oturumlar arası ana takip belgesidir.

## Mevcut çalışma durumu

| Alan | Güncel kayıt |
| --- | --- |
| Current Stage | Aşama 1 — Problem ve veri sözleşmesi (`Not Started`) |
| Current Task | Çalışma planı hazır; ilk uygulama adımı için kullanıcı onayı bekleniyor. |
| Last Completed Task | Kaynak belgeler ve repo durumu incelendi; kalıcı çalışma kuralları ve sekiz aşamalı takip planı oluşturuldu, belge/kapsam kontrolleri geçti. Model geliştirme görevi henüz tamamlanmadı. |
| Next Action | Kullanıcı onayından sonra Aşama 1'deki tahmin hedefini ve pencere/zaman ilişkisini açıklayan ilk sözleşme taslağını hazırlamak; kesin bilgiler ile açık kararları ayırmak. |
| Current Blockers | İlk uygulama adımı kullanıcı onayı bekliyor. Bu repoda ortak özellik tablosu/şeması, doğrulanmış proxy tanımı, Hamza'nın veri bölmeleri ve Yiğit'in entegrasyon arayüzü henüz bulunmuyor; bunlar gerçek veri deneyleri ve entegrasyon için bağımlılıktır. Sözleşme taslağı ve sonraki sentetik kontroller bu çıktıların tümünü beklemek zorunda değildir. |

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

Bu ilk oturumun kapsamı yalnızca `AGENTS.md` ve bu plandır. Model kodlamasına veya ilk sözleşme uygulamasına kullanıcı onayıyla geçilecektir.

## Aşama 1 — Problem ve veri sözleşmesi

**Durum:** `Not Started`

**Amaç:** Tahmin problemini, modelin tükettiği tabloyu ve ürettiği çıktıyı ortak ve sınanabilir biçimde tanımlamak.

**Yapılacak işler**

- [ ] Tamamlanan pencere → sonraki pencere hedefini; özellik, tahmin ve sonuç zamanlarını örnek zaman çizelgesiyle doğrula. Pencere sınırı, saat dilimi ve kayıt günü tanımındaki açık kararları ayır.
- [ ] İsmail'in üreteceği özelliklerin ad, sıra, veri tipi, birim, eksiklik ve geçersiz pencere kurallarını taslaklaştır; sadece Python/Android'de ortak hesaplanabilir özellikleri esas al.
- [ ] Kaan–İsmail işbirliğiyle proxy göstergelerini, eğitimde seçilecek eşikleri, gece aralığını ve etiket sürümünü netleştir; proxy ile gerçek istenmeyen kullanım arasındaki sınırlılığı kaydet.
- [ ] Tahmin olasılığı/skoru, karar etiketi, karar eşiği ve model sürümü için Yiğit'le girdi/çıktı sözleşmesini netleştir; sabit kuralın çıktı anlamını ayrıca belirt.
- [ ] Hamza'nın bölmeleri için kullanıcı kimliği, özellik/sonuç aralıkları ve geçmiş uygunluğu metaverisini tanımla; kimlik ve hedef sütunlarının yanlışlıkla özellik yapılmasını önle.
- [ ] Aynı pencere etiketi, gelecekten kişisel ortalama/geri bildirim, ön işlemenin tüm veriye uygulanması ve sınırdaki örtüşmeler gibi sızıntı risklerini kaydet.
- [ ] İsmail, Hamza ve Yiğit'ten beklenen çıktıları ve kesinleşmemiş kararları bu plana işle; sentetik sözleşme örnekleriyle bağımsız ilerlenebilecek kısmı belirle.

**Bağımlılıklar:** İsmail'in örnek tablosu ve veri kalitesi bilgisi; Hamza'nın bölme gereksinimleri; Yiğit'in ortak format koordinasyonu; Murat'ın özelliklerin cihazda üretilebilirliğine dair girdisi. Taslak hazırlanabilir; ekip çıktıları olmadan nihai şema onaylanmış sayılmaz.

**İlgili dosyalar:** [Geliştirme notları](development_notes.md), [veri notu](../data/README.md), bu plan; ileride ortak kararlar için ilgili notlar ve `tests/fixtures/` altında sentetik örnekler. İsmail'in `src/data/`, Hamza'nın `src/evaluation/` ve Yiğit'in `src/integration/` alanları bağımlılıktır; değişiklikleri ayrıca kapsamlandırılır.

**Tamamlanma kriterleri:** Zaman ilişkisi, özellik/çıktı şeması, proxy tanımı ve sürümü, eksik/geçersiz kayıt kuralları ve bölme metaverisi belgelenmiş; ilgili ekip girdileriyle doğrulanmış; uygulamayı etkileyen açık kararlar çözümlenmiş olmalı. Yalnızca taslak üretmek aşamayı tamamlamaz.

**Doğrulama/test yöntemi:** Küçük sentetik pencere örnekleriyle özellik/sonuç ayrımı, sınır ve eksiklik senaryolarını kontrol et; örnek tablo varsa şemaya karşı incele. Her kararı kaynak belgeye veya kaydedilmiş ekip/kullanıcı kararına dayandır. Bu aşama eğitim veya performans deneyi gerektirmez.

## Aşama 2 — Baseline modeller

**Durum:** `Not Started`

**Amaç:** Ana modellerin karşılaştırılacağı sabit kural ve Logistic Regression yöntemlerini çalıştırılabilir, ortak arayüzlü bir başlangıca dönüştürmek.

**Yapılacak işler**

- [ ] İlk çalışan Python ortamını ve kullanılan bağımlılık sürümlerini doğrula; ortamla ilgili gereken değişikliği ayrıca kapsamlandır.
- [ ] Aşama 1 sözleşmesine göre eğitim ve tahmin arayüzlerini kur; hedef/kimlik sütunları, özellik sırası ve uyumsuz girdiler için açık kontroller ekle.
- [ ] Geçmiş/girdi bilgisinden gelecek pencere kararı veren sabit kuralı tanımla; gelecekteki gerçek hedefi kullanan bir “baseline” kurma. Kural ile proxy hedefinin farkını açıkla.
- [ ] Logistic Regression için gereken eksik değer işleme ve ölçeklemeyi eğitimde öğrenen pipeline'ı kur; dönüşümleri doğrulama/test verisinde yeniden öğrenme.
- [ ] Eğitim ve tahmini küçük sentetik tabloyla çalıştır; model bazında olasılık/skor ve karar çıktısını doğrula.
- [ ] Özellik sırası, eksik/yanlış tip, sınır değerleri ve tek sınıflı eğitim durumu için anlamlı testler ekle; elde edilen sonucu bu plana kaydet.

**Bağımlılıklar:** Kullanılabilir sözleşme taslağı; gerçek veri deneyi için İsmail'in uygun özellik/etiket tablosu ve Hamza'nın bölmeleri. Kesinleşmeyen sözleşmeyle sentetik çalışma yapılırsa geçici kararlar işaretlenir; nihai entegrasyon gibi sunulmaz.

**İlgili dosyalar:** `src/models/`, model testleri için `tests/`, sentetik girdiler için `tests/fixtures/`, keşif gerekiyorsa `notebooks/`; [requirements.txt](../requirements.txt) ortam doğrulamasının kaynağıdır.

**Tamamlanma kriterleri:** İki baseline ortak arayüzden eğitilip tahmin üretebilmeli; ön işleme yalnızca eğitimde öğrenilmeli; uygun sentetik testler geçmeli; çalıştırma komutları ve sonuçları kaydedilmiş olmalı. Gerçek veri başarısı bu aşamayı tamamlamak için uydurulmaz.

**Doğrulama/test yöntemi:** Deterministik küçük sentetik örnekler, ön işlemenin eğitim verisine bağlılığı, tahmin boyutu/değer aralığı, karar eşiği ve beklenen hata durumları. Gerçek veriyle çalıştırma varsa ayrı deney kaydı tutulur.

## Aşama 3 — Ana makine öğrenmesi modelleri

**Durum:** `Not Started`

**Amaç:** Random Forest ve XGBoost'u baseline'larla aynı veri ve arayüz düzeninde tekrar üretilebilir biçimde eğitmek.

**Yapılacak işler**

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

**Durum:** `Not Started`

**Amaç:** Hamza'nın değerlendirme düzeninde bütün modelleri aynı koşullarla karşılaştırmak; genelleme ve sızıntı risklerini görünür kılmak.

**Yapılacak işler**

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

**Durum:** `Not Started`

**Amaç:** Model Exploration ve final rapor/sunumunun model bölümünü yeniden izlenebilir deney kanıtlarıyla hazırlamak. Raporlama önceki aşamalarla paralel yürütülür; bütün deneylerin bitmesini beklemez.

**Yapılacak işler**

- [ ] Model Exploration ödevinin değerlendirme yönergesi varsa incele; mevcut repoda ayrıntılı yönerge yok, teslim formatı/ek ölçütler uydurma.
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
