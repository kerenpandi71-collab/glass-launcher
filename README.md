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
- Kustomisasi: jumlah kolom grid, ukuran ikon, bentuk ikon, label, dan icon pack format ADW.
- Backup dan restore layout serta pengaturan (JSON via Storage Access Framework).
- Tema gelap, terang, sistem, dan warna dinamis Material You.
- GlassBerry v2.7 (app kedua, module `:berry`): HP BlackBerry virtual — layar portrait di atas, keyboard QWERTY fisik-style di tengah, touchpad kotak di bawah, nav strip paling bawah: geser satu jari = gerakkan kursor mouse (hover, dengan kursor terlihat di layar virtual), tap = klik, tap-tahan + geser = drag, dua jari = scroll, tekan lama = atur DPI dan resolusi layar virtual. Di menu aplikasi, touchpad tetap berfungsi: tap = buka aplikasi di bawah kursor, dua jari = scroll daftar. Tombol ⌫ bisa ditahan untuk hapus berulang, keyboard Android bisa diaktifkan/nonaktifkan dari pengaturan layar virtual (dan otomatis mati saat mengetik dengan keyboard GlassBerry).
- GlassBerry + Shizuku: arsitektur ala [Saku Deck](https://github.com/kerenpandi71-collab/saku-deck) (terinspirasi [Dextop by NarYuki](https://github.com/NarYuki/Dextop)) — Shizuku UserService membuat VirtualDisplay sungguhan (API 33+), aplikasi Android asli di-launch ke layar itu (`am start --display`), sentuhan di-forward langsung (multi-pointer), dan keyboard QWERTY mengetik ke aplikasi via injeksi KeyEvent.

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

Jalankan `./gradlew assembleDebug` atau gunakan GitHub Actions. APK debug tersedia di tab Actions → Build → Artifacts (launcher: `glass-launcher-debug`, GlassBerry: `glassberry-debug`). GlassBerry butuh Android 13+ (minSdk 33) dan Shizuku berjalan: buka GlassBerry → IZINKAN → pilih aplikasi dari daftar (atau tekan MENU) → aplikasi jalan di layar HP BlackBerry virtual, keyboard QWERTY bisa dipakai untuk mengetik ke aplikasi tersebut.

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
