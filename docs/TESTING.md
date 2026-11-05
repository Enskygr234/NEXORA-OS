# Test planı

## Otomatik testler

| Test | Tür | Kapsam |
|---|---|---|
| `AppSearchTest` | JVM birim | Arama sıralaması, Türkçe i/İ/ı, aksanlar, baş harf, alt dizi, yeniden adlandırılmış uygulama |
| `DomainModelTest` | JVM birim | `AppKey` dönüşümü, liste taşıma, Türkçe alfabetik sıra, harf başlıkları, hava kodları, ayar sınırları |
| `LauncherInstrumentedTest.mainActivityIsAHomeCandidate` | Cihaz | Varsayılan launcher olarak atanabilirlik (HOME intent filtresi) |
| `…appListLoadsLaunchableApps` | Cihaz | Uygulama listesinin yüklenmesi, launcher'ın kendini listelememesi |
| `…homeLayoutIsSavedAndReordered` | Cihaz | Ana ekran düzeninin kaydedilmesi, sıralama, silme sonrası sıkıştırma |
| `…hidingAnAppRemovesItsPins` | Cihaz | Gizleme |
| `…pruneKeepsAppsOfInactiveProfiles` | Cihaz | Kaldırılan uygulama temizliği, kilitli iş profili koruması |
| `…foldersAndRenames` | Cihaz | Klasörler, yeniden adlandırma |
| `…settingsSurviveRestart` | Cihaz | Yeniden başlatmada ayarların korunması (DataStore yeniden açılır) |

Çalıştırma:

```bash
./gradlew testDebugUnitTest            # JVM
./gradlew connectedDebugAndroidTest    # bağlı cihaz veya emülatör
```

GitHub Actions ikisini de çalıştırır (cihaz testleri API 34 emülatöründe).

## Elle test listesi

Bunlar otomatik test edilemeyen, gerçek cihazda kontrol edilmesi gereken davranışlar:

- [ ] Ayarlar → Varsayılan yap → sistem penceresinde NEXORA OS seçilince Ana ekran tuşu NEXORA'yı açıyor.
- [ ] Çekmecede bir uygulamaya dokununca uygulama açılıyor; geri dönünce çekmece kapalı.
- [ ] Yeni uygulama kurunca/kaldırınca çekmece birkaç saniye içinde güncelleniyor.
- [ ] Ana ekrana eklenen uygulama telefon yeniden başlatılınca yerinde duruyor.
- [ ] Düzenleme modunda ikon sürükle-bırak ile yer değiştiriyor.
- [ ] Widget ekle: saat/hava/takvim widget'ı eklenebiliyor, ayar ekranı olan widget'ta ayar açılıyor, iptal edince widget eklenmiyor.
- [ ] Widget yüksekliği ve sırası değişiyor, telefon döndürülünce widget kaybolmuyor.
- [ ] Ekran döndürme (yatay): ana ekran ve çekmece düzgün görünüyor.
- [ ] Hava durumu: şehir aranıp seçilince sıcaklık geliyor; uçak modunda uygulama çökmüyor, hata mesajı çıkıyor.
- [ ] Bildirim rozetleri: izin verildikten sonra yeni bildirim gelen uygulamada sayı görünüyor.
- [ ] Kısayollar: varsayılan launcher iken basılı tut menüsünde uygulama kısayolları çıkıyor ve çalışıyor.
- [ ] Yedekle → ayarları değiştir → geri yükle: düzen ve ayarlar geri geliyor.
- [ ] Animasyonlar kapalıyken çekmece anında açılıp kapanıyor.
- [ ] Android 8, 10, 13 ve 15 cihaz/emülatörlerinde açılış.
- [ ] Küçük (5") ve büyük (tablet) ekranlar.
