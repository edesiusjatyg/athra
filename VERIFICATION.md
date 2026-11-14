# Verifikasi ATHRA

Tanggal catatan: 14 November 2026.

## Perintah yang dijalankan

Perintah dijalankan dengan cache Gradle/Android diarahkan ke `/tmp` dan compiler Kotlin in-process karena workspace membatasi akses ke direktori Android default:

```sh
GRADLE_USER_HOME=/tmp/athra-gradle ANDROID_USER_HOME=/tmp/athra-android ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain -Pkotlin.compiler.execution.strategy=in-process
```

Hasil terakhir: **BUILD SUCCESSFUL**. Tiga belas unit test domain lulus, lint lulus pada pemeriksaan terakhir, APK debug berhasil dibuat, dan sembilan test Android (empat integration Room + lima Compose smoke test) berhasil dikompilasi. Lint melaporkan warning dependency versi lebih baru, satu saran modifier Compose, label Activity redundan, serta resource template yang tidak dipakai; tidak ada lint error.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Instrumented test dijalankan melalui `:app:connectedDebugAndroidTest`, tetapi task berhenti dengan `DeviceException: No connected devices!`. `adb devices -l` juga tidak menampilkan emulator atau perangkat. AVD terpasang tidak dapat diboot karena snapshot lock lama; AVD sementara juga tidak dapat membuat data partition, membutuhkan 7.37 GB sementara ruang `/tmp` yang tersedia 3.4 GB. Karena itu kesembilan test Android belum dieksekusi.

Peringatan build yang masih muncul: `.android/analytics.settings` tidak dapat ditulis, opsi kompatibilitas KSP/AGP bersifat experimental, Kotlin pada JDK 25 fallback ke target JVM 24, beberapa konstruktor `Locale(String, String)` deprecated, dan ikon `ArrowBack` lama di dua file layar yang sudah tidak menjadi entry point utama.

## Status acceptance criteria

Status “implementasi ada” berarti alur/kode ditemukan dalam aplikasi; itu bukan klaim bahwa seluruh skenario telah diuji di perangkat. Status pengujian otomatis hanya dicantumkan untuk kasus yang memang tercakup dalam unit test atau integration test.

| AC | Status saat ini |
| --- | --- |
| AC01–05 | Alur Firebase Sign Up/Sign In, restore session, profil lokal, retry setup, sign out, dan UID scoping tersedia. Perlu uji Auth Emulator dua akun di perangkat. |
| AC06 | Kalkulasi total dari set tersimpan tersedia; unit test memverifikasi total set/repetisi/volume fixture. |
| AC07–09 | Edit/hapus set dengan ID stabil dan renumber tersedia. Belum ada UI test. |
| AC10 | Beban 0 diterima dan dihitung sebagai set/repetisi; tercakup dalam unit test. |
| AC11–15 | Review menolak tanpa set, membekukan waktu selesai, menyediakan koreksi, dan menghitung ulang PR. Perlu uji UI/rotasi/proses mati. |
| AC16–17 | Finalisasi Room transaksional dan idempoten diimplementasikan; ada integration test, belum dieksekusi tanpa perangkat. |
| AC18–20 | PR baseline/baru/sama/di bawah dan identitas exercise stabil tersedia; status PR tercakup unit test. |
| AC21–23 | Referensi sesi terakhir, perbandingan nomor set, delta, dan kondisi tanpa pembanding tersedia; integration test referensi dibuat tetapi belum dijalankan. |
| AC24 | Analytics fixture PRD tercakup unit test: 3 sesi, 7 set, 68 repetisi, 2.210 kg. |
| AC25 | Sesi REVIEW dikecualikan oleh query COMPLETED; belum ada test spesifik REVIEW. |
| AC26–27 | Favorit dihitung per sesi dan tie-break abjad; tercakup unit test. |
| AC28 | Batas hari pertama 7 hari tercakup unit test. Boundary serupa untuk 30/90 hari belum diuji. |
| AC29 | Empty state data analytics tersedia; belum ada test khusus empty fixture. |
| AC30 | Sesi beban nol tetap dihitung dan tidak dianggap kosong; tercakup unit test. |
| AC31 | Konversi 10 lb ke 4.53592370 kg tercakup unit test. |
| AC32–33 | Preferensi satuan disimpan per UID dan volume dikonversi di Active/Summary/History/Home/Progress; input set yang tidak berubah saat edit mempertahankan nilai kg aslinya. Belum ada test UI/persistensi relaunch. |
| AC34 | Detail memakai snapshot PR final. Integration test snapshot dibuat, belum dieksekusi. |
| AC35–38 | Filter/detail, pencegahan duplikat exercise, edit profil, dan preferensi per UID tersedia; perlu uji UI dan relaunch. |
| AC39 | Workout tersimpan lokal setelah akun disiapkan; kondisi offline belum diuji pada perangkat. |
| AC40 | Dialog konfirmasi discard dan transaksi hapus tersedia; integration test dibuat, belum dijalankan. |
| AC41 | Layout utama dapat discroll, tetapi keyboard layar kecil dan font 200% belum diverifikasi. |
| AC42 | Belum ada fixture 10.000 set atau pengukuran waktu analytics. |

## Teori perkuliahan dan lokasi kode

1. **Activity dan lifecycle** — `MainActivity` menjadi host Compose tunggal dan mengelola ViewModel.
2. **Navigation** — `ui/navigation/NavGraph.kt` mengatur tab utama dan route workout, history, profile, serta preferences.
3. **Coroutine / background task** — `WorkoutViewModel` menjalankan operasi database dan kalkulasi melalui `Dispatchers.IO` dan `viewModelScope`.
4. **State dan ViewModel** — `WorkoutViewModel` mengekspos `StateFlow`; UI Compose mengamati state dan mengirim aksi kembali ke ViewModel.
5. **Database** — `data/local/AppDatabase.kt`, `Entities.kt`, `Daos.kt`, dan `data/repository/WorkoutRepository.kt` menyimpan data workout di Room.

Kelima hal di atas menunjukkan struktur implementasi. Klaim runtime untuk lifecycle/rotasi, state recovery, dan responsivitas masih menunggu instrumented test pada perangkat.
