plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.healthconnectwearapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.healthconnectwearapp"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        compose = true
        viewBinding = true

    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation ("androidx.core:core-ktx:1.16.0")
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation (libs.androidx.wear)
    implementation (libs.play.services.wearable)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.wear:wear-phone-interactions:1.1.0")
    implementation("androidx.wear:wear-remote-interactions:1.0.0")
    compileOnly("com.google.android.wearable:wearable:2.9.0")

}