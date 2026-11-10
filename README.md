# Glass Launcher 🧊

Launcher Android bertema **liquid glass** ala iOS, dibangun dengan Kotlin + Jetpack Compose.
Mendukung **widget Android standar**, **app drawer**, **dock**, dan **secondary display**
(asli maupun simulasi dari Developer Options).

## Fitur

- **Liquid glass UI** — panel kaca transparan dengan gradient, border highlight, dan wallpaper yang di-blur di belakangnya (`Modifier.blur`, butuh Android 12+ untuk blur asli; di bawahnya otomatis fallback ke overlay gelap).
- **Homescreen 2 halaman** — halaman widget + halaman grid aplikasi, dengan indikator halaman.
- **Widget support** — tambah widget lewat *Pengaturan → Tambah widget* (memakai picker sistem Android), widget di-host dengan `AppWidgetHost` dan bisa dihapus per kartu.
- **App drawer** — buka lewat tombol menu di dock, ada pencarian aplikasi.
- **Dock** ala iOS dengan 4 aplikasi + tombol drawer.
- **Jam & tanggal** besar di atas.
- **Secondary display** — buka *Pengaturan → Layar sekunder* untuk melihat daftar display dan menjalankan "Glass Home" di display mana pun. Bisa dites tanpa monitor lewat simulasi (lihat di bawah).

## Cara Build

1. Clone repo ini.
2. Buka di **Android Studio** (Koala/terbaru, JDK 17 bawaan).
3. Jika Android Studio menampilkan error `gradle-wrapper.jar` hilang, pilih opsi **perbaiki/recreate wrapper** yang muncul, atau jalankan `gradle wrapper` di terminal proyek. (File jar wrapper biner tidak disertakan di repo ini.)
4. `Build → Make Project`, lalu `Run` di perangkat/emulator **Android 8.0 (API 26) ke atas**.

## Set sebagai Launcher Default

Setelah install: `Settings → Apps → Default apps → Home app → Glass Launcher`, atau tekan Home lalu pilih Glass Launcher.

## Mencoba Secondary Display (tanpa monitor)

1. Aktifkan **Developer options** di HP.
2. Buka **Developer options → Simulate secondary displays** → pilih misalnya *720p*.
3. Buka Glass Launcher → **Pengaturan → Layar sekunder** → daftar display akan muncul → tekan **Buka** pada layar simulasi.
4. "Glass Home" akan tampil di display tersebut; ketuk ikon aplikasi untuk menjalankannya di display yang sama.

## Batasan yang Diketahui

- Widget yang butuh konfigurasi awal dibuka tanpa wizard konfigurasi (versi awal).
- Blur wallpaper butuh Android 12 (API 31)+; di Android 8–11 tampil sebagai overlay gelap tanpa blur.
- Beberapa ROM membatasi peluncuran aplikasi pihak ketiga di display sekunder; jika aplikasi terbuka di layar utama, itu keterbatasan ROM, bukan bug launcher.

## Struktur Proyek

```
app/src/main/java/com/kerenpandi/glasslauncher/
├── GlassLauncherApp.kt          # Application, inisialisasi WidgetRepository
├── LauncherActivity.kt          # HOME activity utama
├── SecondaryHomeActivity.kt     # Glass Home untuk layar sekunder
├── LauncherViewModel.kt         # State daftar aplikasi & widget
├── data/
│   ├── AppRepository.kt         # Query aplikasi ter-install
│   └── WidgetRepository.kt      # AppWidgetHost + persistensi widget
└── ui/
    ├── theme/Theme.kt           # GlassTheme (dark + transparan)
    ├── glass/Glass.kt           # Modifier glassSurface, GlassPanel, WallpaperBackdrop
    ├── common/Components.kt     # AppIcon, ClockHeader, GlassIconButton
    └── home/                    # HomeScreen, AppDrawer, SettingsSheet
```

## Lisensi

MIT — lihat [LICENSE](LICENSE).
