plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.spesa"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.spesa"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
    // ViewBinding: gera uma classe para cada layout XML, assim acessamos as views
    // com binding.nomeDaView em vez de findViewById.
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    // Gson: transforma as listas em texto JSON para salvar no celular e lê de volta.
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
