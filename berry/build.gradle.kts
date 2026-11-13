plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val glassKeystore = rootProject.file("keystore/glass-launcher.keystore")

android {
    namespace = "com.kerenpandi.glassberry"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kerenpandi.glassberry"
        minSdk = 33
        targetSdk = 35
        versionCode = 9
        versionName = "2.3.1"
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

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}
