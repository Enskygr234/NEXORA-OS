# Uyumluluk

| | |
|---|---|
| **minSdk** | 26 (Android 8.0 Oreo) |
| **targetSdk / compileSdk** | 35 (Android 15) |
| **Mimari** | Tüm ABI'ler (yerel kod yok) |
| **Yön** | Dikey ve yatay; activity yön değişiminde yeniden oluşturulmaz, widget'lar korunur |

## Neden Android 8.0?

- `LauncherApps.getProfiles()`, `ApplicationInfo.category` ve uyarlanabilir (adaptive) ikonlar API 26'da geldi. İkon şekli seçimi ve kategoriler bunlara dayanır.
- `java.time` (saat/tarih biçimlendirme) API 26'dan itibaren kütüphane eklemeden kullanılabilir.
- Android 8.0 altı cihazlar 2026 itibarıyla aktif cihazların çok küçük bir kısmı.

## Sürüme göre davranış farkları

| Özellik | Android 8–9 | Android 10+ | Android 11+ | Android 12+ |
|---|---|---|---|---|
| Varsayılan ana ekran isteği | Sistem "Ana ekran uygulaması" ayar sayfası açılır | `RoleManager` onay penceresi | aynı | aynı |
| Uygulama görünürlüğü | Kısıt yok | Kısıt yok | `<queries>` ile launcher ve widget uygulamaları görünür | aynı |
| Erişilebilirlik kategorisi | "Diğer" altında | "Diğer" altında | "Diğer" altında | Ayrı kategori |
| Kenardan kenara (edge-to-edge) | Uygulanır | Uygulanır | Uygulanır | Android 15'te zorunlu; destekleniyor |

## Bilinen cihaz farklılıkları

- Bazı üreticiler (MIUI/HyperOS, ColorOS) üçüncü parti launcher'larda "son uygulamalar" hareketini kısıtlayabilir. Bu sistem kısıtıdır, uygulama içinden aşılamaz.
- Widget bağlama izin penceresi ilk kez her widget sağlayıcı için bir kez çıkar; bazı cihazlarda "Her zaman izin ver" kutusu bulunur.
- İş profili kilitliyken iş uygulamaları geçici olarak listeden kalkar; sabitlemeleri silinmez.
