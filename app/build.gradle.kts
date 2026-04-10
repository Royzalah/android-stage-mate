import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Read Maps API key from local.properties (not checked into VCS)
val localProperties = project.rootProject.file("local.properties")
val mapsProperties = Properties()
if (localProperties.exists()) {
    mapsProperties.load(localProperties.inputStream())
}

android {
    namespace = "com.roei.stagemate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stagemate.official"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsProperties.getProperty("MAPS_API_KEY", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true          // Required for BuildConfig.DEBUG in App.kt
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // Glide — image loading (use via ImageLoader wrapper, never directly)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Lottie — high-quality animations for splash screen
    implementation("com.airbnb.android:lottie:6.4.0")

    // QR Code Generation (ZXing)
    implementation("com.google.zxing:core:3.5.4")

    // Gson — JSON serialization for SharedPreferences
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase BoM — version-pins all Firebase libs
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")

    // Firebase App Check — Play Integrity (release) + Debug (debug builds only)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Google Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase UI Auth (replaces deprecated GoogleSignIn)
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    // Coroutines & Lifecycle (for lifecycleScope)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

// Android Gradle Plugin doesn't provide a 'testClasses' task that some IDEs expect.
// Register a no-op task so IDE sync and CLI invocations don't fail.
tasks.register("testClasses")
