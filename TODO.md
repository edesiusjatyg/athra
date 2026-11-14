# ATHRA — TODO Implementasi

Checklist ini diturunkan dari PRD v1.0 (14 November 2026). Urutan mengikuti dependensi implementasi; keputusan produk dan acceptance criteria tetap merujuk ke PRD.md.

## Hasil audit awal

Audit dilakukan lewat inspeksi statis repo. Build dan tes belum dijalankan.

### Yang sudah tersedia

- Kerangka aplikasi Android satu Activity dengan Kotlin, Jetpack Compose, Material 3, dan Navigation Compose.
- Tema gelap, font Inter, komponen UI bersama, serta mockup Home, Start Workout, dan Active Workout.
- Gradle wrapper dan version catalog dengan versi dependency yang dipatok.

### Kesenjangan utama

- Navigasi dimulai langsung di Home; layar Sign In/Sign Up dan alur autentikasi belum ada.
- Belum ada Room, DataStore, Firebase Authentication, katalog exercise, repository, domain model, atau ViewModel.
- Tiga layar yang ada memakai data contoh. Timer, statistik, set, dan sesi tidak berubah atau tersimpan.
- Tombol tab bawah, View all, Quick start, kartu suggested workout, sesi terbaru, dan Up next belum menjalankan aksi yang sesuai.
- Library, Filters, Exercise Details, Summary, Analytics, Profile, Preferences, History, dan Edit Profile belum dibuat.
- UI masih berbahasa Inggris; ada tanggal/nama/angka contoh dan label workout statis.
- applicationId dan namespace saat ini com.example.athra, sedangkan PRD menetapkan id.ac.ub.athra. minSdk saat ini 24, sedangkan PRD menetapkan API 26.
- Tes yang ada hanya tes template Android/JUnit. Belum ada README, acceptance test, bukti runtime, atau APK terverifikasi.

## 1. Fondasi dan konfigurasi

- [x] Ubah applicationId ke id.ac.ub.athra; selaraskan namespace, package Kotlin, dan referensi test tanpa merusak launcher.
- [x] Tetapkan minSdk 26. Verifikasi compileSdk, targetSdk, AGP, Kotlin, JDK, Compose, Navigation, Room, Firebase, dan DataStore saling kompatibel; pin semua versi. Debug assemble berhasil; KSP masih memakai opsi kompatibilitas AGP yang experimental.
- [x] Tambahkan dependency yang diperlukan melalui version catalog; hindari versi dinamis.
- [x] Tambahkan konfigurasi Firebase Authentication Email/Password dan Auth Emulator yang hanya aktif di debug dengan flag eksplisit.
- [x] Tambahkan INTERNET permission hanya untuk autentikasi; pisahkan konfigurasi debug dan release, tanpa fake login atau seed history di release.
- [ ] Siapkan struktur UI/data/domain sederhana, AppContainer dan factory ViewModel; pertahankan single Activity.
- [ ] Buat README awal berisi setup Android SDK/JDK, Firebase/Auth Emulator, build, test, pemasangan APK, operasi offline, dan batas penyimpanan lokal.

## 2. Model domain, penyimpanan, dan katalog

- [x] Definisikan model sesi, exercise, set, status sesi, volume, PR, referensi, dan analytics; model profil serta snapshot database menyusul bersama repository.
- [x] Implementasikan Room entities, DAO, foreign key, index, constraint, schema export, dan migrasi yang aman. Database versi awal siap; migration untuk versi berikutnya belum diperlukan.
- [x] Implementasikan TypeConverter desimal kanonik menggunakan BigDecimal/string; jangan memakai Float/Double untuk logika beban, volume, PR, atau delta.
- [ ] Terapkan repository dengan pemeriksaan UID dan kepemilikan parent pada semua akses data pribadi.
- [ ] Terapkan satu slot draft per UID, maksimal satu sesi ACTIVE/REVIEW per akun, dan operasi atomik untuk perubahan set, finalisasi, serta discard.
- [x] Tambahkan DataStore dengan preferensi unit terpisah per Firebase UID; akun baru memakai kg.
- [x] Buat seed katalog lokal idempoten berisi 12 exercise pada PRD, ID stabil, kelompok otot, alat, dan petunjuk singkat.
- [ ] Pastikan draft/history tetap tersedia offline setelah sesi Firebase tersimpan; dokumentasikan bahwa data workout lokal tidak tersinkron antarperangkat.

## 3. Autentikasi dan isolasi akun

- [ ] Buat Auth navigation graph dan layar Sign In serta Sign Up dengan validasi, loading, error, dan retry.
- [ ] Implementasikan daftar email/password Firebase: trim nama/email, validasi email, password minimal 6 karakter, konfirmasi cocok, cegah submit ganda.
- [ ] Implementasikan login, restore sesi saat cold start, sign out, dan pembersihan state/back stack sebelum berganti akun.
- [ ] Pastikan profil lokal dibuat idempoten berdasarkan UID; tangani retry profil jika akun Firebase berhasil dibuat tetapi penyimpanan profil gagal.
- [ ] Pastikan operasi akun lama yang terlambat selesai tidak dapat mengubah UI akun baru; hentikan Flow lama saat UID berganti.
- [ ] Pastikan tidak ada password/token di Room, DataStore, SavedStateHandle, atau log.

## 4. Navigasi dan fondasi state UI

- [ ] Buat Main graph dengan root Home, Train, Progress, Profile dan bottom navigation yang benar-benar berpindah tab tanpa menggandakan destination.
- [ ] Buat workout graph untuk Start, Quick Start, Library, Filters, Exercise Details, Active Workout, dan Summary.
- [ ] Tambahkan route History, Workout Detail, Edit Profile, dan Preferences; validasi UID saat membuka detail lewat ID route.
- [ ] Scope satu WorkoutViewModel ke workout graph agar Library, Active, dan Summary membaca sesi yang sama.
- [ ] Gunakan unidirectional data flow, StateFlow, state hoisting, lifecycle-aware collection, serta SavedStateHandle untuk state layar/input sementara yang sesuai.
- [ ] Pertahankan asal navigasi Exercise Details agar Back kembali ke Library atau Active yang benar.
- [ ] Terapkan Back sesuai state workout: jangan pernah membuang sesi tanpa konfirmasi; Active dapat tetap latihan, kembali dengan draft, atau buang sesi.

## 5. Workout end-to-end

- [ ] Implementasikan Start Workout dan Quick Start: konfirmasi mulai membuat draft atomik dan timer dimulai saat itu; jika draft tersedia, tawarkan lanjutkan atau buang.
- [ ] Implementasikan Library dengan search substring case-insensitive dan filter kelompok otot AND; pertahankan search/filter saat membuka detail lalu kembali.
- [ ] Implementasikan Filters dengan pilihan tunggal, Terapkan, Reset, dan hasil kosong yang tidak menghapus pencarian.
- [ ] Implementasikan Exercise Details beserta nama, kelompok otot, alat, petunjuk, riwayat terakhir, dan aksi tambah.
- [ ] Cegah exercise ID yang sama dibuat dua kali dalam satu sesi; hapus exercise draft dengan konfirmasi jika sudah berisi set.
- [ ] Implementasikan input dan simpan set: repetisi integer > 0; beban >= 0; terima koma/titik sebagai desimal; tolak kosong, negatif, format salah, NaN/infinity, dan overflow.
- [ ] Pastikan perubahan invalid tidak menimpa set lama. Simpan perubahan persisten sebelum UI mengakui set berhasil.
- [ ] Implementasikan edit/hapus set dengan ID stabil, penomoran ulang atomik, dan total yang selalu dihitung ulang dari data sumber.
- [ ] Hitung set, repetisi, jumlah exercise valid, volume per set/exercise/sesi dari set tersimpan; beban 0 tetap dihitung sebagai set dan repetisi.
- [ ] Implementasikan timer dari startedAt, tampilan HH:mm:ss, rotasi/background tanpa reset, pemulihan ACTIVE setelah process death, serta clock yang bisa diinjeksi.
- [ ] Implementasikan REVIEW saat Finish: tolak sesi tanpa set valid, bekukan endedAt/durasi, dan tangani perubahan input yang belum disimpan.
- [ ] Buat Summary dengan durasi, exercise, set, repetisi, volume, status PR, kembali untuk koreksi, dan simpan sesi.
- [ ] Finalisasi REVIEW ke COMPLETED dalam satu transaksi Room bersama snapshot PR dan penghapusan slot draft; retry harus idempoten dan tidak menggandakan sesi.
- [ ] Implementasikan discard draft ACTIVE/REVIEW dengan konfirmasi dan transaksi; draft yang dibuang tidak masuk history, analytics, PR, atau referensi.

## 6. Personal Record dan referensi progressive overload

- [ ] Hitung rekor per UID + exercise ID dari sesi COMPLETED sebelumnya saja, dengan nilai kilogram presisi penuh.
- [ ] Tampilkan empat status: Catatan awal, Rekor baru, Menyamai rekor, dan Belum melampaui rekor; beban nol tetap valid.
- [ ] Hitung preview PR pada Summary dan simpan previousMax, currentMax, status, serta exercise ID sebagai snapshot final.
- [ ] Tampilkan snapshot PR tetap pada Workout Detail; sesi lama tidak berubah ketika rekor baru tercapai.
- [ ] Ambil sesi exercise COMPLETED terbaru untuk referensi secara deterministik; jangan salah memakai sesi dengan beban maksimum.
- [ ] Bandingkan set berdasarkan nomor, tampilkan delta beban/repetisi, jumlah set, dan volume hanya jika ada set valid saat ini.
- [ ] Tampilkan Tidak ada pembanding untuk set tanpa pasangan dan Belum ada riwayat jika belum ada sesi historis; jangan memakai nol sebagai riwayat fiktif.

## 7. Analytics, profil, preferensi, dan riwayat

- [ ] Implementasikan Analytics untuk 7/30/90 hari berdasarkan endedAt, zona waktu perangkat, dan sesi COMPLETED akun aktif saja.
- [ ] Hitung sesi, set, repetisi, volume, favorit berdasarkan frekuensi sesi (tie-break abjad lalu ID), dan bucket volume harian.
- [ ] Implementasikan empty state tanpa data palsu; sesi dengan beban nol tetap menampilkan sesi/set/repetisi dan volume nol.
- [ ] Implementasikan Home dari data akun: ringkasan minggu kalender Senin–sekarang dan tiga sesi terbaru; hilangkan streak, target, atau rekomendasi yang tidak didukung data.
- [ ] Implementasikan Profile, Edit Profile (hanya nama, trim, email read-only), sign out, dan ringkasan akun aktual.
- [ ] Implementasikan Preferences kg/lb per UID dengan preview 32 kg, penyimpanan eksplisit, pembatalan Back, dan konfirmasi jika ada input set yang belum disimpan.
- [ ] Simpan beban dasar sebagai kg; konversi lb memakai 0.45359237 hanya pada batas input/tampilan. Mengganti unit berulang atau membuka lalu menyimpan set tanpa perubahan tidak boleh mengubah nilai dasar.
- [ ] Implementasikan Workout History terbaru dahulu dan Workout Detail read-only, lengkap dengan set, volume, durasi, dan snapshot PR.

## 8. UI, aksesibilitas, dan keandalan

- [ ] Ganti mockup dengan data/state aktual; terjemahkan UI utama ke Bahasa Indonesia sambil mempertahankan nama exercise/istilah yang ditetapkan PRD.
- [ ] Sesuaikan tema gelap ATHRA dengan Figma atau token fallback PRD; jangan menyalin elemen status bar/home indicator iOS.
- [ ] Pastikan seluruh kontrol yang terlihat punya aksi yang benar; hilangkan kontrol placeholder dan angka personal hardcoded.
- [ ] Tangani loading, empty, validasi, network/storage error, retry, detail tidak ditemukan, dan recovery draft sesuai tabel PRD.
- [ ] Pastikan keyboard tidak menutup input/CTA, layout dapat di-scroll, system insets benar, touch target minimal 48 dp, dan font scale 200% tetap dapat dipakai.
- [ ] Tambahkan label input, content description ikon, dan indikator PR/delta yang tidak hanya mengandalkan warna.
- [ ] Pastikan query database dan kalkulasi berjalan di luar main thread; batalkan query usang ketika search/periode/akun berubah.
- [ ] Verifikasi rotasi, background/foreground, process death, retry, pergantian akun, dan transaksi parsial tanpa kehilangan set yang tersimpan.

## 9. Verifikasi dan handoff

- [ ] Ganti tes template dengan unit test domain untuk parsing, volume, konversi, periode, PR, referensi, dan aturan pembulatan.
- [ ] Tambahkan integration test Room untuk transaksi, constraint, migrasi, recovery, idempotensi, dan isolasi UID.
- [ ] Tambahkan Compose/instrumented test untuk alur auth, workout, Summary, History, Analytics, Profile, dan Preferences.
- [ ] Penuhi acceptance criteria AC01–AC42 pada PRD; tambahkan kasus REVIEW setelah process death, pergantian akun saat query berjalan, batas periode 30/90 hari, dan sesi lintas tengah malam.
- [ ] Jalankan dan catat hasil aktual: ./gradlew testDebugUnitTest lintDebug assembleDebug.
- [ ] Jalankan connectedDebugAndroidTest pada emulator/perangkat dengan Auth Emulator bila diperlukan; catat tes yang tidak dapat dijalankan beserta alasannya.
- [ ] Ukur Analytics pada fixture 10.000 set dan catat perangkat serta waktu aktual terhadap target 2 detik.
- [ ] Pasang dan jalankan APK pada emulator/perangkat; verifikasi alur demo, kondisi offline, rotasi, dan state yang dipulihkan.
- [ ] Siapkan fixture debug H1/H2 yang dapat di-reset dan tidak aktif di production; buat H3 lewat UI saat demo.
- [ ] Simpan screenshot runtime nyata untuk alur utama, PR, referensi, analytics, dan validasi.
- [ ] Lengkapi README, path APK, hasil tes, daftar acceptance lulus/gagal/belum dijalankan, lima teori perkuliahan beserta lokasi kode, dan kendala eksternal Firebase production.
- [ ] Pastikan demo, laporan, README, screenshot, dan implementasi menjelaskan perilaku yang sama; jangan klaim fitur atau verifikasi yang belum dilakukan.

## Definition of Done

- [ ] Seluruh fungsi wajib memakai data persisten aktual yang terisolasi per akun.
- [ ] Auth, workout, set, timer, volume, Summary/finalisasi, PR, referensi, analytics, history, profil, dan unit konsisten end-to-end.
- [ ] Build dan pemeriksaan yang dapat dijalankan lulus; hasil tes dan verifikasi perangkat dicatat jujur.
- [ ] Tidak ada kontrol aktif palsu, angka latihan statis, atau data fixture pada akun production.
- [ ] README, APK, bukti runtime, acceptance report, dan catatan prasyarat eksternal tersedia.
