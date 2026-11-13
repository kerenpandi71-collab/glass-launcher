plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val glassKeystore = rootProject.file("keystore/glass-launcher.keystore")

android {
    namespace = "com.kerenpandi.glassberry"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kerenpandi.glassberry"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (glassKeystore.exists()) {
            create("glass") {
                storeFile = glassKeystore
                storePassword = "glasslauncher123"
                keyAlias = "glasslauncher"
                keyPassword = "glasslauncher123"
            }
        }
    }

    buildTypes {
        debug {
            if (glassKeystore.exists()) {
                signingConfig = signingConfigs.getByName("glass")
            }
        }
        release {
            isMinifyEnabled = false
            if (glassKeystore.exists()) {
                signingConfig = signingConfigs.getByName("glass")
            }
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

dependencies {
}
