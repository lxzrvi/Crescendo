// Top-level build file — Crescendo
// AGP 9 provides built-in Kotlin; keep its compiler in step with Compose and Haze.
buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
