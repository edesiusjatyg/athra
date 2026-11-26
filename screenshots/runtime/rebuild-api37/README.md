# QA runtime dan perbaikan Firebase — 26 November 2026

Perangkat: Medium Phone Android Emulator API 37, 1080 × 2400, density 420, font scale **1.0**. Rotasi dan font 200% tidak diuji sesuai arahan pengguna.

## Hasil

**34/34 tes instrumentation lulus**, tanpa kegagalan atau tes dilewati, pada APK setelah perbaikan navigasi. Tes dijalankan langsung melalui AndroidJUnitRunner sesudah proses build selesai untuk membatasi beban RAM.

| Kelompok | Lulus |
| --- | ---: |
| AuthEmulatorIntegrationTest | 2 |
| FlowSmokeTest | 11 |
| WorkoutNavigationFlowTest | 2 |
| AuthValidationTest | 4 |
| FigmaScreenCaptureTest | 15 |

Tes Firebase memakai Auth Emulator lokal dan FirebaseApp bernama terpisah. Cakupannya meliputi konfigurasi aplikasi saat startup, pendaftaran, pembaruan nama, sign out, dan sign in. Sesi Firebase default milik pengguna tidak di-sign-out oleh pengujian. Tes navigasi memakai Room in-memory dan mencakup start → tambah gerakan → detail → simpan set → review → simpan sesi → History → detail → Home → Profile → Edit Profile → Preferences.

Pemeriksaan manual pada aplikasi utama mengonfirmasi History → **Kembali ke beranda** benar-benar kembali ke Home; tab Progress, Profile, Edit Profile, dan Preferences dapat dibuka lalu kembali. Nama, satuan, dan data workout pengguna tidak diubah saat pemeriksaan manual.

## Temuan yang diperbaiki

- Nilai Firebase yang sebelumnya di-inline sebagai string kosong tetap tertinggal saat flag build berubah. Field konfigurasi sekarang dibaca saat runtime. Debug tanpa konfigurasi production lengkap memakai Auth Emulator secara default; release selalu menonaktifkannya.
- `restoreState` pada navigasi Home sebelumnya membuka kembali History. Home sekarang tidak memulihkan stack yang baru saja ditinggalkan.
- Tes navigasi disesuaikan dengan header ganda Figma, tombol kembali dari History, dan kesiapan data asynchronous sebelum berinteraksi.

## Bukti visual

[Overview 15 layar](fixture-overview.png). PNG di [fixtures/](fixtures/) merupakan tangkapan produksi Compose yang dijalankan di emulator dengan **data fixture sintetis**, bukan data akun pengguna. Ini berbeda dari bukti Layoutlib pada `screenshots/ui-rebuild/`.

Build dilakukan berurutan dengan satu worker, compiler Kotlin in-process, dan heap 640–768 MB. Cache QA ditempatkan pada disk di `build/`, bukan tmpfs `/tmp`. Panduan perintah terbaru ada di [how-to-run.md](../../../how-to-run.md).
