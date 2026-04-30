import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("com.chaquo.python")
}

// Add Java toolchain for Java 17 compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.deepeye.otg"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "25.1.8937393"

    defaultConfig {
        applicationId = "com.deepeye.otg"
        minSdk = 26
        targetSdk = 35
        versionCode = 2027200
        versionName = "2027.21.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BYPASS_SERVER_URL", "\"https://api.iremoval-gsmg.com/v1\"")

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                cFlags("-std=c11")
                arguments("-DANDROID_STL=c++_shared", "-DANDROID_PLATFORM=android-26")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // ── Signing ────────────────────────────────────────────────────────
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("STORE_FILE")
            val storePwd = keystoreProps.getProperty("STORE_PASSWORD")
            val keyAl = keystoreProps.getProperty("KEY_ALIAS")
            val keyPwd = keystoreProps.getProperty("KEY_PASSWORD")

            val ciStoreFilePath = System.getenv("KEYSTORE_PATH")
            val ciStorePwd = System.getenv("STORE_PASSWORD")
            val ciKeyAlias = System.getenv("KEY_ALIAS")
            val ciKeyPwd = System.getenv("KEY_PASSWORD")

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true

            if (storeFilePath != null && storePwd != null && keyAl != null && keyPwd != null) {
                storeFile = file(storeFilePath)
                storePassword = storePwd
                keyAlias = keyAl
                keyPassword = keyPwd
            } else if (ciStoreFilePath != null && ciStorePwd != null && ciKeyAlias != null && ciKeyPwd != null) {
                storeFile = file(ciStoreFilePath)
                storePassword = ciStorePwd
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPwd
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile?.exists() == true) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            install("six==1.16.0")
            install("construct==2.10.68")
            install("ecdsa==0.19.0")
            install("requests==2.31.0")
            install("urllib3==2.0.7")
            install("pycryptodome")
            install("pyusb==1.2.1")
        }
    }
}

dependencies {
    // Compose & UI
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Stage 3 & 5 Features
    implementation("io.github.skylot:jadx-core:1.5.0")
    implementation("io.github.skylot:jadx-dex-input:1.5.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Glassmorphism & Visuals
    implementation("dev.chrisbanes.haze:haze:1.5.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.5.1")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")

    // QR & Scanning
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Security & Storage
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room Persistence
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Logging & Tools
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.github.mik3y:usb-serial-for-android:3.7.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("androidx.room:room-testing:$room_version")
    testImplementation("org.robolectric:robolectric:4.13")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
}

detekt {
    config.setFrom(files("$rootDir/detekt-config.yml"))
    parallel = true
    buildUponDefaultConfig = true
}
