package dev.gymapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun signInScreen_displaysSignInButton() {
        composeTestRule.onNodeWithText("Sign in with Google").assertExists()
    }

    @Test
    fun signInScreen_displaysLogo() {
        composeTestRule.onNodeWithContentDescription("Gym App logo").assertExists()
    }
}
