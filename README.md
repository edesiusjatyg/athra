# ATHRA

ATHRA adalah aplikasi Android untuk mencatat sesi latihan dan melihat riwayat, volume, serta personal record. Akun memakai Firebase Authentication; profil, workout, PR, dan preferensi satuan disimpan lokal di perangkat berdasarkan UID Firebase. Data workout tidak tersinkron antarperangkat.

## Prasyarat

- Android Studio dan Android SDK yang menyediakan API 37 untuk build; perangkat minimum API 26.
- JDK 17 atau lebih baru untuk menjalankan Gradle/Android Gradle Plugin. Source Java aplikasi menargetkan bytecode 11; Kotlin saat ini menampilkan peringatan target JVM pada JDK 25.
- Firebase CLI hanya untuk Auth Emulator.

## Menjalankan

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK debug berada di `app/build/outputs/apk/debug/app-debug.apk`. Pasang melalui Android Studio atau:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Firebase Auth Emulator

Jalankan Auth Emulator pada komputer pengembangan:

```sh
firebase emulators:start --only auth --project demo-athra
```

Untuk Android Emulator yang memakai Firebase CLI dengan bind lokal, teruskan port emulator ke device dan build debug dengan host loopback:

```sh
adb reverse tcp:9099 tcp:9099
./gradlew :app:assembleDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1
```

Host default `10.0.2.2` dapat dipakai bila Auth Emulator menerima koneksi dari jaringan emulator. Untuk perangkat fisik, berikan alamat LAN komputer yang menjalankan emulator dan pastikan port 9099 dapat dijangkau:

```sh
./gradlew :app:assembleDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=192.168.1.10
```

Buat akun uji melalui layar Daftar aplikasi. Uji isolasi akun dengan membuat dua alamat email berbeda lalu keluar dan masuk ke akun lainnya. Jangan gunakan password produksi untuk emulator.

Untuk menyiapkan riwayat demo, build debug dengan `-Pathra.authEmulator=true`, masuk ke akun demo khusus, lalu buka **Profil → Demo Auth Emulator → Reset fixture H1/H2**. Aksi ini mengganti dua sesi fixture stabil untuk UID aktif dan mempertahankan sesi lain. Alat ini hanya tampil pada debug yang mengaktifkan Auth Emulator; implementasi release adalah no-op. Buat sesi ketiga lewat alur workout biasa: Bench Press 10×32 kg dan 10×30 kg, lalu Squat 10×40 kg; Finish dan Simpan. Untuk demo yang bersih, gunakan akun Auth Emulator khusus tanpa riwayat lain.

## Firebase project milik pemilik aplikasi

Aktifkan provider Email/Password pada Firebase Authentication. Berikan properti Gradle berikut melalui `~/.gradle/gradle.properties` atau argumen `-P` saat build:

```properties
athra.firebase.projectId=YOUR_PROJECT_ID
athra.firebase.appId=YOUR_ANDROID_APP_ID
athra.firebase.apiKey=YOUR_WEB_API_KEY
```

Jangan commit key atau file credential. Emulator tidak aktif pada build biasa; release selalu menonaktifkannya. Konfigurasi Firebase production belum disediakan di repository, sehingga production sign-in belum dapat diverifikasi.

## Offline dan penyimpanan

Setelah akun terautentikasi, katalog, profil lokal, sesi, set, riwayat, PR, analytics, dan preferensi satuan dibaca dari Room/DataStore di perangkat. Workout yang dicatat offline tetap lokal dan tidak disinkronkan ke Firebase atau perangkat lain. Firebase dipakai untuk identitas akun; jangan menghapus data aplikasi jika ingin mempertahankan workout lokal.

## Status verifikasi

Lihat [TODO.md](TODO.md) untuk item implementasi, [PRD.md](PRD.md) untuk acceptance criteria, dan [VERIFICATION.md](VERIFICATION.md) untuk hasil build/test serta status AC. Run emulator terakhir yang lengkap meluluskan 14/14 unit test dan 12/12 instrumented test. Source saat ini memiliki 23 instrumented test; seluruhnya berhasil dikompilasi, tetapi belum dieksekusi ulang karena tidak ada emulator terhubung. Build lokal terbaru meluluskan unit test, lint, dan debug assemble. Benchmark query/kalkulasi Room pada fixture 10.000 set: 147 ms di Medium_Phone AVD API 37. Screenshot runtime Home H1/H2/H3, Library, Summary, PR, referensi, Progress, dan validasi ada di `screenshots/runtime/`. Auth Emulator, alur H1/H2 → H3, simpan offline, dan pemulihan draft setelah force-stop telah dicoba manual; rotasi, pergantian akun, keyboard software pada font 200%, dan beberapa acceptance UI masih terbuka. Query analytics dan referensi memakai query Room terkelompok untuk menghindari pemuatan N+1.
