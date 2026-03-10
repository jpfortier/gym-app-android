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
    fun app_displaysChatScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Type a message...", substring = true).assertExists()
    }

    @Test
    fun app_hasMicrophone() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Microphone", substring = true).assertExists()
    }
}
