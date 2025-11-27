plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.kaimera"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.kaimera"
        minSdk = 24
        targetSdk = 33
        versionCode = 40
        versionName = "22.3.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.create("release") {
                storeFile = file("release.keystore")
                storePassword = "password"
                keyAlias = "key0"
                keyPassword = "password"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // CameraX dependencies (prepared for future use)
    val cameraxVersion = "1.2.2"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-video:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")
    implementation("androidx.preference:preference-ktx:1.2.0")
    
    // Gallery dependencies
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("io.coil-kt:coil:2.4.0")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
}
