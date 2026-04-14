package com.deepeye.otg.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepeye.otg.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BypassScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() { hiltRule.inject() }

    // TC001 — App launches without crash
    @Test
    fun appLaunches_nocrash() {
        composeRule.onNodeWithText("DeepEye Unlocker")
            .assertExists()
    }

    // TC002 — Valid IMEI → green state
    @Test
    fun validImei_showsGreenManufacturer() {
        composeRule
            .onNodeWithText("IMEI (15 digits)")
            .performTextInput("490154203237518")

        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithText("✅", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNodeWithText("✅", substring = true)
            .assertExists()
    }

    // TC003 — Invalid IMEI → red error
    @Test
    fun invalidImei_showsRedError() {
        composeRule
            .onNodeWithText("IMEI (15 digits)")
            .performTextInput("123456789012345")

        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithText("❌ Invalid IMEI", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNodeWithText("❌ Invalid IMEI", substring = true)
            .assertExists()
    }

    // TC004 — Run button disabled with no valid IMEI
    @Test
    fun runButton_disabledWithoutValidImei() {
        composeRule
            .onNodeWithText("RUN BYPASS")
            .assertIsNotEnabled()
    }

    // TC005 — Log console exists
    @Test
    fun logConsole_exists() {
        composeRule
            .onNodeWithTag("log_console")
            .assertExists()
    }

    // TC006 — Bottom nav has 5 items
    @Test
    fun bottomNav_hasFiveItems() {
        listOf("Home", "Devices", "Apple", "Logs", "Settings")
            .forEach { tab ->
                composeRule
                    .onNodeWithText(tab)
                    .assertExists()
            }
    }

    // TC007 — Python IMEI check under 500ms
    @Test
    fun pythonImeiCheck_responseUnder500ms() {
        val start = System.currentTimeMillis()
        composeRule
            .onNodeWithText("IMEI (15 digits)")
            .performTextInput("490154203237518")
        composeRule.waitUntil(500) {
            composeRule
                .onAllNodesWithText("✅", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val elapsed = System.currentTimeMillis() - start
        assert(elapsed < 500) {
            "Python IMEI check took ${elapsed}ms — expected <500ms"
        }
    }
}
