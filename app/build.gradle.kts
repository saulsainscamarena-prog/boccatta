import java.util.Properties

plugins {
    id("bocatta.android.application")
    id("bocatta.android.compose")
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dropshots)
    id("jacoco")
}

android {
    namespace = "com.bocatta.pos"
    compileSdk = 37

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")

    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }
    if (localPropertiesFile.exists()) {
        val stream = localPropertiesFile.inputStream()
        localProperties.load(stream)
        stream.close()
    }
    val demoEmail = localProperties.getProperty("DEMO_EMAIL") ?: ""
    val demoPassword = localProperties.getProperty("DEMO_PASSWORD") ?: ""
    val demoModeEnabled = localProperties.getProperty("DEMO_MODE_ENABLED") ?: "false"

    defaultConfig {
        applicationId = "com.bocatta.pos"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "DEMO_EMAIL", "\"\"")
            buildConfigField("String", "DEMO_PASSWORD", "\"\"")
            buildConfigField("boolean", "DEMO_MODE_ENABLED", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "DEMO_EMAIL", "\"$demoEmail\"")
            buildConfigField("String", "DEMO_PASSWORD", "\"$demoPassword\"")
            buildConfigField("boolean", "DEMO_MODE_ENABLED", demoModeEnabled)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

kotlin {
    jvmToolchain(17)
}

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.material.icons.extended)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.koin.test.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Jacoco for code coverage
    jacoco

    // Timber (logging)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.play.services)

    // Navegación
    implementation(libs.androidx.navigation.compose)

    // Inyección de dependencias - Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core)
    testImplementation(libs.koin.test)

    // WorkManager (Sincronización en segundo plano)
    implementation(libs.work.runtime.ktx)

    // Baseline Profiles (ProfileInstaller)
    implementation(libs.androidx.profileinstaller)

    // Serialización JSON para guardar ventas offline
    implementation(libs.kotlinx.serialization.json)

    // Room — vía KSP2
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:ventas"))
    implementation(project(":feature:inventario"))
    implementation(project(":feature:admin"))
}

// Configure detekt task
tasks.named<io.gitlab.arturbosch.detekt.Detekt>("detekt") {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

// Enable Jacoco for unit tests (generates .exec files)
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        destinationFile = project.layout.buildDirectory.file("jacoco/${name}.exec").get().asFile
    }
}

// Jacoco HTML + XML report generation
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates Jacoco code coverage report for debug unit tests"
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val mainSrc = "${project.projectDir}/src/main/java"
    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class", "**/R${'$'}*.class", "**/BuildConfig.*",
            "**/Manifest*.*", "**/*Test*.*",
            "**/di/**", "**/*Module*",
            "**/databinding/**", "**/BR.class"
        )
    }

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(debugTree)
    executionData.setFrom(
        fileTree(project.layout.buildDirectory) { include("jacoco/*.exec") }
    )
}

// Jacoco coverage verification ?" fails build below 60%
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Verifies minimum 60% line coverage"
    dependsOn("jacocoTestReport")

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class", "**/R${'$'}*.class", "**/BuildConfig.*",
            "**/Manifest*.*", "**/*Test*.*",
            "**/di/**", "**/*Module*",
            "**/databinding/**", "**/BR.class"
        )
    }

    classDirectories.setFrom(debugTree)
    executionData.setFrom(
        fileTree(project.layout.buildDirectory) { include("jacoco/*.exec") }
    )

    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}



