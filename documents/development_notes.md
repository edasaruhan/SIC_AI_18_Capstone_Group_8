# Geliştirme notları

Bu dosya [literatür/veri/teknoloji incelemesi](literature_data_technology_review.pdf) ve [uygulama planındaki](concept_note_implementation_plan.pdf) geliştirmeyi etkileyen kararları özetler. Aşağıdakiler planlanan yöntemdir; uygulanmış veya doğrulanmış sonuçlar değildir.

## Veri ve tahmin hedefi

- İlk kaynak Mobile Phone Use Dataset'tir: dağıtımda 342 katılımcı vardır; ilişkili yayındaki analiz 337 kullanıcıyı kapsar. Bu iki sayı karıştırılmaz.
- Olaylar ham zaman damgasına göre sıralanır; yerel saate dönüşüm açıkça yapılır. Yerel zaman alanı, özellikle deneyim örnekleme kayıtlarında, doğrudan olay sıralaması için kullanılmaz.
- Oturumlar kilit açmadan ekran kapanışına kadar yeniden oluşturulur. Katılımcılar arasındaki olay türü farklılıkları ortak olay şemasına eşlenir; kayıt boşlukları “kullanım yok” sayılmaz. Eksik ve gerçekçi olmayan oturumlar işaretlenir, uygulanacak dışlama/sınırlama kararları kaydedilir.
- Model birimi 30 dakikalık penceredir. Tamamlanan pencerenin özellikleri **sonraki pencerenin** sonucunu tahmin eder. Hiçbir özellik tahmin anından sonraki veriyi kullanamaz.
- Başlangıç özellikleri: ekranın aktif kaldığı süre, kilit açma sayısı, uygulama geçişleri, önceki oturum süresi, saat, haftanın günü, gece etkinliği ve kişinin geçmişinden sapma. Klavye ve launcher olayları uygulama geçişlerinden çıkarılır. Özellikler Python ve Android tarafında aynı şekilde hesaplanabilmelidir.
- Kişisel ortalamalar ve önceki geri bildirim özetleri yalnızca tahmin anından önceki bilgiye dayanır. Eksik anket puanları genel model özelliği olarak kullanılmaz.

## İki ayrı etiket kaynağı

| Kaynak | Anlamı | Uygulama kuralı |
| --- | --- | --- |
| Davranışsal proxy | Uzun oturum, tekrarlanan kilit açma ve gece etkinliği gibi göstergelerle tanımlanan yoğun kullanım | Eşikler eğitim verisinde seçilir; yaygınlık ve eşik duyarlılığı raporlanır. Doğrulanmış istenmeyen kullanım veya bağımlılık ölçüsü sayılmaz. |
| Kullanıcı geri bildirimi | Belirli bir yakın dönem kullanımının kişinin istediğinden uzun sürüp sürmediği | Yanıt ilgili zaman aralığı ve özellik kaydıyla ilişkilendirilir. Açık “hayır” yanıtı, atlanan sorudan ayrı tutulur. |

Etiketin kaynağı korunur; iki kaynağın uyumu incelenmeden eğitim için birleştirilmez. Hatırlatmayı kabul etme/kapatma ayrı kaydedilir; kapatmak “bu kullanım isteniyordu” etiketi üretmez. Bir yanıt, değerlendirdiği tahminin girdisi olamaz; daha eski yanıtlar sonraki tahminleri besleyebilir.

## Modeller ve değerlendirme

| Yaklaşım | Eğitim verisi |
| --- | --- |
| Genel model | Değerlendirilen kullanıcı hariç diğer kullanıcılar |
| Kişiselleştirilmiş model | Diğer kullanıcılar ve değerlendirilen kullanıcının önceki kayıtları; kişisel kayıtlara daha yüksek örnek ağırlığıyla yeniden eğitim |
| Yalnızca kişisel model | Sadece değerlendirilen kullanıcının önceki kayıtları |

- Sabit kural ve lojistik regresyon başlangıç yöntemleridir. Ana adaylar Random Forest ve XGBoost'tur; üstünlükleri varsayılmaz. Ağaç modelleri SHAP TreeExplainer ile incelenir; açıklamalar nedensellik iddiası taşımaz.
- Genel model için katılımcıya göre gruplanmış çapraz doğrulama kullanılır. Eğitim ve test katılımcıları çakışmaz.
- Kişiselleştirme koşulları, kayıt bulunan ilk **N = 1, 3, 7, 14 gün** geçmiş ve sonraki yedi gün testtir. Sınırda bir pencere boşluk bırakılır; özellik ve sonuç aralıkları ayrımlar arasında örtüşemez.
- N + 7 kayıt gününden az verisi olan katılımcı o koşuldan çıkarılır; her koşulun katılımcı ve etiket sayıları raporlanır.
- Kişisel örnek ağırlığı yalnızca doğrulama katılımcılarında ayarlanır. Sınıf ağırlığı veya yeniden örnekleme yalnızca eğitim verisine uygulanır.
- Precision, recall, F1, PR-AUC ve günlük yanlış uyarı sayısı kullanıcı bazında ve ortalamalarla raporlanır. Planın öngördüğü yaş grubu kırılımları da veri elverdiği ölçüde sunulur.
- Sabit rastgelelik tohumları ve sürümlenen veri bölmeleriyle deneyler tekrar üretilebilir tutulur. Model kuralı geçemiyorsa sonuç açıkça raporlanır; plandaki alternatif kişisel yüzdelik eşiklerdir.

Yaklaşık bir haftalık yetişkin pilotu veri toplama, uygulama kullanılabilirliği ve proxy/geri bildirim uyumunu sınar. 14 günlük kişiselleştirme eğrileri açık veri üzerinde hesaplanır. Seyrek geri bildirim durumunda kişiselleştirme başarısı iddiası yerine uygulanabilirlik ve etiket sayıları raporlanır.

## Android ve entegrasyon

Planlanan araçlar Kotlin, Android Studio/SDK, UsageStatsManager, Room, WorkManager, bildirimler ve Jetpack Compose'dur. Ekran/kilit olayları için minimum sürüm Android 9 / API 28 olarak planlanmıştır. Gerçek olay erişimi, arka plan güvenilirliği ve pil etkisi farklı sürümlerde en az iki telefonda doğrulanacaktır.

Genel modelin cihazda çalıştırılması hedeflenir. ONNX Runtime Mobile adaydır; dışa aktarılmış karar kuralları alternatifidir. Seçim boyut, uyumluluk ve gecikme testinden sonra yapılır. Örnek ağırlıklı kişiselleştirme önce çevrimdışı değerlendirilir; cihazda uyarlama ayrıca gecikme, bellek ve pil açısından sınanır.

Python ve Android özellik çıkarımı ortak sentetik örneklerle karşılaştırılır. Hatırlatmalarda günlük üst sınır ve sessiz saatler bulunur; kullanıcı kapatabilir, duraklatabilir veya tamamen devre dışı bırakabilir.

## İlk ortak veri biçiminde netleştirilecekler

Yiğit koordinasyonunda İsmail, Kaan, Hamza ve Murat kendi bileşenleri açısından aşağıdakileri netleştirir. Alan adları ve değerleri henüz kesinleşmemiştir; kararlar bu bölüme yazılacaktır.

| Konu | Netleştirilecek karar |
| --- | --- |
| Olay ve zaman | Ortak olay türleri, zaman damgası birimi, saat dilimi ve pencere sınırları |
| Özellik tablosu | Özellik adları, sırası, veri tipleri, birimleri ve eksik/geçersiz pencere gösterimi |
| Hedef | Proxy göstergeleri, eşikleri, gece aralığı ve etiket tanımının sürümü |
| Tahmin | Modelin girdi/çıktı biçimi, model sürümü ve karar eşiği |
| Geri bildirim | İlgili dönemle eşleme, etiket kaynağı, “hayır”/atlandı ayrımı ve ayrı hatırlatma tepkisi |

LSTM, ayrıntılı kaydırma davranışı ve ekran görüntüsü analizi ilk faz kapsamında değildir. Backend ve kesin mobil model formatı başlangıç gereksinimi olarak kabul edilmez.

## Veri toplama sınırları

Pilot yalnızca bilgilendirilmiş onam veren yetişkinleri kapsar. Kullanım metaverisiyle sınırlı kalınır; ekran görüntüsü, mesaj içeriği, rehber ve hassas konum toplanmaz. Ham kayıtlar mümkün olduğunca cihazda tutulur; araştırma dışa aktarımı ayrıca isteğe bağlıdır. Takma kimlik kullanımı tam anonimlik olarak sunulmaz.

Toplama öncesinde saklama süresi, silme/çekilme işlemleri ve gerekli kurumsal koşullar belirlenir. Açık veri setinin yeniden kullanım koşulları doğrulanmadan veri yeniden dağıtılmaz. 2016 tarihli, ağırlıkla İspanya kaynaklı kayıtların günümüz Türkiye kullanıcılarına aktarımı doğrulanmış sayılmaz.
