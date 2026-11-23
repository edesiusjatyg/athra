# Menjalankan ATHRA di lokal

1. Start Android Emulator API 37 dari Android Studio → Device Manager.
2. Jalankan dari root repo: `npx --yes firebase-tools@13.29.2 emulators:start --only auth --project demo-athra`
3. Di terminal kedua, jalankan dari root repo: `adb reverse tcp:9099 tcp:9099 && ./gradlew :app:installDebug -Pathra.authEmulator=true -Pathra.authEmulatorHost=127.0.0.1 && adb shell monkey -p id.ac.ub.athra 1`
