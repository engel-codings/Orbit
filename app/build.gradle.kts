plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.engelcode.orbit"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.engelcode.orbit"
        minSdk = 21          // Android 5.0 – läuft auch auf sehr alten Geräten
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // Fester Test-Schlüssel: so lassen sich neue APKs einfach über die alte installieren.
    // Für den Play Store bekommt Orbit später einen eigenen, geheimen Schlüssel.
    signingConfigs {
        getByName("debug") {
            storeFile = file("orbit-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
