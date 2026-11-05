# Kurulum, APK oluşturma ve telefona yükleme

## 1. Projeyi açma

1. [Android Studio](https://developer.android.com/studio) Ladybug (2024.2) veya daha yenisini kur.
2. **File → Open** ile `nexora-os` klasörünü aç.
3. İlk açılışta Gradle eşitlemesi (sync) gerekli kütüphaneleri indirir. Android SDK 35 yüklü değilse Android Studio önerir; kabul et.
4. JDK: Android Studio'nun kendi JDK'sı (17+) yeterli. *Settings → Build Tools → Gradle → Gradle JDK*.

## 2. Debug APK oluşturma

Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

Komut satırı:

```bash
./gradlew assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk`

Debug sürümün paket adı `com.winterdeveloper.nexora.debug`'dır; release sürümüyle aynı telefonda yan yana durabilir.

### APK'yı bilgisayar olmadan almak (GitHub Actions)

Proje GitHub'a yüklendiğinde her gönderimde Actions debug APK derler:

1. Repo → **Actions** sekmesi → en son "Android build" çalışması.
2. Sayfanın altındaki **Artifacts** bölümünden `nexora-os-debug-apk` indir (zip içinde APK).

Derleme başarısız olursa kırmızı çarpıya tıklayıp hata logunu görebilirsin.

## 3. Release APK

```bash
./gradlew assembleRelease
```

Çıktı: `app/build/outputs/apk/release/`. İmzalama ayarlanmadıysa dosya `app-release-unsigned.apk` olur ve **telefona kurulamaz**. İmzalama için bkz. [SIGNING.md](SIGNING.md).

## 4. Telefona kurma

**USB ile (adb):**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Telefonda *Ayarlar → Telefon hakkında → Yapı numarası*'na 7 kez dokunup Geliştirici seçeneklerini aç, sonra *USB hata ayıklama*'yı aç.

**Dosya ile:**

1. APK'yı telefona gönder (Drive, Telegram, USB…).
2. Dosyaya dokun. Android "Bilinmeyen uygulamalar" iznini sorarsa o kaynak (ör. Dosyalar) için izin ver.
3. **Yükle**'ye dokun.

Play Protect uyarı gösterirse bu, mağaza dışı ve tanınmayan bir imzayla yüklendiği içindir.

## 5. Varsayılan ana ekran yapma

1. NEXORA OS'u aç (uygulama listesinde görünür).
2. Boş alana basılı tut → ⚙ (Ayarlar) → **Varsayılan yap**.
3. Android 10+ sistemi bir onay penceresi açar; NEXORA OS'u seç.
   Açılmazsa: *Ayarlar → Uygulamalar → Varsayılan uygulamalar → Ana ekran uygulaması → NEXORA OS*.

Uygulama kısayolları (basılı tut menüsü) yalnızca NEXORA OS varsayılan ana ekranken görünür. Bu Android'in kuralıdır.

## 6. Geri dönmek

*Ayarlar → Uygulamalar → Varsayılan uygulamalar → Ana ekran uygulaması*'ndan eski launcher'ını seç ya da NEXORA OS'u kaldır.

## İlk kullanım

- **Uygulama çekmecesi:** yukarı kaydır veya dock'un sağındaki uygulamalar düğmesine dokun.
- **Ana ekrana / favorilere ekleme:** çekmecede uygulamaya basılı tut.
- **Düzenleme modu:** ana ekranda boş alana basılı tut. İkonları basılı tutup sürükle, kırmızı rozetle kaldır, widget ekle.
- **Hava durumu:** ana ekrandaki "Hava durumu için şehrini seç"e dokun → şehrini ara ve seç.
