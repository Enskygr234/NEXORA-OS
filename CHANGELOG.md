# Değişiklik günlüğü

Biçim [Keep a Changelog](https://keepachangelog.com/tr-TR/1.1.0/) esaslıdır; sürümler [Semantic Versioning](https://semver.org/lang/tr/) izler.

## [1.0.0] - 2026-11-06

İlk sürüm. Henüz cihazda doğrulanmadı; ilk CI derlemesi bekleniyor.

### Eklendi
- Varsayılan ana ekran olarak atanabilen launcher (HOME intent, `RoleManager` isteği).
- Ana ekran: dijital saat (3 stil), tarih, Open-Meteo hava durumu, arama, widget alanı, sürükle-bırak ile sıralanan uygulama ızgarası, favori dock'u.
- Uygulama çekmecesi: `LauncherApps` ile tüm profiller, Türkçe uyumlu hızlı arama, alfabetik sıralama, favoriler, kategoriler, klasörler.
- Uygulama menüsü: kısayollar, ana ekrana/favorilere ekleme, klasöre ekleme, yeniden adlandırma, gizleme, uygulama bilgisi.
- Widget desteği: `AppWidgetHost`, bağlama izni, yapılandırma ekranı, yükseklik ve sıra ayarı.
- Ayarlar: 5 tema, tema profilleri, arka plan modları, duvar kâğıdı ve canlı duvar kâğıdı seçimi, ikon boyutu ve şekli, ızgara, saat seçenekleri, animasyon anahtarı, gizli uygulamalar, bildirim rozetleri, yedekleme/geri yükleme, düzen sıfırlama.
- Türkçe ve İngilizce arayüz.
- Birim ve cihaz testleri, GitHub Actions derleme, test ve sürüm iş akışı.
