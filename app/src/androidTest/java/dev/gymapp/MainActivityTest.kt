package dev.gymapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_displaysGymApp() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Gym App", substring = true).assertExists()
    }

    @Test
    fun app_displaysApiBaseUrl() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("10.0.2.2", substring = true).assertExists()
    }
}
