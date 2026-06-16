plugins {
    id("bocatta.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bocatta.pos.core.database"
}

dependencies {
    implementation(project(":core:model"))
    
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
