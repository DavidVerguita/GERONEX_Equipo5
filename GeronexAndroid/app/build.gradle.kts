plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.geronex"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.geronex"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dirección del backend. debug = laptop por USB (adb reverse); azure = la VM de Azure.
        buildConfigField("String", "BASE_URL", "\"http://localhost:8080/api\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        // APK que habla con la VM de Azure (por nginx en el puerto 80)
        create("azure") {
            initWith(getByName("debug"))
            buildConfigField("String", "BASE_URL", "\"http://20.69.91.86/api\"")
            matchingFallbacks += "debug"
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}