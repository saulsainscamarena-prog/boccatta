plugins {
    id("com.android.test")
}

android {
    namespace = "com.bocatta.pos.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
}
