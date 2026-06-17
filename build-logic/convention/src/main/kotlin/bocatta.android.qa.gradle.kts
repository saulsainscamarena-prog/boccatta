plugins {
    id("jacoco")
    id("io.gitlab.arturbosch.detekt")
}

// Configure detekt task
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
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
            "**/R.class", "**/R$*.class", "**/BuildConfig.*",
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

// Jacoco coverage verification
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Verifies minimum 60% line coverage"
    dependsOn("jacocoTestReport")

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class", "**/R$*.class", "**/BuildConfig.*",
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
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}
