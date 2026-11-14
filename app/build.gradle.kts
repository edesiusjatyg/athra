plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val authEmulatorRequested =
    providers.gradleProperty("athra.authEmulator").orNull?.toBooleanStrictOrNull() ?: false
val authEmulatorHost = providers.gradleProperty("athra.authEmulatorHost").orElse("10.0.2.2").get()
val firebaseProjectId = providers.gradleProperty("athra.firebase.projectId").orElse("").get()
val firebaseAppId = providers.gradleProperty("athra.firebase.appId").orElse("").get()
val firebaseApiKey = providers.gradleProperty("athra.firebase.apiKey").orElse("").get()

android {
    namespace = "id.ac.ub.athra"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "id.ac.ub.athra"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "AUTH_EMULATOR_ENABLED", authEmulatorRequested.toString())
            buildConfigField("String", "AUTH_EMULATOR_HOST", buildConfigString(authEmulatorHost))
            buildConfigField(
                "String",
                "FIREBASE_PROJECT_ID",
                buildConfigString(firebaseProjectId.ifBlank { if (authEmulatorRequested) "demo-athra" else "" }),
            )
            buildConfigField(
                "String",
                "FIREBASE_APP_ID",
                buildConfigString(firebaseAppId.ifBlank {
                    if (authEmulatorRequested) "1:1234567890:android:athradebug" else ""
                }),
            )
            buildConfigField(
                "String",
                "FIREBASE_API_KEY",
                buildConfigString(firebaseApiKey.ifBlank {
                    if (authEmulatorRequested) "demo-api-key" else ""
                }),
            )
        }
        release {
            buildConfigField("boolean", "AUTH_EMULATOR_ENABLED", "false")
            buildConfigField("String", "AUTH_EMULATOR_HOST", buildConfigString("127.0.0.1"))
            buildConfigField("String", "FIREBASE_PROJECT_ID", buildConfigString(firebaseProjectId))
            buildConfigField("String", "FIREBASE_APP_ID", buildConfigString(firebaseAppId))
            buildConfigField("String", "FIREBASE_API_KEY", buildConfigString(firebaseApiKey))
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.firebase.auth)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
