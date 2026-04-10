package com.deepeye.otg

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bypassScreenLoads() {
        // Navigate to bypass screen
        composeTestRule
            .onNodeWithContentDescription("Bypass")
            .performClick()

        // Verify key UI elements exist
        composeTestRule
            .onNodeWithText("DEEPEYE BYPASS")
            .assertIsDisplayed()

        // Verify grid cards rendered
        composeTestRule
            .onAllNodesWithTag("bypass_card")
            .fetchSemanticsNodes()
            .let { assert(it.isNotEmpty()) { "No bypass cards rendered!" } }
    }

    @Test
    fun bottomNavRendersAllTabs() {
        val tabs = listOf("Dashboard", "Lab", "Bypass", "Tool", "Archive")
        tabs.forEach { tab ->
            composeTestRule
                .onNodeWithContentDescription(tab)
                .assertIsDisplayed()
        }
    }

    @Test
    fun neonGlassCardsHaveContent() {
        composeTestRule
            .onNodeWithContentDescription("Bypass")
            .performClick()

        composeTestRule.waitUntil(3000) {
            composeTestRule
                .onAllNodesWithTag("bypass_card")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        // Each card should have Run button
        composeTestRule
            .onAllNodesWithText("RUN")
            .fetchSemanticsNodes()
            .let { assert(it.isNotEmpty()) { "Run buttons missing!" } }
    }

    @Test
    fun searchFiltersWork() {
        composeTestRule
            .onNodeWithContentDescription("Bypass")
            .performClick()

        composeTestRule
            .onNodeWithTag("search_input")
            .performTextInput("Hello")

        composeTestRule.waitForIdle()

        // Verify results updated
        composeTestRule
            .onNodeWithTag("result_count")
            .assertExists()
    }

    @Test
    fun runButtonChangesState() {
        composeTestRule
            .onNodeWithContentDescription("Bypass")
            .performClick()

        composeTestRule.waitUntil(3000) {
            composeTestRule
                .onAllNodesWithText("RUN")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Click first RUN button
        composeTestRule
            .onAllNodesWithText("RUN")
            .onFirst()
            .performClick()

        // Should show RUNNING state
        composeTestRule.waitUntil(1000) {
            composeTestRule
                .onAllNodesWithText("RUNNING")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
</task_progress>
- [ ] Install APK on device
- [ ] Verify installation
- [ ] Launch app and monitor logs
- [ ] Check for crashes
- [ ] Take screenshots of all screens
- [ ] Create screenshot tests
- [ ] Add missing semanticsTestTags to BypassScreen.kt
- [ ] Check performance
- [ ] Apply final visual polish improvements
- [ ] Build release version
</task_progress>
</write_to_file>