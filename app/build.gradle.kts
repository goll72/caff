repositories {
    google()
    mavenCentral()
}

plugins {
    id("com.android.application") version "8.13.0"
    kotlin("android") version "2.2.21"
}

android {
    namespace = "cc.goll.caff"

    compileSdk = 36
    
    defaultConfig {
        applicationId = "cc.goll.caff"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
