plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The Google Services plugin needs app/google-services.json, which only
// exists once you've created your own Firebase project for push
// notifications (see README.md). Applying it only when that file is present
// means the app builds and works fine - just without push - before you've
// done that step, instead of failing every build with "File
// google-services.json is missing."
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.trelegantboutique.admin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.trelegantboutique.admin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Changed in Settings > Server address inside the app itself, so this
        // default only matters the very first time the app is opened.
        buildConfigField("String", "DEFAULT_API_BASE_URL", "\"https://trelegantboutique.com/api/\"")
    }

    signingConfigs {
        create("release") {
            // These four values come from the keystore.properties file that sits
            // next to this build.gradle.kts (see README.md for how it is created
            // in GitHub Actions from repository secrets - it is never committed).
            val props = java.util.Properties()
            val f = rootProject.file("keystore.properties")
            if (f.exists()) {
                props.load(f.inputStream())
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Secure local storage for the session token (Android Keystore-backed)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Fingerprint / biometric unlock (BiometricPrompt needs a FragmentActivity)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // Push notifications
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
