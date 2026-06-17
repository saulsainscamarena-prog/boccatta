plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.findByType<com.android.build.api.dsl.ApplicationExtension>()?.apply {
    buildFeatures {
        compose = true
    }
}
extensions.findByType<com.android.build.api.dsl.LibraryExtension>()?.apply {
    buildFeatures {
        compose = true
    }
}

composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_metrics")
}

dependencies {
    val bom = libs.findLibrary("androidx-compose-bom").get()
    add("implementation", platform(bom))
    add("androidTestImplementation", platform(bom))

    add("implementation", libs.findLibrary("androidx-compose-ui").get())
    add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
    add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    add("implementation", libs.findLibrary("androidx-compose-material3").get())
    add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())

    add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
    add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
}