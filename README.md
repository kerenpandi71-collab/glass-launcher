# Glass Launcher 🧊

Launcher Android bertema **liquid glass** ala iOS, dibangun dengan Kotlin + Jetpack Compose.
Dirancang ringan untuk HP entry-level (diuji menargetkan Redmi 12C / Helio G85),
**tanpa laci aplikasi** — semua aplikasi tampil di halaman homescreen seperti iPhone.

## Fitur

- **Liquid glass UI** — panel kaca transparan dengan gradient dan border highlight, tampil di atas **wallpaper sistem asli** (tanpa blur bitmap berat → hemat RAM & baterai di HP kentang).
- **Tanpa app drawer** ala iPhone — halaman 1 berisi widget, halaman berikutnya grid aplikasi (24 aplikasi per halaman) dengan indikator halaman.
- **Widget Android standar** — tambah via *Pengaturan → Tambah widget*, di-host dengan `AppWidgetHost`.
- **Widget bisa di-resize** — tarik handle di pojok kanan-bawah kartu widget; ukuran disimpan otomatis dan isi widget ikut me-layout ulang (`updateAppWidgetSize`).
- **Dock** ala iOS dengan 4 aplikasi.
- **Desktop Mode** (dulu "layar sekunder") — desktop ala PC dengan **taskbar** kaca di bawah (terinspirasi [farmerbb/Taskbar](https://github.com/farmerbb/Taskbar)): tombol semua-aplikasi, aplikasi pinned, dan peluncuran aplikasi dalam jendela freeform bila didukung perangkat. Bisa jalan di display asli maupun simulasi.

## Optimasi untuk HP entry-level (Redmi 12C)

- Tidak ada blur full-screen (`Modifier.blur` dihilangkan) — efek glass murni gradient + translucensi.
- Wallpaper tidak pernah dibuka sebagai bitmap — window launcher menampilkan wallpaper sistem langsung (overdraw minimal).
- Ikon aplikasi di-decode sekali ke bitmap 96px dan di-cache per aplikasi.
- Tanpa laci = tidak ada layout ganda; satu daftar aplikasi dipakai bersama.

## Cara Build

1. Clone repo ini.
2. Buka di **Android Studio** (Koala/terbaru, JDK 17 bawaan), atau jalankan `./gradlew assembleDebug`.
3. APK debug juga otomatis di-build oleh GitHub Actions setiap push — unduh dari tab **Actions → Build → Artifacts**.

## Set sebagai Launcher Default

Setelah install: `Settings → Apps → Default apps → Home app → Glass Launcher`, atau tekan Home lalu pilih Glass Launcher.

## Mencoba Desktop Mode (tanpa monitor)

1. Aktifkan **Developer options** di HP.
2. Buka **Developer options → Simulate secondary displays** → pilih misalnya *720p*.
3. Buka Glass Launcher → **Pengaturan → Desktop Mode** → daftar display muncul → tekan **Buka**.
4. Desktop dengan taskbar tampil di display tersebut; ketuk ikon untuk meluncurkan aplikasi (jendela freeform bila ROM mendukung).

## Batasan yang Diketahui

- Widget yang butuh konfigurasi awal dibuka tanpa wizard konfigurasi (versi awal).
- Jendela freeform di Desktop Mode bergantung pada dukungan ROM (`FEATURE_FREEFORM_WINDOW_MANAGEMENT`); tanpa dukungan, aplikasi terbuka fullscreen di display tersebut.
- Beberapa ROM membatasi peluncuran aplikasi pihak ketiga di display sekunder; jika aplikasi terbuka di layar utama, itu keterbatasan ROM, bukan bug launcher.

## Struktur Proyek

```
app/src/main/java/com/kerenpandi/glasslauncher/
├── GlassLauncherApp.kt          # Application, inisialisasi WidgetRepository
├── LauncherActivity.kt          # HOME activity utama
├── DesktopModeActivity.kt       # Desktop Mode + taskbar untuk layar lain
├── LauncherViewModel.kt         # State daftar aplikasi & widget
├── data/
│   ├── AppRepository.kt         # Query aplikasi ter-install
│   └── WidgetRepository.kt      # AppWidgetHost + persistensi widget & ukuran
└── ui/
    ├── theme/Theme.kt           # GlassTheme (dark + transparan)
    ├── glass/Glass.kt           # glassSurface, GlassPanel, GlassScrim, DesktopBackdrop
    ├── common/Components.kt     # AppIcon, ClockHeader, GlassIconButton
    └── home/                    # HomeScreen (widget + resize), SettingsSheet
```

## Lisensi

MIT — lihat [LICENSE](LICENSE).
