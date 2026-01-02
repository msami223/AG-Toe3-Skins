plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.devstormtech.toe3skins"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devstormtech.toe3skins"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../toe3skins-release.jks")
            storePassword = "Sami223@!"
            keyAlias = "toe3skins"
            keyPassword = "Sami223@!"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true // Enable R8 shrinking to reduce size
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // HTTP requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.15.1")

    // RecyclerView for lists
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CardView for skin cards
    implementation("androidx.cardview:cardview:1.0.0")

    // PULL TO REFRESH
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Firebase (The Postman)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-messaging")

    // SPLASH SCREEN
    implementation("androidx.core:core-splashscreen:1.0.1")

    // SHIMMER EFFECT (Added this)
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // MVVM & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation("androidx.fragment:fragment-ktx:1.7.0")

    // Google Play In-App Updates (Force Update)
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Yandex Mobile Ads SDK (Core)
    implementation("com.yandex.android:mobileads:7.0.1")
}