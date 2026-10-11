
plugins {
    //========== START: PLUGINS ==========
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    //========== END: PLUGINS ==========
}

android {
    //========== START: ANDROID CONFIG ==========
    namespace = "com.zizzverse.zizzchat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zizzverse.zizzchat"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    //========== END: ANDROID CONFIG ==========
}

dependencies {
    //========== START: JETPACK COMPOSE ==========
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    //========== END: JETPACK COMPOSE ==========

    //========== START: FIREBASE ==========
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    //========== END: FIREBASE ==========

    //========== START: ZEGOCLOUD VOICE & VIDEO CALL ==========
    implementation("im.zego:zego_uikit_prebuilt_call_android:+")
    //========== END: ZEGOCLOUD VOICE & VIDEO CALL ==========

    //========== START: DEBUG ==========
    debugImplementation("androidx.compose.ui:ui-tooling")
    //========== END: DEBUG ==========
}
