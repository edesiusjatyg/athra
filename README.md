# ATHRA

ATHRA adalah aplikasi Android untuk mencatat sesi latihan dan melihat riwayat, volume, serta personal record. Akun memakai Firebase Authentication; profil, workout, PR, dan preferensi satuan disimpan lokal di perangkat berdasarkan UID Firebase. Data workout tidak tersinkron antarperangkat.

Panduan singkat untuk menjalankan seluruh app ada di [how-to-run.md](how-to-run.md).

## Prasyarat

- Android Studio dengan Android SDK Platform 37, Android SDK Platform-Tools, dan Android Emulator. Perangkat minimum API 26.
- JDK 17 atau lebih baru. Gradle wrapper sudah ada di repository.
- Node.js dan npm untuk menjalankan Firebase CLI/Auth Emulator.

## Menjalankan seluruh app secara lokal

1. Buka folder repository ini di Android Studio. Tunggu Gradle Sync selesai.
2. Di **Device Manager**, buat atau jalankan Android Virtual Device dengan API 37. Emulator yang sedang dipakai untuk QA adalah Medium_Phone API 37.
3. Buka terminal pertama di root repository dan jalankan Auth Emulator:

   ```sh
   npx --yes firebase-tools@13.29.2 emulators:start --only auth --project demo-athra
   ```

   [firebase.json](firebase.json) mengonfigurasi Auth Emulator pada port 9099 dan menonaktifkan Emulator UI karena app hanya memerlukan Auth. Emulator memakai project demo lokal; tidak perlu Firebase project production atau credential.

4. Buka terminal kedua di root repository. Pastikan AVD terlihat oleh ADB, teruskan port Auth Emulator ke Android Emulator, lalu build dan install debug app dengan koneksi emulator diaktifkan:

   ```sh
   adb devices
   adb reverse tcp:9099 tcp:9099
   ./gradlew :app:installDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1
   adb shell monkey -p id.ac.ub.athra 1
   ```

   Perintah terakhir membuka ATHRA di AVD. Jika Android Studio meminta memilih device, pilih AVD yang sama. Untuk tombol **Run** Android Studio, debug otomatis memakai Auth Emulator ketika konfigurasi production belum lengkap, dengan default host `10.0.2.2`. Jika memakai properti host `127.0.0.1`, pertahankan `adb reverse`. Auth Emulator harus tetap berjalan.

5. Di ATHRA pilih **Daftar**, buat akun uji email/password, lalu masuk. Email dan password ini hanya hidup di Auth Emulator lokal; jangan pakai credential production.

6. Untuk demo, buka **Profil → Demo Auth Emulator → Reset fixture H1/H2**. Buat sesi H3 lewat UI: Bench Press 10×32 kg dan 10×30 kg, lalu Squat 10×40 kg; pilih **Selesai dan tinjau → Simpan sesi**. H3 lalu tampil di Home, Progress, History, detail PR, dan referensi latihan.

Auth Emulator adalah layanan lokal yang diperlukan untuk signup/sign-in demo. Tidak ada server backend ATHRA terpisah untuk dijalankan: profil, katalog, workout, PR, history, analytics, dan preferensi disimpan di Room/DataStore pada perangkat. Data workout tidak sinkron ke cloud atau perangkat lain. Biarkan terminal Auth Emulator tetap berjalan selama demo; jika akunnya hilang setelah emulator dihentikan, daftar ulang dengan akun uji.

Untuk melihat konfigurasi Emulator Suite dan koneksi Android resminya, lihat [Firebase Emulator Suite](https://firebase.google.com/docs/emulator-suite/install_and_configure) dan [Firebase Auth Emulator untuk Android](https://firebase.google.com/docs/emulator-suite/connect_auth).

### Build, test, dan pasang APK

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
./gradlew connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK debug berada di `app/build/outputs/apk/debug/app-debug.apk`. Pasang melalui Android Studio atau:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Firebase Auth Emulator

Langkah singkat menjalankan app ada di [how-to-run.md](how-to-run.md); detail konfigurasi ada di bagian [Menjalankan seluruh app secara lokal](#menjalankan-seluruh-app-secara-lokal). Auth Emulator aktif secara default pada build debug tanpa konfigurasi production lengkap dan dapat dipilih eksplisit lewat `athra.authEmulator`; Firebase SDK Android menggunakan `10.0.2.2` sebagai host emulator bila tidak memakai port reverse.

```sh
adb reverse tcp:9099 tcp:9099
./gradlew :app:installDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1
```

Untuk emulator Android, Auth SDK biasanya memakai host `10.0.2.2`; instruksi di atas memakai `adb reverse` sehingga app dapat memakai `127.0.0.1`. [Dokumentasi Firebase](https://firebase.google.com/docs/emulator-suite/connect_auth) menjelaskan koneksi Android dan default port 9099.

## Firebase project milik pemilik aplikasi

Aktifkan provider Email/Password pada Firebase Authentication. Berikan properti Gradle berikut melalui `~/.gradle/gradle.properties` atau argumen `-P` saat build:

```properties
athra.firebase.projectId=YOUR_PROJECT_ID
athra.firebase.appId=YOUR_ANDROID_APP_ID
athra.firebase.apiKey=YOUR_WEB_API_KEY
```

Jangan commit key atau file credential. Release selalu menonaktifkan emulator. Debug dengan konfigurasi production lengkap memilih production secara default; `-Pathra.authEmulator=false` menonaktifkan emulator secara eksplisit. Konfigurasi Firebase production belum disediakan di repository, sehingga production sign-in belum dapat diverifikasi.

## Offline dan penyimpanan

Setelah akun terautentikasi, katalog, profil lokal, sesi, set, riwayat, PR, analytics, dan preferensi satuan dibaca dari Room/DataStore di perangkat. Workout yang dicatat offline tetap lokal dan tidak disinkronkan ke Firebase atau perangkat lain. Firebase dipakai untuk identitas akun; jangan menghapus data aplikasi jika ingin mempertahankan workout lokal.

## Status verifikasi

Lihat [TODO.md](TODO.md) untuk item implementasi, [PRD.md](PRD.md) untuk acceptance criteria, dan [VERIFICATION.md](VERIFICATION.md) untuk hasil build/test serta status AC. Pada 23 November 2026, unit test, lint, build debug/release, dan `connectedDebugAndroidTest` (26/26) lulus. Demo manual Auth Emulator → reset H1/H2 → simpan H3 → cek Home/Progress/History/detail juga lulus; panduan jalan lokal ada di [how-to-run.md](how-to-run.md). Frame Figma `ATHRA UI UX/` menyediakan delapan layar utama dan delapan prototype/supporting screens untuk review lokal; perbandingan rinci semua layar masih berjalan. Benchmark query/kalkulasi Room pada fixture 10.000 set: 147 ms di Medium_Phone AVD API 37; ini bukan pengukuran frame time UI. Screenshot runtime Home H1/H2/H3, Library, Active Workout, Summary, PR, referensi, Progress, dan validasi ada di `screenshots/runtime/`. Pemulihan draft setelah force-stop, rotasi Library/Active, pergantian akun, serta font 200% pada Active/Summary sudah dicoba manual. Input dengan keyboard terbuka berhasil digunakan saat signup dan menyimpan set pada AVD; pengujian ukuran layar kecil, mode keyboard lain, audit target sentuh 48 dp, screen reader, dan beberapa skenario acceptance masih terbuka. Query analytics dan referensi memakai query Room terkelompok untuk menghindari pemuatan N+1.

## UI rebuild — 26 November 2026

Seluruh layar utama, termasuk Active Workout, dan layar pendukung sudah dibangun ulang mengikuti PDF Figma lokal. Perubahan mencakup font Inter dengan bobot sebenarnya, komponen/warna bersama, navigasi outline, susunan kartu, tabel set, chart, form, dan CTA. Review independen atas 15 render Compose normal-font menyatakan temuan visual material selesai. Lihat [bukti render dan cara reproduksi](screenshots/ui-rebuild/README.md) serta [sistem visual](DESIGN.md).

Render ini memakai Layoutlib, bukan emulator. Screenshot lama pada `screenshots/runtime/` mendahului rebuild. QA runtime setelah rebuild belum diulang; rotasi dan font 200% ditunda sesuai prioritas pengguna. Bahasa Indonesia, data akun nyata, dan batas fitur PRD tetap dipertahankan.

## Perbaikan Auth dan QA runtime terbaru

Konfigurasi Firebase kini dibaca saat runtime agar perubahan flag Gradle tidak memakai nilai kosong dari build lama. Debug tanpa konfigurasi production lengkap otomatis memakai Auth Emulator; release tetap menonaktifkannya. [Panduan menjalankan](how-to-run.md) mencakup koneksi emulator dan opsi build hemat RAM. Setelah perbaikan, 34/34 tes UI/Auth lulus pada emulator API 37, termasuk 15 screenshot runtime dan alur penyimpanan workout. Tombol History → Home juga diperbaiki. Lihat [laporan QA runtime](screenshots/runtime/rebuild-api37/README.md); rotasi dan font 200% tetap ditunda.
