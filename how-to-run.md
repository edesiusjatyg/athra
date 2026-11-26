# Menjalankan ATHRA di lokal

1. Start Android Emulator API 37 dari Android Studio → Device Manager. Pilih build variant **debug**.
2. Jalankan dari root repo di terminal pertama:

   ```sh
   npx --yes firebase-tools@13.29.2 emulators:start --only auth --project demo-athra
   ```

   Tunggu **All emulators ready** dan biarkan terminal ini berjalan. Layanan Authentication harus tersedia pada port **9099**.

3. Di terminal kedua, jalankan dari root repo:

   ```sh
   adb reverse tcp:9099 tcp:9099
   ./gradlew :app:installDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1
   adb shell am force-stop id.ac.ub.athra
   adb shell monkey -p id.ac.ub.athra 1
   ```

4. Pilih **Buat akun**, isi nama, email uji, password minimal 6 karakter, dan konfirmasi password. Akun ini dibuat di Auth Emulator lokal.

## Tombol Run Android Studio

Build debug otomatis memakai Auth Emulator jika konfigurasi Firebase production belum lengkap. Default host `10.0.2.2` mengarah ke komputer host dari Android Emulator, sehingga tombol **Run** tetap bisa dipakai tanpa menyimpan flag Gradle tambahan.

Jika memakai host `127.0.0.1` lewat properti `athra.authEmulatorHost`, pertahankan `adb reverse tcp:9099 tcp:9099`. Build release selalu menonaktifkan emulator. Debug yang memakai Firebase production lengkap memilih production secara default; gunakan `-Pathra.authEmulator=false` untuk menonaktifkan emulator secara eksplisit.

## Jika pendaftaran gagal

- **Konfigurasi Firebase belum tersedia:** pasang ulang build debug dengan perintah langkah 3. Perbaikan konfigurasi membaca nilai BuildConfig saat runtime, sehingga pergantian flag tidak lagi memakai nilai kosong yang tertinggal dari build sebelumnya.
- **Network / timeout:** pastikan terminal Auth Emulator masih menampilkan layanan aktif pada port 9099; jalankan lagi `adb reverse` jika memakai host `127.0.0.1`.

Instalasi ulang dengan `installDebug` mempertahankan data aplikasi. Tidak perlu uninstall atau menghapus data.

## Build dengan RAM lebih kecil

Jika Android Studio dan Android Emulator sedang berjalan bersama, gunakan perintah berikut sebagai pengganti perintah Gradle pada langkah 3. Build memakai satu worker dan compiler Kotlin di proses yang sama, dengan heap 768 MB:

```sh
./gradlew :app:installDebug \
  -Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1 \
  -Pkotlin.compiler.execution.strategy=in-process \
  '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m -XX:ReservedCodeCacheSize=128m -XX:ActiveProcessorCount=2' \
  --max-workers=1 --no-parallel --no-daemon --no-watch-fs
```

Jalankan `adb reverse` sebelum build dan perintah pembuka aplikasi setelahnya seperti pada langkah 3. Jalankan tes emulator setelah proses build selesai.
