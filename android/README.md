# Android uygulaması

**Ana sorumlu:** Murat Gürel. **Entegrasyon desteği:** Yiğit Kayalı.

Kotlin Android projesi Android Studio ile bu klasörün içinde kurulacaktır. Henüz Gradle projesi veya çalıştırılabilir uygulama yoktur.

## Planlanan başlangıç

1. Android 9 / API 28 minimum sürümüyle projeyi kurmak.
2. Kullanıcı onamı ve sistem ayarlarından kullanım erişimi izni akışını hazırlamak.
3. UsageStatsManager olaylarını toplayıp farklı Android sürümlerinde erişilebilirliği, arka plan davranışını ve pil etkisini kontrol etmek.
4. Room ile yerel saklama, WorkManager ile zamanlanmış işleme ve Jetpack Compose ile temel ekranları geliştirmek.
5. Tamamlanan 30 dakikalık pencerenin özelliklerini Python ile aynı kurallarla hesaplamak; ortak sentetik örneklerle eşliği doğrulamak.
6. Yiğit ile model tahminini bağlamak; günlük sınır, sessiz saatler, kapatma/duraklatma ve geri bildirim akışlarını eklemek.

ONNX Runtime Mobile henüz kesinleşmiş bir bağımlılık değildir. Mobil tahmin formatı boyut, uyumluluk ve gecikme denemesinden sonra seçilecektir. Cihazda kişiselleştirme de ayrıca değerlendirilecektir.

Gerçek kullanım kayıtları ve geri bildirimler repoya eklenmez. “Hayır” yanıtı ve atlanan soru ayrı tutulur; hatırlatmayı kapatma eylemi kullanım etiketi yerine geçmez. Ayrıntılar [geliştirme notlarında](../documents/development_notes.md) yer alır.
