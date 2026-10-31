# Model veri sözleşmesi — zaman ve aday özellik taslağı

**Durum:** Taslak; ortak veri şeması veya ekip tarafından onaylanmış entegrasyon sözleşmesi değildir.

Bu belge [model planının](model_development_plan.md) Aşama 1'deki zaman ilişkisi ve aday özellik şeması işlerinin çıktısıdır. [Geliştirme notlarındaki](development_notes.md) tamamlanan pencere → sonraki pencere kuralını zaman örnekleriyle açıklar. Bölüm 7'deki alan adı, sıra, tip ve birimler model tarafı önerileridir; gerçek tablo veya Android ile henüz doğrulanmadı. Proxy eşikleri ve mobil çıktı formatı açık kalır.

## 1. Kaynak belgelerde kesin olan hedef

- Her model örneği bir kullanıcıya ve tamamlanmış 30 dakikalık bir özellik penceresine aittir.
- Bu pencerenin özellikleri, hemen sonraki 30 dakikalık pencerenin **yoğun kullanım proxy** sonucunu tahmin eder. Aynı pencerenin ölçümlerinden aynı pencerenin etiketini yeniden üretmek amaç değildir.
- Proxy; uzun oturum, tekrarlanan kilit açma ve gece etkinliği gibi davranış göstergelerine dayanacaktır. Eşik ve birleştirme kuralı henüz kararlaştırılmamıştır; eğitim verisinde seçilip duyarlılığı incelenecektir.
- Proxy etiketi doğrulanmış istenmeyen kullanım veya bağımlılık ölçümü değildir. Kullanıcının belirli bir dönem hakkındaki geri bildirimi ayrı etiket kaynağıdır; bu iki kaynak kendiliğinden birleştirilmez.
- Tahminin kendi sonucunu değerlendiren yanıt girdiye eklenmez. Eski geri bildirim ve kişisel özetler ancak tahmin anında bilinebilen geçmişten üretilebilir.

## 2. Zaman ilişkisi

`t`, tamamlanan özellik penceresinin bitişidir. Aşağıdaki aralık gösterimi **taslak öneridir**: `[başlangıç, bitiş)` başlangıcı içerir, bitişi içermez. Böylece tam sınırdaki olay iki pencereye birden yazılmaz. Ekip farklı bir sınır kuralı seçerse örnekler ve sonraki uygulama birlikte güncellenmelidir.

```text
       özellik penceresi                 sonuç / hedef penceresi
      [t − 30 dakika, t)                 [t, t + 30 dakika)
------------------------------|---------------------------------->
                              t
                     nominal tahmin sınırı
```

`X(t)`, tamamlanan pencerenin özellikleri ve tahmin anından önceki izinli geçmiş bilgisidir. `y_proxy(t)`, sonraki pencere için hesaplanacak etiketi temsil eder. Bu semboller CSV sütun adı veya yazılım arayüzü değildir.

Çevrimdışı eğitimde gelecekteki pencere görüldükten sonra etiket hesaplanabilir; bu pencerenin olayları `X(t)` içine giremez. Canlı kullanımda hedef etiket tahmin yapılırken henüz bilinmez. `t` nominal sınırdır; Android işinin fiilen ne zaman çalışacağı ve gecikmiş veriyle ne yapılacağı ayrıca doğrulanmalıdır.

## 3. Sentetik zaman çizelgesi

Aşağıdaki saatler aynı gün ve tek bir sabit saat referansında yalnızca açıklama içindir. UTC, Türkiye saati, ham zaman damgası birimi veya gerçek veri seti örneği olarak kabul edilmez.

| Örnek | Özellik başlangıcı | Özellik sonu / nominal tahmin | Hedef başlangıcı | Hedef sonu |
| --- | --- | --- | --- | --- |
| W0 | 10:00 | 10:30 | 10:30 | 11:00 |
| W1 | 10:30 | 11:00 | 11:00 | 11:30 |
| W2 | 11:00 | 11:30 | 11:30 | 12:00 |

W0 için 10:00 olayı özellik penceresindedir; 10:29:59 da bu pencerededir. Tam 10:30 olayı özellik penceresine girmez; hedef penceresinin başlangıcındadır. Tam 11:00 olayı W0'ın hedef penceresinde değildir. Oturumların pencere sınırında nasıl bölüştürüleceği bu örnekle kesinleşmez; İsmail ve Murat ile ortak özellik hesaplama kararı gerekir.

W0'ın 10:30–11:00 arasındaki kullanım süresini veya o döneme ilişkin sonradan gelen yanıtı W0 girdisine eklemek geleceği kullanır. Özellik penceresinden tahmin edilen proxy sonucu ile gerçekleşen kullanıcı değerlendirmesi ayrıca ilişkilendirilmeli; yanıt zamanı, değerlendirdiği zaman aralığı yerine konmamalıdır.

## 4. Bölme sınırı ve veri kapsamı

Kaynak protokol kişiselleştirmede geçmiş ile test arasında bir pencere boşluk bırakılmasını ve özellik/sonuç aralıklarının örtüşmemesini ister. Bu örnekte **W0 eğitimdeyse W1'i doğrudan teste koymak uygun değildir**: W0'ın hedefi ile W1'in özellik aralığı aynıdır. W1 örneğini sınırda dışarıda bırakıp W2'yi testte başlatmak bu yerel örtüşmeyi kaldırır.

Bu gösterim Hamza'nın kullanıcı/zaman temelli bölme kodunun yerine geçmez. Grup ayrımı, N günlük kişisel geçmiş ve sonraki yedi günlük testin gerçek kayıt kapsamı ayrıca doğrulanmalıdır. Pencere atlamak tek başına genel modelde kullanıcı sızıntısını önlemez.

Kayıt boşluğu “telefon kullanılmadı” anlamına gelmez. Özellik veya hedef penceresinin kaydı yetersizse sıfır özellik/negatif etiket üretilmesi varsayılmaz. Geçerli pencere kapsamı, eksik oturum ve aradaki kayıpsız ardışıklık koşulları İsmail ve Hamza ile netleştirilene kadar gerçek tablo üzerinde tam sözleşme doğrulaması yapılamaz.

## 5. Açık kararlar ve bağımlılıklar

| Karar | Şu anki bilgi / gereken çıktı | Bağlantılı sorumlular |
| --- | --- | --- |
| Pencere sınırı ve hizalama | 30 dakika sabit. Yarı açık aralık taslak öneri; hangi zaman referansına göre hizalanacağı ve sınır olaylarının uygulaması henüz onaylanmadı. | İsmail, Hamza, Yiğit; cihaz tarafında Murat |
| Zaman temsili | Ham zaman damgasıyla sıralama ve açık yerel saat dönüşümü gerekli. Birim, saat dilimi, yerel saat değişimleri ve ortak saklama biçimi henüz belirlenmedi. | İsmail, Yiğit, Murat |
| Kayıt günü | Deneyler kayıt bulunan günlere dayanır. Günün saat dilimi, kısmi gün ve yeterli kayıt kapsamı tanımı henüz belirlenmedi. | İsmail, Hamza |
| Veri erişilebilirliği | Geçmiş bilgi şartı kesin. Geç gelen olay/geri bildirim ve gerçek Android iş gecikmesinde pencerenin işlenme politikası açık. | İsmail, Murat, Yiğit; değerlendirmede Hamza |
| Özellik ve eksiklik kuralları | Bölüm 7'de ad/sıra/tip/birim ve eksik/geçersiz değer taslağı var. Kesinleştirmek için örnek tablo, oturum bölüştürme ve cihazda üretilebilirlik doğrulaması gerekli. | İsmail, Kaan, Yiğit; cihazda üretilebilirlik için Murat |
| Proxy tanımı | Sonraki pencere hedeflenir; göstergelerin eşikleri, gece aralığı, birleştirme kuralı ve sürümü açık. | Kaan ve İsmail; protokol kontrolü için Hamza |
| Tahmin ve değerlendirme arayüzü | Olasılık/skor/karar, kimlik ve zaman metaverisinin kesin alanları henüz tanımlanmadı. | Kaan, Yiğit, Hamza |

Bu bağımlılıklar ekipten yanıt alınmış gibi sunulmaz. Bölüm 7, Kaan'ın model girdisi önerisidir; diğer sorumluların kodu veya ortak şema bu belgeyle değiştirilmez.

## 6. Bu taslağın doğrulaması

Sentetik çizelgede her özellik ve hedef aralığının 30 dakika olduğu, hedefin özellik bitişinde başladığı, sınır olaylarının yarı açık aralıklarla tek pencereye ait olduğu ve W0/W1 ile W0/W2 bölme örneklerinin örtüşme davranışı kontrol edilir. Belgedeki yerel bağlantılar ve kaynak notlarla yöntem uyumu ayrıca incelenir.

Bu kontroller zaman taslağını doğrular; veri setinin kalitesini, proxy geçerliliğini, Android davranışını veya model performansını doğrulamaz. Nihai ortak veri sözleşmesi ancak ilgili ekip girdileri ve gerçek örneklerle tamamlanabilir.

## 7. Aday özellik şeması

Bu liste kaynak notlardaki sekiz özellik grubunun dar bir taslağıdır. Her adayın açık veri ve Android'de aynı anlamla üretilebildiği doğrulanmalıdır; üretilemeyen aday aktarılabilir modelden çıkarılır. Önerilen sıranın kesinleşmesi yeni bir şema kararı gerektirir. Tüm alanların her modelde zorunlu kullanılacağı veya cold-start eksikliklerinin çözüldüğü varsayılmaz.

`integer`, kesirsiz sayıyı; `float`, sonlu gerçek sayıyı anlatır. Bunlar mantıksal tiplerdir; pandas/NumPy/Kotlin saklama tipleri veya dosya formatı henüz seçilmedi. Sayısal alanlara metin ya da boolean sessizce dönüştürülmez. Eksiklik gösterimi aşağıda ayrıca tanımlanır.

| Sıra | Önerilen alan adı | Mantıksal tip | Birim | Geçerli değer | Önerilen anlam |
| --- | --- | --- | --- | --- | --- |
| 1 | `screen_active_seconds` | float | saniye | 0–1800 | Tamamlanan özellik penceresinde ekranın aktif kaldığı toplam süre; çakışan aralıklar iki kez sayılmaz. |
| 2 | `unlock_count` | integer | adet | 0 veya pozitif | Aynı özellik penceresindeki geçerli kilit açma olayı sayısı. |
| 3 | `app_switch_count` | integer | adet | 0 veya pozitif | Aynı pencerede geçerli foreground uygulama değişimi sayısı; klavye ve launcher hariç. |
| 4 | `previous_session_duration_seconds` | float | saniye | 0 veya pozitif | Tahmin sınırı `t` öncesinde tamamlanmış son geçerli oturumun tam süresi; devam eden oturumun gelecekteki bitişi kullanılamaz. |
| 5 | `hour_of_day` | integer | saat | 0–23 | Öneri: nominal tahmin sınırı `t` için yerel saat. |
| 6 | `day_of_week` | integer | gün kodu | 0–6 | Öneri: aynı `t` için yerel gün; Pazartesi=0, Pazar=6. |
| 7 | `night_activity_seconds` | float | saniye | 0–1800 | Öneri: özellik penceresindeki ekran aktif süresinin tanımlanacak gece aralığına düşen kısmı; ekran aktif toplamını aşamaz. |
| 8 | `screen_active_deviation_seconds` | float | saniye farkı | -1800–1800 | Öneri: mevcut `screen_active_seconds` eksi aynı kullanıcının önceki geçerli 30 dakikalık pencerelerindeki ekran aktif sürelerinin ortalaması. |

**Hesaplama kararları henüz onaylanmadı:** Saniye birimi, alan sırası, saat/günün `t` üzerinden alınması ve gün kodlaması öneridir. Uygulama değişiminde pencere öncesindeki son foreground durumun nasıl taşınacağı; son oturumun seçimi ve kayıt boşluklarının bu seçime etkisi; gece saatleri ve yerel saat değişimleri ayrıca netleştirilmelidir. `previous_session_duration_seconds` tam oturum süresidir; 30 dakikaya sığmak zorunda değildir. Geçersiz uzun oturumlara uygulanacak kalite kararı, model tarafında sessiz bir 1800 saniye kırpmasıyla değiştirilmez.

Kişisel sapma için **yalnızca geçmiş** pencereler kullanılması önerilir: bu taslağın yarı açık gösteriminde özellik penceresi başlangıcında veya öncesinde bitmiş pencereler. Mevcut pencere, başka kullanıcılar veya gelecek test pencereleri kişisel ortalamaya katılmaz. Geçmişin kaç güne/pencereye bakacağı, yeterlilik ölçütü ve gün/saat koşullaması henüz seçilmedi. Her geçerli pencere süresi 0–1800 olduğundan bu fark -1800–1800 aralığındadır; hangi geçmişin kullanılacağı yine açık karardır. Farkın ekran süresi üzerinden tanımı bir öneridir; kaynak belgeler tüm kişisel sapma formüllerini kesinleştirmez.

Bu tablo yalnızca `X(t)` adaylarıdır. Kullanıcı kimliği, zaman aralıkları, kayıt kapsamı, etiket kaynağı, proxy etiketi ve mevcut tahmini değerlendiren geri bildirim model özelliği değildir. Bunların kesin metaveri alanları sonraki sözleşme işinde ele alınacaktır. Önceki geri bildirimden ek özellikler bu ilk listeye eklenmez; ortak veri setinde üretilebilirlik ve ayrı deney kararı gerekir.

### Eksik ve geçersiz değerler

- Belge ve sentetik örneklerde bilinmeyen değer `null` ile gösterilir; `-1`, boş metin veya sıfır eksiklik işareti değildir. Saklama biçimindeki karşılığı henüz seçilmedi.
- Kayıt kapsamı doğrulanmış pencerede gerçekten olay yoksa sayım/süre sıfır olabilir. Sensör yokluğu, kayıt boşluğu veya bilinmeyen oturum bitişi sıfır kullanım diye doldurulmaz.
- Yeni kullanıcıda geçmiş yoksa kişisel sapma `null` kalır. Önceki geçerli oturum bilinmiyorsa oturum süresi `null` kalır. Bunlar yeni kullanıcının tüm örneklerini otomatik silmek için gerekçe değildir; Aşama 2'de geçmiş yetersizliğini destekleyen ön işleme kararı gerekir.
- Gece aralığı/saat dilimi belirlenmemişse gece etkinliği hesaplanmış kabul edilmez. Gece dışında geçerli bir penceredeki sıfır ile tanımı bilinmeyen `null` ayrıdır.
- Negatif sayım/süre, kesirli sayım, aralık dışı saat/gün, sonsuz değer ve ekran aktif toplamını aşan gece süresi geçersizdir. `NaN` sonlu bir ölçüm değildir; kullanılacak tablo biçiminde eksikliği temsil ederse `null` ile aynı eksiklik anlamına gelmelidir.
- Geçersiz değerler sessizce kırpılmaz, yuvarlanmaz veya sıfıra çevrilmez. Alan/satır gerekçesiyle işaretlenir; düzeltme/dışlama ve pencere geçerliliği İsmail/Hamza girdileriyle kesinleşir. Aşama 2'de seçilecek imputasyon/ölçekleme yalnızca eğitim verisinde öğrenilir; kullanıcılar arasında geleceğe bakan doldurma yapılmaz.
- Gerekli alanın tabloda hiç bulunmaması, mevcut alandaki `null` ile aynı durum değildir. Modelin seçilmiş özellik listesine göre ad/sıra uyumu açıkça doğrulanmalıdır; sütun sırası tesadüfen doğru kabul edilmez.

### Sentetik şema örnekleri

Aşağıdaki iki satır gerçek kullanıcı verisi değildir ve henüz bir eğitim dosyası değildir. Sıra yukarıdaki taslakla aynıdır. İlk örnekte geçmiş ortalama **480 saniye** kabul edilmiştir: `600 − 480 = 120`. İkinci örnekte kayıt kapsamı yeterli fakat kullanım ve bilinen kişisel geçmiş yoktur. İki örnekte de saat/gün ve gece aralığının örnek için bilindiği, pencerenin gece dışında olduğu varsayılır; gerçek saat dilimi/gece politikası belirlenmez.

```json
[
  {
    "screen_active_seconds": 600.0,
    "unlock_count": 4,
    "app_switch_count": 7,
    "previous_session_duration_seconds": 300.0,
    "hour_of_day": 10,
    "day_of_week": 4,
    "night_activity_seconds": 0.0,
    "screen_active_deviation_seconds": 120.0
  },
  {
    "screen_active_seconds": 0.0,
    "unlock_count": 0,
    "app_switch_count": 0,
    "previous_session_duration_seconds": null,
    "hour_of_day": 10,
    "day_of_week": 4,
    "night_activity_seconds": 0.0,
    "screen_active_deviation_seconds": null
  }
]
```

Taslak kontrolünde alan sırası/tip/aralık, iki satırın gece süresi ilişkisi, fark hesabı ve cold-start `null` ayrımı doğrulanır. Ayrıca 1801 saniye ekran süresi, negatif/kesirli sayım, saat=24, gün=7 ve ekran süresini aşan gece etkinliği reddedilmesi gereken örneklerdir. Önceki oturumun 3600 saniye olması ise tek başına bu şemayı ihlal etmez; veri kalitesi kararı ayrıdır. Bu kontroller veri çıkarımının ya da Android eşliğinin doğrulandığı anlamına gelmez.
