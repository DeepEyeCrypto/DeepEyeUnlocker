plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "com.deepeye.otg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.deepeye.otg"
        minSdk = 24
        targetSdk = 34
        versionCode = 2027181
        versionName = "2027.18.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // ── Signing ────────────────────────────────────────────────────────
    // CI: reads env vars set by GitHub Actions (KEYSTORE_PATH, STORE_PASSWORD, etc.)
    // Local: reads keystore.properties file in project root
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = java.util.Properties().apply {
        if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
    }

    signingConfigs {
        create("release") {
            val propsFile = rootProject.file("local.properties")
            val props = java.util.Properties().apply {
                if (propsFile.exists()) load(propsFile.inputStream())
            }
            // Read from local.properties (DO NOT commit keystore to git)
            storeFile = if (props.containsKey("KEYSTORE_PATH")) file(props["KEYSTORE_PATH"] as String) else null
            storePassword = props.getProperty("KEYSTORE_PASS", "")
            keyAlias = props.getProperty("KEY_ALIAS", "")
            keyPassword = props.getProperty("KEY_PASS", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0-alpha10")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Haze - Real glassmorphism blur
    implementation("dev.chrisbanes.haze:haze:1.5.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.5.1")
    
    // Compose Animation extras
    implementation("androidx.compose.animation:animation:1.8.0")
    implementation("androidx.compose.animation:animation-graphics:1.8.0")
    
    // Coil for image loading
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // Timber logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Tauri dependencies
    implementation("app.tauri:android-sdk:1.0.0-beta.10")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // USB Serial (for BROM serial fallback)
    implementation("com.github.mik3y:usb-serial-for-android:3.7.0")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ADB Lib (pure Kotlin ADB client — no daemon needed)
    implementation("com.github.tananaev:adblib:0.6")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Room Persistence Library
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
}

// Detekt configuration
detekt {
    config.setFrom(files("$rootDir/detekt-config.yml")) // Assuming a config file exists at the root
    parallel = true
    buildUponDefaultConfig = true // Use default detekt rules
}
