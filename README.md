# NEXORA OS

**Redefine Your Android Experience.**
Winter Developer tarafından geliştirilen, cyberpunk ve fütüristik tasarım dilli bir Android ana ekran (launcher) uygulaması.

> **Durum: 1.0.0, ilk sürüm. Henüz derlenip cihazda denenmedi.** Kod, Android SDK'ya erişimi olmayan bir ortamda yazıldı. İlk derleme ve testler GitHub Actions'ta çalışacak (bkz. [Derleme](#derleme)). Ekran görüntüleri uygulama cihazda çalıştırıldıktan sonra `docs/screenshots/` klasörüne eklenecek.

## Özellikler

Aşağıdakilerin hepsi gerçek Android API'leriyle yazıldı. Taklit edilen ya da sahte veri gösteren bir özellik yok.

**Ana ekran**
- Dijital saat (3 stil: Büyük, Neon, Minimal; 24 saat ve saniye seçenekleri). Saate dokununca alarm uygulaması, tarihe dokununca takvim açılır.
- Tarih ve gün bilgisi (cihaz diline göre).
- Hava durumu: [Open-Meteo](https://open-meteo.com) verisi. API anahtarı yok, konum izni yok; şehri kullanıcı ayarlardan seçer.
- Arama çubuğu (çekmeceyi arama odaklı açar).
- Widget alanı (sistemdeki her widget eklenebilir; yükseklik ve sıra ayarlanır).
- Ana ekran uygulama ızgarası: basılı tut → sürükle-bırak ile sırala, kırmızı rozetle kaldır.
- Favoriler dock'u (ilk 5 favori) ve çekmece düğmesi.
- Yukarı kaydırınca çekmece açılır. Boş alana basılı tutunca düzenleme modu açılır.
- Ana ekran tuşu: zaten ana ekrandayken basılırsa açık çekmece, klasör ve düzenleme modu kapanır.

**Uygulama çekmecesi**
- Başlatılabilir tüm uygulamalar `LauncherApps` API'siyle okunur; iş profili (work profile) uygulamaları da listelenir.
- Hızlı arama: Türkçe karakterleri ve aksanları yok sayar (`cicek` → "Çiçek Sepeti", `İnsta` → "Instagram"), baş harflerle bulur (`gps` → "Google Play Store"). Klavyedeki "Git" ilk sonucu açar.
- Alfabetik sıralama (Türkçe harf sırasına uygun) ve harf başlıkları.
- Sekmeler: Tümü, Favoriler, Kategoriler (Android'in uygulama kategorileri), Klasörler.
- Klasör oluşturma, yeniden adlandırma, silme; klasöre uygulama ekleme/çıkarma.
- Uygulamaya dokununca gerçek uygulama açılır.

**Uygulama menüsü (basılı tutunca)**
- Uygulama kısayolları (NEXORA OS varsayılan ana ekran olduğunda; bu bir Android kuralıdır).
- Ana ekrana ekle/kaldır, favorilere ekle/çıkar, klasöre ekle, yeniden adlandır, gizle, uygulama bilgisi / kaldırma.

**Kişiselleştirme (ayrı Ayarlar ekranı)**
- 5 renk teması: Neon Mavi, Elektrik Moru, Siber Nane, Plazma Pembe, Kutup Beyazı.
- Tema profilleri: mevcut görünümü (tema, arka plan, ikon boyutu/şekli, ızgara, saat, animasyonlar) adla kaydet, tek dokunuşla geri yükle.
- Arka plan: NEXORA gradyanı, sistem duvar kâğıdı (karartma ayarıyla) veya OLED siyah.
- Duvar kâğıdı seçimi ve canlı duvar kâğıdı seçimi (sistem seçicileri).
- İkon boyutu (40–72 dp), ikon şekli (Sistem, Daire, Yumuşak kare, Yuvarlak kare), uygulama adlarını göster/gizle.
- Izgara sütun sayısı (3–6).
- Animasyonları aç/kapat (kapalıyken tüm geçişler anında olur).
- Gizli uygulamalar listesi ve geri gösterme.
- Bildirim sayacı rozetleri (kullanıcı bildirim erişimi verirse; yalnızca paket adları sayılır, içerik okunmaz).
- Yedekleme ve geri yükleme: kullanıcının seçtiği bir JSON dosyasına.
- Ana ekran düzenini sıfırlama. Düzen her değişiklikte otomatik kaydedilir.

## Bilinen sınırlar ve eksikler

Bunlar bilerek yapılmadı ya da henüz yapılmadı:

| Konu | Durum |
|---|---|
| Derleme ve cihaz testi | Kod henüz derlenmedi. İlk GitHub Actions çalışmasında hata çıkarsa düzeltilmesi gerekecek. |
| Ekran görüntüleri | Yok. Uygulama çalıştırıldıktan sonra eklenecek. |
| Üçüncü parti ikon paketleri | Desteklenmiyor. İkon şekli ve boyutu değiştirilebiliyor. |
| Çoklu ana ekran sayfası | Yok. Tek, kaydırılabilir bir ana ekran var. |
| Widget yerleşimi | Widget'lar tam genişlikte, alt alta durur. Yükseklik ve sıra ayarlanır; serbest ızgara yerleşimi yok. |
| Çekmeceden ana ekrana sürükle-bırak | Yok. Basılı tut menüsündeki "Ana ekrana ekle" ile yapılıyor. |
| Aşağı kaydırınca bildirim paneli | Yok. Bu yalnızca gizli (herkese açık olmayan) Android API'siyle yapılabiliyor. |
| Widget'ların yedeklenmesi | Yok. Android widget kimliklerini her kurulum için ayrı verdiği için taşınamaz. |
| Uygulama gizleme | Yalnızca NEXORA OS içinde gizler. Uygulama telefonda yüklü ve Ayarlar'da görünür kalır (Android başka bir uygulamayı sistemden gizlemeye izin vermez). |

## Ekran görüntüleri

`docs/screenshots/` klasörüne eklenecek. Bkz. [docs/screenshots/README.md](docs/screenshots/README.md).

## Derleme

Gereken: Android Studio Ladybug (2024.2) veya daha yenisi, JDK 17.

```bash
./gradlew testDebugUnitTest     # birim testleri
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # imzalama ayarlıysa imzalı release APK
```

Ayrıntılar:
- [Kurulum ve APK rehberi](docs/INSTALL.md): projeyi açma, APK oluşturma, telefona kurma, varsayılan ana ekran yapma.
- [İmzalama rehberi](docs/SIGNING.md): release anahtarı oluşturma ve GitHub Secrets.
- [Mimari](docs/ARCHITECTURE.md)
- [Uyumluluk](docs/COMPATIBILITY.md): minimum Android sürümü ve sürüm farkları.
- [Test planı](docs/TESTING.md)

## GitHub Actions

`.github/workflows/android.yml` her `main` gönderiminde ve PR'da:
1. Birim testlerini çalıştırır,
2. Debug APK derler ve **nexora-os-debug-apk** adıyla Actions sayfasına yükler,
3. Lint raporu üretir,
4. API 34 emülatöründe cihaz testlerini çalıştırır.

`v1.0.0` gibi bir etiket gönderilince release APK derlenir ve GitHub Releases'e eklenir (imzalama sırları tanımlıysa imzalı).

## İzinler

| İzin | Neden |
|---|---|
| `INTERNET` | Yalnızca Open-Meteo'dan hava durumu almak için. |
| Bildirim erişimi (isteğe bağlı) | Yalnızca kullanıcı rozetleri açıp sistem ayarlarından izin verirse. |

Konum, rehber, depolama, `QUERY_ALL_PACKAGES` gibi izinler istenmez. Uygulama listesi `<queries>` ve `LauncherApps` ile okunur.

## Lisans

MIT. Bkz. [LICENSE](LICENSE). Hava durumu verisi Open-Meteo.com'dan, CC BY 4.0 lisansıyla.

---

© 2026 Winter Developer
