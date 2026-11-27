# Android için genel model paketi

Prototip için önceden belirlenmiş `dataset_v1` fold_1 XGBoost artefaktı kullanıldı. Kaydedilen model ve doğrulama seçimi, [deney kaydıyla](../reports/model_refinement_dataset_v1/results.json) eşleşiyor. Model 150 ağaç, azami derinlik 5 ve XGBoost 3.2.0 ile kaydedilmiş. Üç fold'daki dokuz Python artefaktı yeniden yüklenerek tahmin üretimi kontrol edildi. Fold seçimi prototip için önceden belirlenmişti ve eğitim tekrarlanmadı.

## Paket ve tahmin sözleşmesi

Yerel paket `models/mobile/dataset_v1-xgboost-fold-1-v1/` altında. Sürüm `dataset-v1-xgboost-fold-1-aa2dd42aaefb`.

| Dosya | İçerik |
| --- | --- |
| `model.onnx` | 269.121 bayt. Medyan doldurma ve XGBoost modelinden pozitif sınıf skoruna kadar tek ONNX grafiği. |
| `manifest.json` | Model sürümü, model dosyası SHA-256, girdi/çıktı tensörleri, karar eşiği ve kaynak deney bilgileri. |
| `feature_schema.json` | Sıralı alanlar, Kotlin alan eşlemeleri, birimler ve Python girdi sınırları. |
| `preprocessing.json` | Eğitimde öğrenilen medyanlar, öğrenilen eksiklik göstergelerinin indisleri ve sınıflandırıcı sütun sırası. |
| `golden_samples.json` | 16 sentetik girdi, özgün Python skorları ve beklenen kararlar. |
| `parity_report.json` | Toplu eşlik sonuçları, tolerans ve araç sürümleri. |

| İndis | Model alanı | `FeatureVector` | Anlam |
| --- | --- | --- | --- |
| 0 | `screen_s` | `screenSeconds` | Tamamlanan penceredeki toplam ekran süresi, saniye. |
| 1 | `sessions_started` | `sessionsStarted` | Pencerede başlayan oturum sayısı. |
| 2 | `longest_run_s` | `longestRunSeconds` | Bir oturumun pencere içindeki en uzun kesintisiz ekran süresi, saniye. |
| 3 | `app_switches` | `appSwitches` | Uygulama geçişi sayısı. |
| 4 | `prev_session_s` | `previousSessionSeconds` | Pencere başlangıcında veya öncesinde biten son oturumun sınırlanmış süresi, saniye. Geçmiş bilinmiyorsa mevcut akış 0 kullanıyor. |
| 5 | `hour` | `hour` | Pencere başlangıcının yerel saati, 0–23. |
| 6 | `dow` | `dayOfWeek` | Pencere başlangıcının yerel günü, Pazartesi 0 ve Pazar 6. |

- `AppConfig.FEATURE_SCHEMA_VERSION` değeri `v1`. ONNX girdisi `features`, `float32[batch, 7]`; tek pencere için `[1, 7]`. Grafiğe ham değerler verilir. Ön işleme grafikte bir kez uygulanır.
- Fold_1 medyanları özellik sırasında `[3.771499991416931, 0, 3.6449999809265137, 0, 72.8970000743866, 12, 3]`. `SimpleImputer(add_indicator=True, keep_empty_features=True)` bu eğitimde gösterge sütunu öğrenmedi. Grafik NaN değerleri medyanla doldurur. Dönüştürücü, öğrenilmiş göstergeler varsa eski eksiklik maskesini aynı indis sırasıyla sona ekler. Ölçekleme uygulanmaz.
- Golden örneklerindeki JSON `null`, ONNX tensöründe IEEE NaN olarak beslenir. Bu örnekler modelin doğrudan girişini sınar. Uygulamanın canlı `PredictionRunner` akışı sonlu, sınırlar içindeki vektörleri ve yeterli gözlem kalitesini ister.
- `positive_score`, pencere başına tek `float32` değer verir. Anlamı `predict_proba[:, 1]`, yani sonraki 30 dakikanın `y_next_any` davranışsal proxy skoru. Pozitif karar `score >= 0.25`. `Predictor.predictRawScore` bu skoru döndürür ve `PredictionRunner` eşiği uygular.

## Üretim ve doğrulama

Repo kökünde modelin eğitim ortamını kullanın. Bu dışa aktarım Python 3.11.15, scikit-learn 1.9.1, XGBoost 3.2.0, ONNX 1.23.2, onnxmltools 1.16.0 ve ONNX Runtime 1.32.0 ile doğrulandı.

```bash
.venv/bin/python -m pip install -r requirements-mobile-export.txt
.venv/bin/python -m src.models.mobile_export --output models/mobile/dataset_v1-xgboost-fold-1-check
.venv/bin/python -m pytest -q --tb=short
```

[Dışa aktarım kodu](../src/models/mobile_export.py) güvenilen yerel `fold_1/xgboost.joblib` ve komşu metadata'yı kullanır. Girdi veri dosyalarının SHA-256 değerlerini kaynak deneyle karşılaştırır. ONNX şeması ve eşlik kontrolleri geçince yeni çıktı dizinini oluşturur. Var olan paketi değiştirmeden tekrar üretmek için yeni bir `--output` yolu verilir.

| Doğrulama grubu | Pencere/örnek | En büyük mutlak skor farkı | Karar farkı |
| --- | ---: | ---: | ---: |
| Uygun gerçek v1 pencereleri, tüm bölmeler | 362.255 | 6,56 × 10⁻⁷ | 0 |
| Eşiğe en yakın gerçek pencereler, tek tek | 256 | 2,68 × 10⁻⁷ | 0 |
| Golden örnekleri, tek tek | 16 | 4,17 × 10⁻⁷ | 0 |
| Sentetik eksik değer maskeleri | 128 | 1,86 × 10⁻⁷ | 0 |

Skor toleransı `1e-6`. Gerçek veri sonuçları dışa aktarım eşliğini ölçüyor. Paketteki satır düzeyinde örnekler sentetik. ONNX bağımlılıklarıyla birlikte Python test kümesi `88 passed`; ek testler sıfır ve birden çok öğrenilmiş göstergeyi kapsıyor.

Dönüştürücü, XGBoost 3.2.0'ın tek öğeli vektör `base_score` değerini `TreeEnsembleClassifier` için logit uzayına çevirir. Paket ONNX opset 13, `ai.onnx.ml` opset 1 ve IR 7 kullanıyor. Android, tam `com.microsoft.onnxruntime:onnxruntime-android:1.32.0` paketiyle CPU üzerinde çalışır. [ONNX Runtime Android kurulumu](https://onnxruntime.ai/docs/install/#install-on-android).

## Android entegrasyonu

Uygulama, modeli bir kez yükler ve manifesti, özellik sırasını, ONNX imzasını ve SHA-256'yı doğrular. Manuel sorgu ve WorkManager aynı tahmin ve Room yolunu kullanır. Kalite kontrolünü geçen her tamamlanmış pencere için bir sonraki 30 dakikanın skoru, karar eşiği, karar, zaman ve model sürümü saklanır. Aynı pencere ve model sürümü için ilk kaydedilen tahmin korunur. Hedef aralığı bitmişse tahmin geçmişe dönük olarak gösterilir. [Android derleme ve T20 komutları](../android/README.md#genel-model-ve-t20-testi).

Pixel 8 API 37 ARM64 emülatöründe T20'nin üç testi geçti. 16 sentetik golden girdide en büyük mutlak skor farkı 4,17 × 10⁻⁷ ve karar farkı sıfır. Sonlu 8 örnek `FeatureVector` ve `PredictionRunner` üzerinden de çalıştırıldı. Diğer testler yanlış şema ve yetersiz kalite reddini, Room'da doğru hedef pencerenin kaydını ve tekrar sorgusunda tahminin korunmasını doğruladı. Yerel Android birim testleri 40/40. Uygulamada manuel olay sorgusundan sonra **Son model tahmini** satırı görüldü.

Isınmış bir oturumda 100 tek pencere CPU çağrısında medyan 0,053 ms, p95 0,217 ms ölçüldü. Ölçüm Kotlin adaptöründeki tensör oluşturma, tahmin ve sonuç okumayı kapsıyor.

Kaynak verinin sabit saat ofsetiyle Android'in `ZoneId` hesabı arasındaki fark olaydan vektöre ayrı bir kontroldür. İsteğe bağlı hatırlatma gönderimi de emülatörde doğrulandı. [Hatırlatma kuralları ve doğrulama](../android/README.md#kullanım-hatırlatmaları). Fiziksel cihaz pilotunda arka plan zamanlaması ve pil etkisi açık.
