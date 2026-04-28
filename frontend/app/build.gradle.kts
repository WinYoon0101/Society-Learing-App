plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.frontend"
    // SỬA LỖI DÒNG 7: Thay đổi cú pháp compileSdk đơn giản hơn
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.frontend"
        minSdk = 24
        // SỬA LỖI: targetSdk nên để 34 hoặc 35 để ổn định
        targetSdk = 34
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
}

dependencies {
    // Thư viện cơ bản
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")

    // UI & Animation
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.airbnb.android:lottie:6.1.0")

    // Glide (Đã xóa bản cũ 4.15.1, giữ lại bản mới nhất 4.16.0)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}