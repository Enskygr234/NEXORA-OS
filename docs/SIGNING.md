# İmzalama rehberi

Release APK'nın telefona kurulabilmesi için imzalanması gerekir. Anahtar (keystore) **asla** repoya eklenmez; `.gitignore` `*.jks`, `*.keystore` ve `keystore.properties` dosyalarını dışarıda tutar.

> Anahtarı ve şifrelerini güvenli bir yerde yedekle. Kaybedersen aynı uygulamanın güncellemesini yayınlayamazsın.

## 1. Anahtar oluşturma

```bash
keytool -genkeypair -v \
  -keystore nexora-release.jks \
  -alias nexora \
  -keyalg RSA -keysize 4096 -validity 10000
```

Android Studio ile: **Build → Generate Signed App Bundle / APK → APK → Create new...**

## 2. Yerel derleme için

Proje kökünde `keystore.properties` oluştur (git'e girmez):

```properties
storeFile=/tam/yol/nexora-release.jks
storePassword=...
keyAlias=nexora
keyPassword=...
```

Sonra:

```bash
./gradlew assembleRelease
```

Çıktı: `app/build/outputs/apk/release/app-release.apk`

Alternatif olarak aynı değerler ortam değişkenleriyle verilebilir: `NEXORA_KEYSTORE_FILE`, `NEXORA_KEYSTORE_PASSWORD`, `NEXORA_KEY_ALIAS`, `NEXORA_KEY_PASSWORD`.

## 3. GitHub Actions ile imzalı sürüm

Repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Değer |
|---|---|
| `NEXORA_KEYSTORE_BASE64` | `base64 -w0 nexora-release.jks` çıktısı (macOS: `base64 -i nexora-release.jks`) |
| `NEXORA_KEYSTORE_PASSWORD` | keystore şifresi |
| `NEXORA_KEY_ALIAS` | `nexora` |
| `NEXORA_KEY_PASSWORD` | anahtar şifresi |

Sürüm yayınlamak için:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow release APK'yı derler, imzalar ve GitHub Releases sayfasına ekler. Secret tanımlı değilse APK imzasız yayınlanır (kurulamaz).

## 4. İmzayı doğrulama

```bash
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app-release.apk
```
