plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hirain.aiagent.test"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hirain.aiagent.test"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // Eval Debug 在 x86_64 模拟器验证文本/AIDL，不打包仅有 ARM ABI 的语音库。
            ndk {
                abiFilters += "x86_64"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.drawerlayout)
    implementation(files("libs/SparkChain.aar"))
    implementation(files("libs/Codec.aar"))
    implementation(files("libs/xxpermissions-8.2.aar"))
    debugImplementation(libs.gson)
    testImplementation(libs.junit)
}
