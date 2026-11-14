# Verifikasi ATHRA

Tanggal catatan: 14 November 2026.

## Perintah yang dijalankan

Perintah dijalankan dengan cache Gradle/Android diarahkan ke `/tmp` dan compiler Kotlin in-process karena workspace membatasi akses ke direktori Android default:

```sh
GRADLE_USER_HOME=/tmp/athra-gradle ANDROID_USER_HOME=/tmp/athra-android ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain -Pkotlin.compiler.execution.strategy=in-process
```

Hasil build lokal terakhir (14 November 2026): **BUILD SUCCESSFUL** untuk 14 unit test, lint, debug/release assemble, dan kompilasi instrumented test. ViewModel workout kini dimiliki oleh NavBackStackEntry graph dan tetap hidup saat berpindah antar destination di graph. Reset H1/H2 ditambahkan sebagai alat debug yang hanya tampil saat Auth Emulator diaktifkan; release menyediakan implementasi no-op. Input workout memilih keyboard angka/desimal, menangani aksi IME, dan menambahkan inset keyboard ke area scroll. Pengamatan StateFlow memakai `collectAsStateWithLifecycle`. Lint melaporkan warning dependency versi lebih baru, saran modifier Compose, label Activity redundan, serta resource template yang tidak dipakai; tidak ada lint error.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Run instrumented terakhir sebelum tes fixture ditambahkan (14 November 2026) menunjukkan **10/10 lulus**: empat tes repository Room, satu tes constraint, dan lima Compose smoke test. Perangkat: `emulator-5554`, Medium_Phone AVD, API 37, x86_64. Suite saat ini menambahkan satu tes Room H1/H2/H3; `:app:compileDebugAndroidTestKotlin` lulus, tetapi tes baru belum dijalankan karena `adb devices -l` kosong. Benchmark fixture 10.000 set mencatat **148 ms**, di bawah target 2 detik; angka ini adalah waktu query/kalkulasi repository Room, bukan pengukuran frame time atau respons UI end-to-end.

APK juga dipasang dan alur runtime dicoba dengan Firebase Auth Emulator lokal (`demo-athra`, port 9099). Sign in, Home, mulai workout, cari dan pilih Bench Press, simpan 1 set (10 repetisi × 32 kg), Finish, Summary, lalu kembali ke Home dengan satu sesi tersimpan berhasil. Pada lingkungan ini emulator aplikasi diarahkan ke host lokal memakai `adb reverse tcp:9099 tcp:9099` dan host debug `127.0.0.1`. Kondisi offline, rotasi, pemulihan process death, sign up penuh, dan perpindahan antar dua akun belum diverifikasi manual.

Screenshot runtime yang tersedia: `screenshots/runtime/home-completed-api37.png`, `screenshots/runtime/exercise-library-api37.png`, dan `screenshots/runtime/workout-summary-api37.png`. Belum ada bukti gambar untuk History/PR, referensi progressive overload, Analytics, atau validasi input.

Peringatan build yang masih muncul: `.android/analytics.settings` tidak dapat ditulis, opsi kompatibilitas KSP/AGP bersifat experimental, Kotlin pada JDK 25 fallback ke target JVM 24, beberapa konstruktor `Locale(String, String)` deprecated, dan ikon `ArrowBack` lama di dua file layar yang sudah tidak menjadi entry point utama.

## Status acceptance criteria

Status “implementasi ada” berarti alur/kode ditemukan dalam aplikasi; itu bukan klaim bahwa seluruh skenario telah diuji di perangkat. Status pengujian otomatis hanya dicantumkan untuk kasus yang memang tercakup dalam unit test atau integration test.

| AC | Status saat ini |
| --- | --- |
| AC01–05 | Sign in Firebase Auth Emulator dan masuk ke Home berhasil dicoba manual. Sign up penuh, restore session, retry setup, sign out, serta isolasi dua akun belum diuji manual end-to-end. |
| AC06 | Kalkulasi total dari set tersimpan tersedia; unit test memverifikasi total set/repetisi/volume fixture. |
| AC07–09 | Edit/hapus set dengan ID stabil dan renumber tersedia. Belum ada UI test. |
| AC10 | Beban 0 diterima dan dihitung sebagai set/repetisi; tercakup dalam unit test. |
| AC11–15 | Review menolak tanpa set, membekukan waktu selesai, menyediakan koreksi, dan menghitung ulang PR. Perlu uji UI/rotasi/proses mati. |
| AC16–17 | Finalisasi Room transaksional dan idempoten diimplementasikan; tes repository finalisasi dan idempotensi lulus pada emulator. |
| AC18–20 | PR baseline/baru/sama/di bawah dan identitas exercise stabil tersedia; status PR tercakup unit test. |
| AC21–23 | Referensi sesi terakhir, perbandingan nomor set, delta, dan kondisi tanpa pembanding tersedia; integration test bahwa referensi memilih sesi terbaru lulus. Tampilan visual referensi belum ditangkap. |
| AC24 | Analytics fixture PRD tercakup unit test: 3 sesi, 7 set, 68 repetisi, 2.210 kg. |
| AC25 | Sesi REVIEW dikecualikan oleh query COMPLETED; belum ada test spesifik REVIEW. |
| AC26–27 | Favorit dihitung per sesi dan tie-break abjad; tercakup unit test. |
| AC28 | Boundary hari pertama 7/30/90 tercakup unit test; sesi tepat tengah malam sebelum batas dikeluarkan, sesi tepat pada awal hari dihitung. Sesi lintas tengah malam juga diuji untuk timer dan bucket berdasarkan hari selesai. |
| AC29 | Empty state data analytics tersedia; belum ada test khusus empty fixture. |
| AC30 | Sesi beban nol tetap dihitung dan tidak dianggap kosong; tercakup unit test. |
| AC31 | Konversi 10 lb ke 4.53592370 kg tercakup unit test. |
| AC32–33 | Preferensi unit disimpan per UID dan konversi tampilan tersedia; Compose test penyimpanan eksplisit lulus. Persistensi lintas relaunch dan isolasi dua UID belum diuji manual. |
| AC34 | Detail memakai snapshot PR final; integration test snapshot final lulus pada emulator. Tampilan detail belum dicapture. |
| AC35–38 | Filter dan detail exercise dicoba manual. Pencegahan duplikat ada di kode; Edit Profile dan isolasi preferensi per UID belum diuji manual. |
| AC39 | Workout tersimpan lokal setelah akun disiapkan dan alur online dicoba; mode offline belum diuji. |
| AC40 | Tes repository bahwa discard menghapus draft beserta child rows lulus pada emulator. |
| AC41 | Layout dapat discroll; keyboard numerik/desimal dan inset IME dipakai saat input set di API 37. Ukuran layar kecil, touch target, dan font 200% belum diverifikasi. |
| AC42 | Benchmark fixture 10.000 set lulus dalam 148 ms pada Medium_Phone AVD, API 37, x86_64 (target <2 detik). Ini bukan pengukuran UI frame time. |

## Teori perkuliahan dan lokasi kode

1. **Activity dan lifecycle** — `MainActivity` menjadi host Compose tunggal, mengelola ViewModel, dan mengamati StateFlow dengan `collectAsStateWithLifecycle`.
2. **Navigation** — `ui/navigation/NavGraph.kt` mengatur tab utama dan route workout, history, profile, serta preferences.
3. **Coroutine / background task** — `WorkoutViewModel` menjalankan operasi database dan kalkulasi melalui `Dispatchers.IO` dan `viewModelScope`.
4. **State dan ViewModel** — `WorkoutViewModel` mengekspos `StateFlow`; UI Compose mengamati state dan mengirim aksi kembali ke ViewModel.
5. **Database** — `data/local/AppDatabase.kt`, `Entities.kt`, `Daos.kt`, dan `data/repository/WorkoutRepository.kt` menyimpan data workout di Room.

Kelima hal di atas menunjukkan struktur implementasi. Rotasi, process death, recovery state, dan responsivitas UI belum diverifikasi runtime.
