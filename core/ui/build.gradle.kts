plugins {
    id("bocatta.android.library")
    id("bocatta.android.compose")
}

android {
    namespace = "com.bocatta.pos.core.ui"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
}
