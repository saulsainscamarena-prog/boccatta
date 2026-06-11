plugins {
    id("bocatta.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bocatta.pos.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
