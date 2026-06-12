plugins {
    id("bocatta.android.feature")
}

android {
    namespace = "com.bocatta.pos.feature.auth"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
}




