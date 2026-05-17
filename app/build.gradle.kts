plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // id("org.jetbrains.kotlin.kapt")
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("jacoco")
}

android {
    namespace = "com.bocatta.pos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bocatta.pos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEMO_EMAIL", "\"demo@bocatta.com\"")
        buildConfigField("String", "DEMO_PASSWORD", "\"demo123\"")
        buildConfigField("boolean", "DEMO_MODE_ENABLED", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        getByName("test") {
            java.srcDirs("src/test/java")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.compose.material:material-icons-extended")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Jacoco for code coverage
    jacoco
    // Crashlytics & Analytics (explicit versions to guarantee resolution)
    //implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.2")
    implementation("com.google.firebase:firebase-analytics")
    // Timber (logging)
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation(libs.kotlinx.coroutines.play.services)

    // Navegación
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Inyección de dependencias - Koin
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("io.insert-koin:koin-androidx-compose:4.0.0")
    implementation("io.insert-koin:koin-core:4.0.0")
    testImplementation("io.insert-koin:koin-test:4.0.0")


    // WorkManager (Sincronización en segundo plano)
    implementation(libs.work.runtime.ktx)

    // Serialización JSON para guardar ventas offline
    implementation(libs.kotlinx.serialization.json)

    // Room — vía KSP2 (compatible con Kotlin 2.2.10)
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
}

// Task to replace FirebaseFirestore.getInstance() with provider
tasks.register<Exec>("replaceFirestore") {
    group = "refactor"
    description = "Reemplaza FirebaseFirestore.getInstance() por FirebaseFirestoreProvider.db en todos los .kt"
    commandLine(
        "bash",
        "-c",
        """
    find . -name \"*.kt\" ! -path \"*/FirebaseFirestoreProvider.kt\" \
      -exec sed -i 's/FirebaseFirestore\\.getInstance()/FirebaseFirestoreProvider.db/g' {} +
        """.trimIndent()
    )
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

// Simple task to list coverage files
tasks.register("coverageCheck") {
    group = "verification"
    description = "Lists Jacoco coverage .exec files"
    doLast {
        val jacocoDir = project.layout.buildDirectory.dir("jacoco").get().asFile
        if (jacocoDir.exists()) {
            jacocoDir.listFiles()?.forEach { file ->
                println("Coverage file: ${file.name} (${file.length()} bytes)")
            }
        }
    }
}