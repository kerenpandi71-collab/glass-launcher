# Glass Launcher 🧊

Launcher Android bertema **liquid glass** ala iOS, dibangun dengan Kotlin + Jetpack Compose.
Dirancang ringan untuk HP entry-level (diuji menargetkan Redmi 12C / Helio G85),
**tanpa laci aplikasi dan tanpa widget** — tampilan bersih: semua aplikasi di halaman homescreen.

## Fitur

- **Liquid glass UI** — panel kaca transparan dengan gradient dan border highlight, tampil di atas **wallpaper sistem asli** (tanpa blur bitmap berat → hemat RAM & baterai di HP kentang).
- **Tanpa app drawer** ala iPhone — semua aplikasi dibagi ke halaman (24 aplikasi per halaman) dengan indikator halaman; halaman tetangga di-pre-compose supaya geser halaman mulus di HP kentang.
- **Tanpa widget** — sengaja dihapus demi tampilan bersih dan performa.
- **Long-press ikon aplikasi** → menu glass: *Info aplikasi* dan *Uninstall*, seperti launcher pada umumnya.
- **Dock** ala iOS dengan 4 aplikasi.
- **Aplikasi clone / Dual Apps tampil** — daftar aplikasi memakai `LauncherApps` + `UserManager.userProfiles`, jadi aplikasi hasil clone (Dual Apps/Parallel, work profile) ikut muncul dengan ikon ber-badge, dan bisa diluncurkan di user-nya masing-masing.
- **Desktop Mode** — desktop ala PC dengan **taskbar** kaca di bawah (terinspirasi [farmerbb/Taskbar](https://github.com/farmerbb/Taskbar)): tombol semua-aplikasi, aplikasi pinned, dan peluncuran aplikasi freeform bila didukung. Di-render lewat **`Presentation`** (API resmi untuk display sekunder) sehingga lebih andal di display simulasi; fallback ke activity bila Presentation gagal. **Tidak aktif di display 0 (layar utama).**

## Optimasi untuk HP entry-level (Redmi 12C)

- Tidak ada blur full-screen — efek glass murni gradient + translucensi.
- Wallpaper tidak pernah dibuka sebagai bitmap — window launcher menampilkan wallpaper sistem langsung (overdraw minimal).
- Ikon aplikasi di-decode sekali ke bitmap 96px dan di-cache per aplikasi.
- Halaman pager di-pre-compose (`beyondViewportPageCount = 1`) supaya geser tidak kaku.

## Cara Build

1. Clone repo ini.
2. Buka di **Android Studio** (Koala/terbaru, JDK 17 bawaan), atau jalankan `./gradlew assembleDebug`.
3. APK debug juga otomatis di-build oleh GitHub Actions setiap push — unduh dari tab **Actions → Build → Artifacts**.

## Set sebagai Launcher Default

Setelah install: `Settings → Apps → Default apps → Home app → Glass Launcher`, atau tekan Home lalu pilih Glass Launcher.

## Mencoba Desktop Mode (tanpa monitor)

1. Aktifkan **Developer options** di HP.
2. Aktifkan **Force desktop mode on external displays** (Android 10+).
3. Buka **Developer options → Simulate secondary displays** → pilih misalnya *720p*.
4. Buka Glass Launcher → **Pengaturan → Desktop Mode** → pilih layar (bukan layar 0) → tekan **Buka**.
5. Desktop dengan taskbar tampil di display tersebut via Presentation; ketuk ikon untuk meluncurkan aplikasi.

## Batasan yang Diketahui

- Beberapa ROM (terutama MIUI) membatasi peluncuran activity pihak ketiga di display sekunder; jika aplikasi terbuka di layar utama, itu keterbatasan ROM, bukan bug launcher.
- Jendela freeform bergantung pada dukungan ROM (`FEATURE_FREEFORM_WINDOW_MANAGEMENT`); tanpa dukungan, aplikasi terbuka fullscreen di display tersebut.

## Struktur Proyek

```
app/src/main/java/com/kerenpandi/glasslauncher/
├── LauncherActivity.kt          # HOME activity utama + pengelola Desktop Mode
├── DesktopPresentation.kt       # Desktop Mode di layar kedua (Presentation API)
├── DesktopModeActivity.kt       # Fallback Desktop Mode via activity
├── LauncherViewModel.kt         # State daftar aplikasi
├── data/
│   └── AppRepository.kt         # LauncherApps: aplikasi semua profil + peluncuran
└── ui/
    ├── theme/Theme.kt           # GlassTheme (dark + transparan)
    ├── glass/Glass.kt           # glassSurface, GlassPanel, GlassScrim, DesktopBackdrop
    ├── common/Components.kt     # AppIcon (long-press menu), ClockHeader, GlassIconButton
    ├── desktop/DesktopUi.kt     # DesktopScreen, Taskbar, AllAppsPanel
    └── home/                    # HomeScreen, SettingsSheet
```

## Lisensi

MIT — lihat [LICENSE](LICENSE).
