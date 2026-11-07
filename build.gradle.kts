plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.23" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
}

subprojects {
    // Enable AndroidX for all subprojects
    extra["android.useAndroidX"] = true
    extra["android.enableJetifier"] = true
}