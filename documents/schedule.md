# Takvim

**Hedef teslim/final sunum: 27 Kasım 2026.**

Aşağıdaki tarihler [uygulama planındaki](concept_note_implementation_plan.pdf) özgün takvimdir. 29 Ekim 2026 itibarıyla uygulamaya başlanmamıştır; tarihlerin geçmiş olması işlerin tamamlandığını göstermez. Bu dosya yeni bir takvim kararı içermez; başlangıç işlerinin durumu ekip ilerledikçe güncellenir.

## Kaynak plandaki aşamalar

| Tarih (2026) | Aşama | Planlanan çıktı |
| --- | --- | --- |
| 12–18 Ekim | Kavram notu ve uygulama planı | Kapsam, mimari, takvim ve görev matrisi |
| 19–30 Ekim | Veri hazırlama ve özellik çıkarımı | Veri doğrulama, oturumlar, özellikler, proxy tanımı, veri kalitesi raporu ve veri seti v1; 30 Ekim sunumu |
| 19 Ekim–10 Kasım | Paralel Android hazırlığı | İzinler, olay toplama ve pil denemesi; ardından yerel saklama, özellik eşliği ve arayüz |
| 31 Ekim–4 Kasım | Başlangıç ve genel modeller | Sabit kural, lojistik regresyon, Random Forest ve XGBoost karşılaştırması |
| 3–10 Kasım | Kişiselleştirme ve değerlendirme | 1/3/7/14 gün karşılaştırmaları, kullanıcı bazlı sonuçlar, SHAP, model seçimi ve dışa aktarımı |
| 11–17 Kasım | Entegrasyon ve pilot | Cihazda tahmin, hatırlatmalar, yetişkin pilotu, sürüm adayı ve hata düzeltmeleri |
| 18–26 Kasım | Rapor ve sunum hazırlığı | Tam taslak ve ekip provası |
| 27 Kasım | Final | Teslim/final sunum |

## Yaklaşan kontrol noktaları

- **4 Kasım:** Başlangıç ve genel modellerin aynı değerlendirme düzeninde karşılaştırılması.
- **10 Kasım:** Üç model yaklaşımının değerlendirilmesi; model seçimi ve dışa aktarımı.
- **12 Kasım:** Mobil tahmin formatının testlere göre seçilmesi.
- **14 Kasım:** En az iki telefonda 24 saat çökmeden kayıt; pil etkisinin ölçülmesi.
- **17 Kasım:** Onam, sınırlı hatırlatmalar ve geri bildirim içeren entegre sürüm adayı; pilot uygulanabilirlik bulguları.
- **26 Kasım:** Tam sunum taslağının tüm ekiple prova edilmesi.
- **27 Kasım:** Final sunumu.

Başlangıçtaki paralel işler ve sorumluları [görev dağılımında](task_distribution.md) yer alır. Haftalık gözden geçirmede gerçek ilerleme ve takvim değişiklikleri kaydedilir.
