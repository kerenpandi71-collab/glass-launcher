plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val glassKeystore = rootProject.file("keystore/glass-launcher.keystore")

android {
    namespace = "com.kerenpandi.glasslauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kerenpandi.glasslauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.7"
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
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("dev.rikka.shizuku:api:13.1.5")
}
