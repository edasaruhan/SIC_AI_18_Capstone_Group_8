# Repo çalışma kuralları

- Her yeni oturumda `README.md`, `documents/task_distribution.md`, `documents/development_notes.md`, `documents/model_development_plan.md` ve işin ilgili klasör belgelerini oku. Takvim için `documents/schedule.md`, yöntem ayrıntıları için mevcut araştırma ve uygulama planı PDF'lerini kullan.
- Koda başlamadan Git durumunu, mevcut kodu/testleri ve planın tamamlanan işlerini incele; repo ile planın tutarlılığını doğrula. Mevcut kodu okumadan yeniden yazma.
- Kaan'ın ana sorumluluğu model geliştirme ve kişiselleştirmedir. İsmail'in veri işleme, Hamza'nın değerlendirme, Murat'ın Android ve Yiğit'in entegrasyon sorumluluğunu koru. Başka bir alanda değişiklik gerekiyorsa önce bağımlılığı ve önerilen değişikliği açıkla; kullanıcı izni olmadan kapsamı genişletme.
- Başka ekip üyelerinin değişikliklerini silme veya üzerlerine yazma. Çakışan değişikliklerde mevcut çalışmayı koruyarak kullanıcıyla netleştir.
- Her değişikliği proje hedefi ve plandaki görevle ilişkilendir. Gereksiz dosya, altyapı, model, teknoloji veya özellik ekleme. Büyük mimari değişiklikler için kullanıcı onayı al.
- İşleri küçük ve doğrulanabilir parçalara ayır. Kod geliştirme aşamalarında çalıştırılabilir kod ve uygun testler üret; belge değişikliklerinde içerik ve bağlantı tutarlılığını kontrol et.
- Gerçek veriyle doğrulanmamış sonuçları başarılı deney olarak sunma. Sentetik testleri gerçek veri performansından, proxy etiketleri kullanıcı geri bildiriminden ayrı raporla.
- Bir görevi ancak gereken kod, test veya doğrulanmış çıktı mevcutsa tamamlandı olarak işaretle. İlerlemeyi tarihe veya çalışma süresine göre belirleme.
- Veri ya da ekip çıktısı eksikse bağımlılığı kaydet; sorumluluk sınırları içindeki bağımsız işlerle devam et. Geçici varsayımları kesin karar gibi sunma.
- Her çalışma sonunda `documents/model_development_plan.md` dosyasındaki mevcut işi, durumu, kanıtları, kararları ve sıradaki adımı güncelle; kullanıcıya tamamlanan işi ve doğrulama sonucunu özetle.
- Commit ve push için kullanıcı onayı al; önceki bir işlemin onayını sonraki çalışmalara otomatik taşıma.
