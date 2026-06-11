plugins {
    id("bocatta.android.library")
    alias(libs.plugins.ksp)
}

android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("Boolean", "DEMO_MODE_ENABLED", "true")
        buildConfigField("String", "DEMO_EMAIL", "\"demo@bocatta.com\"")
        buildConfigField("String", "DEMO_PASSWORD", "\"bocatta123\"")
    }
    namespace = "com.bocatta.pos.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    
    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    
    // Room
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}



