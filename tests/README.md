# Testler

Henüz test kodu yoktur. Kod geliştikçe aşağıdaki kontroller burada yer alacaktır:

- Olay sıralama, oturum çıkarımı, eksik kayıtlar ve pencere sınırları.
- Özelliklerin tahmin anından sonrasını kullanmaması; katılımcı/zaman ayrımları ve sonuç aralıklarının örtüşmemesi.
- Metrik hesapları ve günlük yanlış uyarı sayısı.
- Aynı sentetik girdilerden Python ve Android tarafında eş özellikler çıkarılması; model girdi/çıktısı ve uçtan uca akış.

`fixtures/` yalnızca küçük, sentetik ve paylaşılabilir örnekler içindir. Oturum çıkarımı ayrıca plandaki gibi en az beş katılımcının elle kontrol edilmiş örnekleriyle yerel olarak doğrulanır; bu gerçek kayıtlar repoya eklenmez.

Değerlendirme kontrollerini Hamza, entegrasyon ve sistem testlerini Yiğit koordine eder; veri ve Android sorumluları kendi bileşenlerine katkı verir.
