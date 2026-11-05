# Mimari

NEXORA OS tek bir Gradle modülünden (`:app`) oluşur ve Clean Architecture katmanlarına **paket** düzeyinde ayrılır. Bir launcher'ın hızlı açılması önemli olduğu için çoklu Gradle modülü ve DI çatısı (Hilt/Koin) kullanılmadı. Katmanlar ileride ayrı modüllere bölünebilecek şekilde bağımlılık yönüne uyar.

```
com.winterdeveloper.nexora
├── core/                 Ortak altyapı
│   ├── AppContainer.kt   Elle yazılmış bağımlılık kabı (süreç başına bir tane)
│   └── designsystem/     Tema, renk paletleri, cam efekti, animasyon ayarı
├── domain/               Saf Kotlin: Android'e bağımlı değil, birim testli
│   ├── model/            AppKey, LauncherApp, Appearance, WeatherReport...
│   ├── repository/       Depo arayüzleri
│   └── usecase/          AppSearch (sıralamalı arama), ListOps (taşıma, alfabetik sıra)
├── data/                 Depo uygulamaları
│   ├── apps/             LauncherApps ile uygulama listesi, kısayollar, ikon önbelleği
│   ├── layout/           Ana ekran, favoriler, klasörler, gizli ve yeniden adlandırılan uygulamalar (Room)
│   ├── local/            Room veritabanı, tablolar, DAO'lar
│   ├── settings/         DataStore ayarları, tema profilleri
│   ├── weather/          Open-Meteo istemcisi + önbellek
│   └── backup/           JSON yedekleme / geri yükleme
├── launcher/             Ana ekran (Presentation)
│   ├── MainActivity      HOME intent'ini karşılayan activity
│   ├── LauncherViewModel
│   ├── DefaultLauncher   Varsayılan ana ekran kontrolü ve isteği
│   └── ui/               Compose ekranları: HomeScreen, AppDrawer, dialoglar, saat, hava
├── widgets/              AppWidgetHost yönetimi, widget alanı ve seçici
├── settings/             Ayarlar activity'si, ViewModel ve ekranı (Customization dahil)
└── notifications/        Bildirim rozetleri için NotificationListenerService
```

## Bağımlılık yönü

`launcher / settings / widgets (UI)` → `domain` ← `data`

- UI katmanı yalnızca ViewModel'leri ve domain modellerini görür.
- ViewModel'ler depo **arayüzlerine** bağlıdır (`domain/repository`). Tek istisna `AppsRepositoryImpl`: ikon yükleme ve iş profili bilgisi için platform tiplerine (LauncherActivityInfo) ihtiyaç duyulduğundan somut sınıf olarak verilir.
- `domain` paketi hiçbir Android sınıfı içe aktarmaz; bu yüzden JVM'de hızlı test edilir.

## Veri saklama

| Veri | Yer | Neden |
|---|---|---|
| Görünüm ve hava ayarları | DataStore Preferences (`nexora_settings`) | Küçük anahtar-değer verisi, Flow ile canlı |
| Son hava durumu okuması | DataStore (`nexora_weather`) | 30 dk önbellek; her açılışta ağa gidilmez |
| Ana ekran, favoriler, widget'lar, klasörler, gizli/yeniden adlandırılan uygulamalar, tema profilleri | Room (`nexora.db`) | İlişkisel veri, sıralama, işlem (transaction) |

Uygulamalar `AppKey` (paket + activity sınıfı + kullanıcı seri numarası) ile tanımlanır. Seri numarası yeniden başlatmada değişmez, böylece iş profili uygulamaları da kalıcı olarak sabitlenebilir.

`android:allowBackup="false"`: veriler bulut yedeğine gitmez. Kullanıcı isterse Ayarlar → Yedekleme ile kendi seçtiği dosyaya alır.

## Akışlar

**Uygulama listesi:** `AppsRepositoryImpl` açılışta ve `LauncherApps.Callback` her paket değişikliğinde listeyi arka planda yeniden okur. Kaldırılan uygulamaların sabitlemeleri `prune` ile 3 sn gecikmeyle temizlenir; o an görünmeyen (kilitli) iş profilinin uygulamalarına dokunulmaz.

**Widget ekleme:** ID ayır → `bindAppWidgetIdIfAllowed` → gerekirse sistemin izin diyaloğu (`ACTION_APPWIDGET_BIND`) → widget'ın ayar ekranı varsa `startAppWidgetConfigureActivityForResult` → Room'a kaydet. Herhangi bir adım iptal edilirse ID silinir. `AppWidgetHost` uygulama süreci boyunca yaşar; ekran döndürmede widget'lar yeniden kurulmaz.

**Hava durumu:** Şehir adı → Open-Meteo geocoding → enlem/boylam ayarlara kaydedilir → `forecast?current=...` çağrısı. Konum izni kullanılmaz.

**Animasyonlar:** Tüm UI geçişleri `nexoraTween()` kullanır; "Animasyonlar" kapalıysa `snap()` döner. Hareketli arka plan ışıması da aynı ayara bağlıdır.

## Güvenlik ve performans notları

- İstenen izin: yalnızca `INTERNET`. `QUERY_ALL_PACKAGES` yerine manifestte `<queries>` var.
- Bildirim dinleyicisi yalnızca paket adına göre sayar; bildirim başlığı/içeriği okunmaz, saklanmaz, loglanmaz.
- Loglarda kullanıcı verisi yok. Release derlemesinde `Log.d/v` çağrıları R8 ile silinir.
- İkonlar arka planda yüklenir, bellek bütçeli `LruCache`'te tutulur, `onTrimMemory`'de boşaltılır.
- Ağ çağrıları 10 sn zaman aşımlı, `Dispatchers.IO` üzerinde. Hatalar `Result` olarak döner, çökme yerine UI'da mesaj gösterilir.
- Kalıcı arka plan işi (WorkManager, servis) yok. Hava durumu yalnızca ana ekran öne gelince ve önbellek 30 dk'dan eskiyse yenilenir.
