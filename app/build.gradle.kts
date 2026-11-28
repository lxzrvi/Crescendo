import com.android.build.api.variant.HostTestBuilder

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseStorePath = System.getenv("CRESCENDO_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("CRESCENDO_RELEASE_PASSWORD")
val releaseRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

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
        // Actions run numbers increase monotonically, allowing future signed updates.
        versionCode = (releaseRunNumber ?: 2).coerceAtLeast(2)
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            if (!releaseStorePath.isNullOrBlank() && !releaseStorePassword.isNullOrBlank()) {
                storeFile = file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = "crescendo"
                keyPassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
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

// AGP 9 enables host tests for debug only unless a release variant opts in.
// This reuses the release compilation for CI tests without touching APK signing.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.hostTests.getValue(HostTestBuilder.UNIT_TEST_TYPE).enable = true
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
    // Extract actual album-cover colors for the Home cards, off the main thread.
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")

    // Haze still supplies the progressive blur behind the status bar.
    implementation("dev.chrisbanes.haze:haze:2.1.0")
    implementation("dev.chrisbanes.haze:haze-blur:2.1.0")

    // Pinned upstream PrismalAGSL v1.0.7, including its AGSL assets and manifest.
    // The AAR is vendored to avoid relying on a JitPack download during builds.
    implementation(files("libs/PrismalAGSL-v1.0.7.aar"))
    implementation("androidx.startup:startup-runtime:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
