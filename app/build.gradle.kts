import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

android {
    namespace = "com.example.kaimera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kaimera"
        minSdk = 24
        targetSdk = 34
        versionCode = 57
        versionName = "33.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                
                val properties = Properties()
                val localPropertiesFile = rootProject.file("local.properties")
                if (localPropertiesFile.exists()) {
                    properties.load(FileInputStream(localPropertiesFile))
                }
                
                storePassword = properties.getProperty("store.password")
                keyAlias = properties.getProperty("key.alias")
                keyPassword = properties.getProperty("key.password")
                
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // CameraX dependencies (prepared for future use)
    val cameraxVersion = "1.5.1"
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
    
    // OkHttp for downloads
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // LibGDX
    val gdxVersion = "1.12.1"
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    
    val natives by configurations.creating
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
    
    implementation("com.squareup.okhttp3:okhttp:4.12.0")


    // Task to copy native libs
    val copyAndroidNatives = tasks.register("copyAndroidNatives") {
        doFirst {
            val jniLibsDir = file("src/main/jniLibs")
            jniLibsDir.mkdirs()
            
            configurations["natives"].files.forEach { jar ->
                var outputDir: java.io.File? = null
                if (jar.name.endsWith("natives-armeabi-v7a.jar")) outputDir = file("src/main/jniLibs/armeabi-v7a")
                if (jar.name.endsWith("natives-arm64-v8a.jar")) outputDir = file("src/main/jniLibs/arm64-v8a")
                if (jar.name.endsWith("natives-x86.jar")) outputDir = file("src/main/jniLibs/x86")
                if (jar.name.endsWith("natives-x86_64.jar")) outputDir = file("src/main/jniLibs/x86_64")
                
                if (outputDir != null) {
                    outputDir.mkdirs()
                    copy {
                        from(zipTree(jar))
                        into(outputDir)
                        include("*.so")
                    }
                }
            }
        }
    }
    
    tasks.whenTaskAdded {
        if (name.contains("package")) {
            dependsOn(copyAndroidNatives)
        }
    }

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Extended Icons
    implementation("androidx.compose.material:material-icons-extended")
}
