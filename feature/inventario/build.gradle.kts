plugins {
    id("bocatta.android.feature")
}

android {
    namespace = "com.bocatta.pos.feature.inventario"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
}