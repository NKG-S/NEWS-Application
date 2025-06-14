plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.uoc.fot.ict.edunews"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uoc.fot.ict.edunews"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core and UI libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // RecyclerView, ViewPager2
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    // SwipeRefreshLayout
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // AndroidX KTX extensions (provides Consumer and other utilities)
    implementation ("androidx.core:core-ktx:1.13.1") // Keep this updated with the latest stable version

    // Firebase BOM (Bill of Materials) - ALWAYS include this platform line first
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Using a very recent stable version, adjust if newer is out
    // Firebase specific SDKs - do NOT specify versions here when using BOM
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database") // If you're using Realtime Database
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CircleImageView (for profile icon)
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
