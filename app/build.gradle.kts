plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dlanuza.mixxxdroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dlanuza.mixxxdroid"

        minSdk = 28
        targetSdk = 36

        // =========================================================
        // VERSIONAMIENTO INCREMENTAL
        // =========================================================
        // versionCode: Número entero que incrementas obligatoriamente
        // con cada actualización que subas a Google Play (ej. 1, 2, 3...)
        versionCode = 2

        // versionName: Texto visible para el usuario en la tienda (ej. "1.0", "1.1", "1.0.1")
        versionName = "1.1"

        ndk {
            abiFilters += listOf(
                "arm64-v8a",
                "armeabi-v7a"
            )
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_shared")
            }
        }

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

// =========================================================
// CMAKE
// =========================================================
//
// IMPORTANTE:
// Este bloque va directamente dentro de android,
// NO dentro de defaultConfig.
//
// =========================================================

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        prefab = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7")
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("com.google.oboe:oboe:1.11.0")
}