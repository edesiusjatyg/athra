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

Jalankan emulator pada komputer pengembangan:

```sh
firebase emulators:start --only auth --project demo-athra
```

Untuk Android Emulator, host default `10.0.2.2` mengarah ke komputer host. Build debug mengaktifkan emulator secara eksplisit:

```sh
./gradlew :app:assembleDebug -Pathra.authEmulator=true
```

Untuk perangkat fisik, berikan alamat LAN komputer yang menjalankan emulator:

```sh
./gradlew :app:assembleDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=192.168.1.10
```

Buat akun uji melalui layar Daftar aplikasi. Uji isolasi akun dengan membuat dua alamat email berbeda lalu keluar dan masuk ke akun lainnya. Jangan gunakan password produksi untuk emulator.

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

Lihat [TODO.md](TODO.md) untuk item implementasi, [PRD.md](PRD.md) untuk acceptance criteria, dan [VERIFICATION.md](VERIFICATION.md) untuk hasil aktual build/test serta status AC. Instrumented test sudah dikompilasi, tetapi butuh emulator/perangkat untuk dijalankan. Screenshot runtime dan pengukuran analytics 10.000 set belum tersedia. Query analytics dan referensi memakai query Room terkelompok untuk menghindari pemuatan N+1.
