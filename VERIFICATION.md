# Verifikasi ATHRA

Tanggal catatan: 16 November 2026.

## Perintah yang dijalankan

Cache Gradle/Android diarahkan ke `/tmp` karena workspace membatasi akses ke direktori Android default. Perintah verifikasi source saat ini:

```sh
XDG_DATA_HOME=/tmp/athra-xdg ANDROID_USER_HOME=/tmp/athra-android GRADLE_USER_HOME=/tmp/athra-gradle ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
XDG_DATA_HOME=/tmp/athra-xdg ANDROID_USER_HOME=/tmp/athra-android GRADLE_USER_HOME=/tmp/athra-gradle ./gradlew connectedDebugAndroidTest
```

Hasil source: **14/14 unit test** dan **25/25 instrumented test** lulus pada 14 November 2026. Instrumented dijalankan pada `emulator-5554`, Medium_Phone AVD, API 37, x86_64; tidak ada tes yang dilewati. Suite mencakup 8 tes Room, 1 constraint, 11 Compose smoke, 1 sesi penuh lintas route, dan 4 auth validation. Review/empty analytics dan draft setelah database dibuka ulang juga diuji. Migration belum diperlukan karena database masih schema versi pertama. Pada 16 November, debug assemble dan lint berhasil setelah refinement visual Home; suite instrumented belum diulang setelah perubahan UI itu.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Benchmark fixture 10.000 set mencatat **147 ms**, di bawah target 2 detik; angka ini hanya waktu query/kalkulasi repository Room, bukan frame time UI.

## Pemeriksaan revisi terbaru

Workout graph memakai route terpisah untuk Start, Quick Start, Library, Filters, Exercise Details, Active Workout, dan Summary. Tes navigasi penuh kini melewati sesi mulai, tambah gerakan, simpan set, review, finalisasi, History/Detail, Profile/Edit Profile, dan Preferences. Test menegaskan perpindahan navigasi setelah operasi Room yang suspend berjalan pada main thread. State mutation memakai lock tersinkron agar UI tidak mengaktifkan aksi sebelum operasi sebelumnya benar-benar selesai.

Page `ATHRA - Minimal Redesign` ber-ID `29:2` menurut pemeriksaan sebelumnya. Pada 16 November, context MCP menghasilkan “nothing selected”, metadata terkena batas Starter, dan URL publik memberi 403. User kemudian mengekspor frame ke `ATHRA UI UX/`: delapan layar utama serta delapan layar prototype/supporting tersedia sebagai PDF; `README.md` dan `contact-sheet.png` menginventarisasinya. Refinement Home mengadopsi logo sparkle, ikon profil, dekorasi ring, rentang tanggal minggu, dan ikon sesi dari frame lokal. Perbandingan layar lain dan screenshot render terbaru belum dilakukan.

APK dipasang dan diuji dengan Firebase Auth Emulator lokal (`demo-athra`, port 9099). Auth Emulator dapat diakses lewat `adb reverse tcp:9099 tcp:9099` dan debug host `127.0.0.1`. Pendaftaran akun dengan nama, sign in, reset fixture H1/H2, lalu pembuatan H3 lewat UI berhasil. Home menampilkan H3 terbaru; Progress berubah dari H1/H2 (2 sesi, 4 set, 38 repetisi, 1.190 kg) menjadi H1/H2/H3 (3 sesi, 7 set, 68 repetisi, 2.210 kg). Detail H3 menyimpan snapshot “Rekor baru” Bench Press 32 kg dan “Menyamai rekor” Squat 40 kg.

Mode Wi-Fi/data dimatikan setelah sign in; sesi offline berhasil disimpan dan tampil di Home. Force-stop saat draft aktif berisi satu set, lalu relaunch, memulihkan akun, set Bench Press 10×20 kg, dan timer. Pada font scale 200%, Home menumpuk judul/subjudul, workout dapat di-scroll, tombol Simpan set dapat dipakai, dan bottom navigation menampilkan ikon yang tetap dapat membuka Latihan, Progress, serta Profil. Soft keyboard Android tidak terbuka melalui otomasi ADB, jadi inset/CTA saat keyboard benar-benar terbuka belum dibuktikan. Rotasi dan pergantian akun belum diuji manual. Font scale emulator dikembalikan ke 100% setelah pengecekan.

Screenshot runtime: `screenshots/runtime/home-completed-api37.png`, `home-h3-api37.png`, `exercise-library-api37.png`, `workout-summary-api37.png`, `workout-detail-pr-h3-api37.png`, `exercise-reference-h3-api37.png`, `active-reference-h3-api37.png`, `progress-h3-api37.png`, dan `workout-validation-api37.png`.

Peringatan build yang masih muncul: `.android/analytics.settings` tidak dapat ditulis, opsi kompatibilitas KSP/AGP bersifat experimental, Kotlin pada JDK 25 fallback ke target JVM 24, dan ikon `ArrowBack` lama di dua file layar yang sudah tidak menjadi entry point utama. Konstruktor `Locale(String, String)` di Home diganti dengan `Locale.forLanguageTag` pada refinement 16 November.

## Status acceptance criteria

Status “implementasi ada” berarti alur/kode ditemukan dalam aplikasi; itu bukan klaim bahwa seluruh skenario telah diuji di perangkat. Status pengujian otomatis hanya dicantumkan untuk kasus yang memang tercakup dalam unit test atau integration test.

| AC | Status saat ini |
| --- | --- |
| AC01–05 | Sign up dengan nama, sign in, Home, dan pemulihan sesi Auth setelah force-stop berhasil dicoba manual. Retry setup, sign out, dan isolasi dua akun belum diuji manual end-to-end. |
| AC06 | Kalkulasi total dari set tersimpan diuji lewat unit/instrumented test dan demo H1/H2/H3: 7 set, 68 repetisi, 2.210 kg. |
| AC07–09 | Edit/hapus set dengan ID stabil dan renumber tersedia. Belum ada UI test. |
| AC10 | Beban 0 diterima dan dihitung sebagai set/repetisi; tercakup dalam unit test. |
| AC11–15 | Review menolak tanpa set, membekukan waktu selesai, menyediakan koreksi, dan menghitung PR; pada H3 UI menunjukkan PR baru Bench Press dan rekor Squat yang sama. Rotasi/process death saat REVIEW belum diuji. |
| AC16–17 | Finalisasi Room transaksional dan idempoten diimplementasikan; tes repository finalisasi dan idempotensi lulus pada emulator. |
| AC18–20 | PR baseline/baru/sama/di bawah dan identitas exercise stabil tersedia; status PR tercakup unit test. |
| AC21–23 | Referensi sesi terakhir, perbandingan nomor set, delta, dan kondisi tanpa pembanding tersedia; integration test memilih sesi terbaru lulus. Tampilan referensi H3 tersimpan di screenshot. |
| AC24 | Analytics fixture H1/H2/H3 menampilkan 3 sesi, 7 set, 68 repetisi, 2.210 kg; cocok dengan unit/instrumented test. |
| AC25 | Sesi REVIEW dikecualikan oleh query COMPLETED; tes repository review/empty analytics lulus. |
| AC26–27 | Favorit dihitung per sesi dan tie-break abjad; tercakup unit test. |
| AC28 | Boundary hari pertama 7/30/90 tercakup unit test; sesi tepat tengah malam sebelum batas dikeluarkan, sesi tepat pada awal hari dihitung. Sesi lintas tengah malam juga diuji untuk timer dan bucket berdasarkan hari selesai. |
| AC29 | Empty state data analytics tersedia; tes empty history memastikan belum ada data analytics. |
| AC30 | Sesi beban nol tetap dihitung dan tidak dianggap kosong; tercakup unit test. |
| AC31 | Konversi 10 lb ke 4.53592370 kg tercakup unit test. |
| AC32–33 | Preferensi unit disimpan per UID dan konversi tampilan tersedia; Compose test penyimpanan eksplisit lulus. Persistensi lintas relaunch dan isolasi dua UID belum diuji manual. |
| AC34 | Detail memakai snapshot PR final; integration test snapshot final lulus pada emulator. Screenshot detail H3 ada di `screenshots/runtime/workout-detail-pr-h3-api37.png`. |
| AC35–38 | Filter dan detail exercise dicoba manual; nama saat sign-up sekarang tersimpan sebagai nama profil. Pencegahan duplikat tersedia; Edit Profile dan isolasi preferensi per UID belum diuji manual. |
| AC39 | Sesi diselesaikan saat Wi-Fi dan data dimatikan setelah login; Home menampilkan sesi tersimpan lokal. |
| AC40 | Tes repository bahwa discard menghapus draft beserta child rows lulus pada emulator. |
| AC41 | Font scale 200% diuji manual: Home bertumpuk, layar workout dapat di-scroll, set tersimpan, CTA dapat dijangkau, dan bottom navigation tetap dapat dipakai dengan ikon ber-content description. Soft keyboard, inset IME, perangkat kecil, dan audit target 48 dp belum sepenuhnya diverifikasi. |
| AC42 | Benchmark fixture 10.000 set lulus dalam 147 ms pada Medium_Phone AVD, API 37, x86_64 (target <2 detik). Ini bukan pengukuran UI frame time. |

## Teori perkuliahan dan lokasi kode

1. **Activity dan lifecycle** — `MainActivity` menjadi host Compose tunggal, mengelola ViewModel, dan mengamati StateFlow dengan `collectAsStateWithLifecycle`.
2. **Navigation** — `ui/navigation/NavGraph.kt` mengatur tab utama dan route workout, history, profile, serta preferences.
3. **Coroutine / background task** — `WorkoutViewModel` menjalankan operasi database dan kalkulasi melalui `Dispatchers.IO` dan `viewModelScope`.
4. **State dan ViewModel** — `WorkoutViewModel` mengekspos `StateFlow`; UI Compose mengamati state dan mengirim aksi kembali ke ViewModel.
5. **Database** — `data/local/AppDatabase.kt`, `Entities.kt`, `Daos.kt`, dan `data/repository/WorkoutRepository.kt` menyimpan data workout di Room.

Kelima hal di atas menunjukkan struktur implementasi. Pemulihan Auth dan draft setelah force-stop serta penggunaan dasar pada font 200% sudah diverifikasi runtime. Rotasi, pemulihan state saat layar REVIEW, dan responsivitas pada perangkat kecil masih belum diverifikasi.
