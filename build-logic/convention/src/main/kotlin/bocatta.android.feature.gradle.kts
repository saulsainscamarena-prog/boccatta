plugins {
    id("bocatta.android.library")
    id("bocatta.android.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", libs.findLibrary("androidx-core-ktx").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    add("implementation", libs.findLibrary("androidx-navigation-compose").get())
    add("implementation", platform(libs.findLibrary("firebase-bom").get()))
    add("implementation", libs.findLibrary("firebase-firestore").get())
    add("implementation", libs.findLibrary("firebase-auth").get())
    add("implementation", libs.findLibrary("timber").get())

    
    // Koin
    add("implementation", libs.findLibrary("koin-android").get())
    add("implementation", libs.findLibrary("koin-androidx-compose").get())
    add("implementation", libs.findLibrary("koin-core").get())

    // Testing
    add("testImplementation", libs.findLibrary("junit").get())
    add("testImplementation", libs.findLibrary("mockk").get())
    add("testImplementation", libs.findLibrary("koin-test").get())
    add("testImplementation", libs.findLibrary("koin-test-junit4").get())
    add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
    add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
}


