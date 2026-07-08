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
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
}
