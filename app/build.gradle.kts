plugins {
    id("com.android.application")
}

android {
    namespace = "com.miracast.enabler"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.miracast.enabler"
        minSdk = 31
        targetSdk = 34
        versionCode = 200
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
