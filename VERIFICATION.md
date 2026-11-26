# Verifikasi ATHRA

## Perbaikan Firebase dan QA runtime — 26 November 2026

Peringatan konfigurasi Firebase ditelusuri ke bytecode Kotlin lama yang masih meng-inline string kosong walaupun BuildConfig sudah berisi konfigurasi Auth Emulator. Field konfigurasi kini non-constant dan dibaca saat runtime; debug otomatis memakai emulator jika konfigurasi production belum lengkap, sedangkan release tetap menonaktifkannya.

**34/34 tes UI/Auth instrumentation lulus** di Android Emulator API 37, font 1.0, setelah perbaikan. Termasuk 2 integration test Firebase (startup, daftar, nama, sign out/sign in), 11 smoke, 2 navigasi workout, 4 validasi auth, dan 15 capture runtime. Unit test kalkulasi 14/14 juga lulus pada build perbaikan awal. Alur navigasi History → Home yang mengembalikan History akibat restoreState diperbaiki; pemeriksaan manual pada aplikasi utama mengonfirmasi tombol kembali, Progress, Profile, Edit Profile, dan Preferences. Sesi akun default dan data pengguna dipertahankan.

Lihat [laporan serta screenshot runtime terbaru](screenshots/runtime/rebuild-api37/README.md). Capture memakai fixture terpisah; bukan data akun pengguna. Rotasi/font 200% tetap ditunda. Pengujian setelah restart laptop memakai build satu worker, heap 640–768 MB, cache pada disk, dan eksekusi tes sesudah build selesai. Panduan menjalankan app telah diperbarui di [how-to-run.md](how-to-run.md).

## UI rebuild — 26 November 2026

Rebuild visual mencakup delapan layar utama dan enam supporting screens dari PDF lokal; Workout Detail memakai sistem visual yang sama. Font Inter kini memakai instance statis untuk bobot 400/500/600/700; seluruh slot typography dan color roles diatur. Home, Active Workout, Library, Summary, Progress, Profile, Auth, Pre-workout, dan supporting flows dicocokkan dengan render PDF.

**Hasil terbaru:** `assembleDebug`, `assembleRelease`, `testDebugUnitTest` (**14/14**), `compileDebugAndroidTestKotlin`, `lintDebug`, `updateDebugScreenshotTest`, dan `validateDebugScreenshotTest` lulus. Satu lint run awal terhenti saat source tes masih diedit; run akhir dengan source stabil lulus.

15 screenshot fixture dirender memakai native Compose Layoutlib pada normal font scale. Reviewer independen **GPT-6.1 Sol high** memberi verdict **ship untuk lingkup visual normal-font** dan menilai seluruh temuan material sebelumnya selesai. Lihat [`screenshots/ui-rebuild/README.md`](screenshots/ui-rebuild/README.md), overview, dan baseline PNG untuk bukti serta cara reproduksi.

Ini bukan hasil runtime emulator/perangkat: lingkungan tidak menyediakan `/dev/kvm`, dan percobaan emulator software berhenti dengan segmentation fault. `connectedDebugAndroidTest` setelah rebuild belum dijalankan; kompilasi tesnya lulus. Screenshot `screenshots/runtime/` dan hasil 26/26 di bagian historis berikut berasal dari sebelum rebuild. System insets, keyboard, gesture, dan navigasi runtime setelah rebuild belum diverifikasi ulang. QA rotasi dan font 200% secara eksplisit ditunda oleh pengguna. Review visual menyatakan kecocokan komposisi pada fixture yang ditinjau, bukan identitas piksel atau perluasan fitur di luar PRD.

## Catatan historis sebelum rebuild

Tanggal catatan: 23 November 2026.

## Perintah yang dijalankan

Cache Gradle/Android diarahkan ke `/tmp` karena workspace membatasi akses ke direktori Android default. Perintah verifikasi source saat ini:

```sh
XDG_DATA_HOME=/tmp/athra-xdg ANDROID_USER_HOME=/tmp/athra-android GRADLE_USER_HOME=/tmp/athra-gradle ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
XDG_DATA_HOME=/tmp/athra-xdg ANDROID_USER_HOME=/tmp/athra-android GRADLE_USER_HOME=/tmp/athra-gradle ./gradlew connectedDebugAndroidTest
```

Hasil source pada 23 November 2026: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, dan `assembleRelease` lulus. `connectedDebugAndroidTest` lulus **26/26**, 0 gagal, 0 dilewati, pada `emulator-5554`, Medium_Phone AVD, API 37. Suite mencakup 8 tes Room (termasuk rollback saat insert snapshot gagal), 1 constraint, 11 Compose smoke, 2 tes navigasi workout, dan 4 auth validation. Migration belum diperlukan karena database masih schema versi pertama.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Benchmark fixture 10.000 set mencatat **147 ms**, di bawah target 2 detik; angka ini hanya waktu query/kalkulasi repository Room, bukan frame time UI.

## Pemeriksaan revisi terbaru

Workout graph memakai route terpisah untuk Start, Quick Start, Library, Filters, Exercise Details, Active Workout, dan Summary. Tes navigasi penuh kini melewati sesi mulai, tambah gerakan, simpan set, review, finalisasi, History/Detail, Profile/Edit Profile, dan Preferences. Test menegaskan perpindahan navigasi setelah operasi Room yang suspend berjalan pada main thread. State mutation memakai lock tersinkron agar UI tidak mengaktifkan aksi sebelum operasi sebelumnya benar-benar selesai.

Page `ATHRA - Minimal Redesign` ber-ID `29:2` menurut pemeriksaan sebelumnya. Pada 16 November, context MCP menghasilkan “nothing selected”, metadata terkena batas Starter, dan URL publik memberi 403. User mengekspor frame ke `ATHRA UI UX/`: delapan layar utama serta delapan layar prototype/supporting tersedia sebagai PDF. Home, Active Workout, dan Summary dibandingkan dengan frame lokal; Active Workout kini memakai timer/set card, exercise cards, bagian berikutnya, serta Finish CTA pinned. Summary memakai completion hero, kartu sesi, metrik aktual, dan highlight pergerakan. Library kini menampilkan kartu gerakan dan quick-add; tap `+` Bench Press secara manual menambahkan gerakan dan membuka Workout aktif. Screenshot runtime disimpan pada `screenshots/runtime/`. Login, Pre-workout, Progress, Profile, dan layar supporting masih memerlukan review visual terperinci; keseluruhan belum diklaim pixel-identik.

APK dipasang dan diuji dengan Firebase Auth Emulator lokal (`demo-athra`, port 9099). Auth Emulator dapat diakses lewat `adb reverse tcp:9099 tcp:9099` dan debug host `127.0.0.1`. Pada smoke demo 23 Nov, pendaftaran akun, reset H1/H2, pembuatan dan penyimpanan H3 lewat UI berhasil. H3 berisi Bench Press 10×32 kg dan 10×30 kg serta Squat 10×40 kg; Summary melaporkan 3 set dan 1.020 kg. Home menampilkan H3 terbaru; Progress dan History berisi H1/H2/H3 dengan total 3 sesi, 7 set, 68 repetisi, 2.210 kg. Detail H3 menyimpan snapshot “Rekor baru” Bench Press 32 kg dan “Menyamai rekor” Squat 40 kg. Signup dan penyimpanan set berhasil saat keyboard AVD terbuka; variasi posisi keyboard dan ukuran layar lain masih perlu diperiksa.

Auth Emulator berjalan lokal di `demo-athra:9099`, diakses lewat `adb reverse tcp:9099 tcp:9099`; build debug memakai `-Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1`. Home, Library, Active Workout, Summary, dan detail History dicoba manual; quick-add Bench Press dari Library membuka Active Workout dengan gerakan itu terpilih. Pada font scale 200%, Active Workout dapat di-scroll, Summary metrics tersusun vertikal, dan CTA pinned tetap terlihat. Rotasi Library mempertahankan query; rotasi Active mempertahankan 1 set/12 repetisi. Background/foreground mempertahankan set Bench Press 30 kg × 12, volume 360 kg, dan timer yang terus maju. Force-stop saat ACTIVE memulihkan draft/set/timer; force-stop saat REVIEW mengembalikan Home dengan draft lalu melanjutkan ke Summary yang sama (durasi 21 menit, set 30 kg × 12). Pergantian manual akun `demo@athra.test` → `switch@athra.test` memperlihatkan Home kosong pada akun kedua; kembali ke akun pertama menampilkan fixture H1/H2 lagi. Pada smoke 23 Nov, tombol Daftar dan Simpan set terlihat serta dapat dipakai saat keyboard AVD terbuka. Posisi keyboard lain, perangkat kecil, audit target sentuh 48 dp, screen reader, rotasi REVIEW, pergantian UID saat operasi berjalan, dan retry manual masih belum diverifikasi.

Screenshot runtime: `screenshots/runtime/home-completed-api37.png`, `home-h3-api37.png`, `home-redesign-api37.png`, `exercise-library-api37.png`, `exercise-library-quick-add-api37.png`, `exercise-library-quick-add-result-api37.png`, `active-workout-redesign-api37.png`, `active-workout-resume-api37.png`, `active-workout-force-stop-recovery-api37.png`, `active-workout-ime-floating-api37.png`, `active-workout-ime-bottom-api37.png`, `workout-summary-redesign-api37.png`, `active-workout-font200-api37.png`, `workout-summary-font200-api37.png`, `workout-summary-api37.png`, `workout-review-force-stop-recovery-api37.png`, `workout-detail-pr-h3-api37.png`, `exercise-reference-h3-api37.png`, `active-reference-h3-api37.png`, `progress-h3-api37.png`, dan `workout-validation-api37.png`.

Peringatan build pada sesi ini: Gradle tidak dapat menulis `/home/fast9/.android/analytics.settings` karena file system read-only; lint/build tetap lulus. Konstruktor `Locale(String, String)` di Home diganti dengan `Locale.forLanguageTag` pada refinement 16 November.

## Status acceptance criteria

Status “implementasi ada” berarti alur/kode ditemukan dalam aplikasi; itu bukan klaim bahwa seluruh skenario telah diuji di perangkat. Status pengujian otomatis hanya dicantumkan untuk kasus yang memang tercakup dalam unit test atau integration test.

| AC | Status saat ini |
| --- | --- |
| AC01–05 | Sign up/sign in/Auth restore telah diuji sebelumnya; sign out dan pergantian akun demo sekarang diuji manual. Akun kedua mulai dengan Home kosong, dan akun pertama kembali menampilkan fixture H1/H2. Retry setup belum diuji manual. |
| AC06 | Kalkulasi total dari set tersimpan diuji lewat unit/instrumented test dan demo H1/H2/H3: 7 set, 68 repetisi, 2.210 kg. |
| AC07–09 | Edit/hapus set dengan ID stabil dan renumber tersedia. Belum ada UI test. |
| AC10 | Beban 0 diterima dan dihitung sebagai set/repetisi; tercakup dalam unit test. |
| AC11–15 | Review menolak tanpa set, membekukan waktu selesai, menyediakan koreksi, dan menghitung PR; pada H3 UI menunjukkan PR baru Bench Press dan rekor Squat yang sama. Force-stop saat REVIEW lalu lanjut dari Home membuka Summary dengan set dan durasi yang sama; rotasi saat REVIEW belum diuji. |
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
| AC41 | Font scale 200% diuji manual pada Active Workout dan Summary: konten dapat di-scroll dan pinned CTA tetap terlihat; metrik Summary menjadi vertikal. Pada smoke 23 Nov, Daftar dan Simpan set dapat dipakai saat keyboard AVD terbuka. Variasi posisi keyboard, perangkat kecil, font scale layar lain, screen reader, dan audit target 48 dp masih terbuka. |
| AC42 | Benchmark fixture 10.000 set lulus dalam 147 ms pada Medium_Phone AVD, API 37, x86_64 (target <2 detik). Ini bukan pengukuran UI frame time. |

## Teori perkuliahan dan lokasi kode

1. **Activity dan lifecycle** — `MainActivity` menjadi host Compose tunggal, mengelola ViewModel, dan mengamati StateFlow dengan `collectAsStateWithLifecycle`.
2. **Navigation** — `ui/navigation/NavGraph.kt` mengatur tab utama dan route workout, history, profile, serta preferences.
3. **Coroutine / background task** — `WorkoutViewModel` menjalankan operasi database dan kalkulasi melalui `Dispatchers.IO` dan `viewModelScope`.
4. **State dan ViewModel** — `WorkoutViewModel` mengekspos `StateFlow`; UI Compose mengamati state dan mengirim aksi kembali ke ViewModel.
5. **Database** — `data/local/AppDatabase.kt`, `Entities.kt`, `Daos.kt`, dan `data/repository/WorkoutRepository.kt` menyimpan data workout di Room.

Kelima hal di atas menunjukkan struktur implementasi. Pemulihan Auth/draft setelah force-stop (ACTIVE dan REVIEW), rotasi Library/Active, isolasi dua akun, serta penggunaan font 200% pada Active/Summary sudah diverifikasi runtime. Rotasi saat REVIEW, retry manual, pergantian UID saat operasi berlangsung, variasi keyboard, audit aksesibilitas, dan perangkat kecil belum diverifikasi.
