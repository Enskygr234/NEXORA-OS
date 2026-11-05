# Ekran görüntüleri

Bu klasör şu an boş. Uygulama bir cihazda veya emülatörde çalıştırıldıktan sonra gerçek ekran görüntüleri buraya eklenecek. Uygulamanın kendisi dışında hazırlanmış (mockup) görsel eklenmeyecek.

Önerilen dosyalar (1080×2400 veya cihazın kendi çözünürlüğü, PNG):

| Dosya | İçerik |
|---|---|
| `home.png` | Ana ekran: saat, hava, arama, widget, ızgara, dock |
| `home-neon-clock.png` | Neon saat stili |
| `drawer.png` | Uygulama çekmecesi, Tümü sekmesi |
| `search.png` | Çekmecede arama sonuçları |
| `folders.png` | Klasörler sekmesi veya açık klasör |
| `edit-mode.png` | Düzenleme modu |
| `widget-picker.png` | Widget seçici |
| `settings-theme.png` | Ayarlar: tema ve profiller |
| `settings-icons.png` | Ayarlar: ikonlar ve ızgara |

Ekran görüntüsü almak için: `adb exec-out screencap -p > docs/screenshots/home.png`
