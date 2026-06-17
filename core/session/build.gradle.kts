plugins {
    id("bocatta.android.library")
    id("bocatta.android.compose")
}

android {
    namespace = "com.bocatta.pos.core.session"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(project(":core:ui"))
    
    // Koin for dependency injection
    implementation(libs.koin.androidx.compose)
    
}
