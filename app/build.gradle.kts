plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.lxzrvi.crescendo"
    // Haze 2.1 and the Compose UI it uses require API 37.0 at compile time.
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.lxzrvi.crescendo"
        minSdk = 26
        // Keep runtime behavior unchanged while updating the compile toolchain.
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Foreground in-app playback lets Crescendo build its own real play history.
    implementation("androidx.media3:media3-exoplayer:1.11.1")

    // Haze still supplies the progressive blur behind the status bar.
    implementation("dev.chrisbanes.haze:haze:2.1.0")
    implementation("dev.chrisbanes.haze:haze-blur:2.1.0")

    // Pinned upstream PrismalAGSL v1.0.7, including its AGSL assets and manifest.
    // The AAR is vendored to avoid relying on a JitPack download during builds.
    implementation(files("libs/PrismalAGSL-v1.0.7.aar"))
    implementation("androidx.startup:startup-runtime:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
