// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}

val mojibakeCharacters = setOf('\uFFFD', '\u00C3', '\u00C2', '\u00E2', '\u00F0')
val mojibakeCheckedExtensions = setOf(
    "gradle",
    "kts",
    "kt",
    "java",
    "xml",
    "json",
    "rules",
    "txt",
    "md",
    "properties",
    "toml",
    "yml",
    "yaml",
    "ps1",
)
val mojibakeCheckedFileNames = setOf(".editorconfig", ".gitattributes")
val mojibakeExcludedDirectories = setOf(
    ".git",
    ".gradle",
    "build",
    ".idea",
    "bocatta-windows-port",
    "dead_code_quarantine",
    "testsprite_tests",
    "audit-avd",
)
val mojibakeExcludedFiles = setOf("build_info.txt")

tasks.register("checkMojibake") {
    group = "verification"
    description = "Fails when text files contain common mojibake markers."

    doLast {
        val findings = mutableListOf<String>()

        rootDir.walkTopDown()
            .onEnter { directory -> directory.name !in mojibakeExcludedDirectories }
            .filter { file ->
                file.isFile &&
                    file.name !in mojibakeExcludedFiles &&
                    (file.extension in mojibakeCheckedExtensions || file.name in mojibakeCheckedFileNames)
            }
            .forEach { file ->
                file.useLines(Charsets.UTF_8) { lines ->
                    lines.forEachIndexed { index, line ->
                        if (line.any { it in mojibakeCharacters }) {
                            findings += "${file.relativeTo(rootDir).invariantSeparatorsPath}:${index + 1}: possible mojibake"
                        }
                    }
                }
            }

        if (findings.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Mojibake sospechoso detectado:")
                    findings.forEach { appendLine(it) }
                },
            )
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("checkMojibake"))
    }
}
