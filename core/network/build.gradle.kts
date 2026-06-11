plugins {
    id("bocatta.android.library")
}

android {
    namespace = "com.bocatta.pos.core.network"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}
