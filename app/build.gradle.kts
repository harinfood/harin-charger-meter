plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.contoh.chargermeter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.contoh.chargermeter"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
