package com.bocatta.pos.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Generates a Baseline Profile for Bocatta POS.
 *
 * Run on API 33+ emulator or rooted device:
 *   ./gradlew :benchmark:connectedCheck -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 *
 * After execution, copy the generated baseline-prof.txt from
 * benchmark/build/outputs/connected_android_test_additional_output/ into
 * app/src/main/baseline-prof.txt
 */
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() {
        baselineProfileRule.collect(
            packageName = "com.bocatta.pos"
        ) {
            pressHome()
            // Start the main activity from the launcher
            startActivityAndWait()
        }
    }
}
