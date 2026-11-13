# Glass Launcher

Launcher Android bertema liquid glass ala iOS, dibangun dengan Kotlin + Jetpack Compose dan ditujukan tetap ringan untuk perangkat entry-level.

## Fitur

- Liquid-glass home screen di atas wallpaper sistem.
- Halaman aplikasi dengan swipe horizontal dan indikator halaman.
- Dock kaca yang dapat diatur hingga empat aplikasi.
- Susun ulang ikon dan menu long-press.
- Folder bergaya iPhone: panel tengah, grid ikon 3 kolom, edit nama, tambah dan keluarkan aplikasi.
- Aplikasi clone / Dual Apps melalui LauncherApps dan profil pengguna.
- Info aplikasi dan uninstall melalui dialog sistem Android.
- Pencarian aplikasi cepat (geser ke atas atau ikon cari).
- Badge jumlah notifikasi per aplikasi melalui NotificationListenerService.
- Gestur: geser atas membuka pencarian, geser bawah membuka panel notifikasi, double-tap mengunci layar (via device admin).
- Sembunyikan aplikasi dari home, drawer, dan pencarian.
- Kustomisasi: jumlah kolom grid, ukuran ikon, bentuk ikon (termasuk gaya iPhone: squircle superellipse dengan latar putih), label, dan icon pack format ADW.
- Backup dan restore layout serta pengaturan (JSON via Storage Access Framework).
- At a Glance: cuaca (Open-Meteo) dan agenda kalender berikutnya.
- Tema gelap, terang, sistem, dan warna dinamis Material You.

## Performa

- Tidak menggunakan blur bitmap full-screen.
- Wallpaper ditampilkan langsung oleh window launcher.
- Ikon aplikasi di-cache.
- Halaman pager tetangga di-pre-compose.
- Data halaman di-memoize agar daftar tidak di-chunk ulang pada setiap recomposition.
- Grid halaman dibatasi enam baris per halaman.

## Shizuku (opsional)

Fitur "Hentikan aplikasi" bekerja optimal dengan [Shizuku](https://shizuku.rikka.app/). Saat Shizuku aktif, launcher akan meminta izin dan memakai `am force-stop`. Tanpa Shizuku, launcher otomatis memakai `killBackgroundProcesses` sebagai fallback.

## Rilis

Metadata fastlane untuk F-Droid tersedia di `fastlane/metadata/android/`. APK debug/release dapat dibangun lewat GitHub Actions.

## Build

Jalankan `./gradlew assembleDebug` atau gunakan GitHub Actions. APK debug tersedia di tab Actions → Build → Artifacts.

## Set sebagai launcher default

Settings → Apps → Default apps → Home app → Glass Launcher.

## Struktur

```
app/src/main/java/com/kerenpandi/glasslauncher/
├── LauncherActivity.kt
├── LauncherViewModel.kt
├── data/
└── ui/
    ├── theme/
    ├── glass/
    ├── common/
    └── home/
```

Lisensi MIT.
