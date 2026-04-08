plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        versionCode = 2027160
        versionName = "2027.16.0"

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
            storeFile = file(
                System.getenv("KEYSTORE_PATH")
                    ?: keystoreProps.getProperty("STORE_FILE", "keystore/deepeye-release.jks")
            )
            storePassword = System.getenv("STORE_PASSWORD")
                ?: keystoreProps.getProperty("STORE_PASSWORD", "")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProps.getProperty("KEY_ALIAS", "")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: keystoreProps.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

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
