repositories {
    google()
    mavenCentral()
}

plugins {
    id("com.android.application") version "8.13.0"
    kotlin("android") version "2.2.21"

    id("org.jetbrains.compose") version "1.9.2"
    kotlin("plugin.compose") version "2.2.21"

    kotlin("plugin.serialization") version "2.2.21"
}

dependencies {
    implementation("androidx.activity:activity:1.11.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.activity:activity-ktx:1.11.0")

    implementation("androidx.compose.ui:ui:1.9.4")
    implementation("androidx.compose.runtime:runtime:1.9.4")
    implementation("androidx.compose.material3:material3:1.4.0")

    implementation("com.google.android.material:material:1.14.0-alpha06")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

android {
    namespace = "cc.goll.caff"

    compileSdk = 36
    
    defaultConfig {
        applicationId = "cc.goll.caff"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storeFile = file(System.getenv("KEYSTORE_FILE"))
            storePassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

kotlin {
    jvmToolchain(17)
}
