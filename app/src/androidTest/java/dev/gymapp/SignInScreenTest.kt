package dev.gymapp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signOut() {
        (ApplicationProvider.getApplicationContext() as GymApplication).authRepository.signOut()
    }

    @Test
    fun signInScreen_displaysSignInButton() {
        composeTestRule.onNodeWithContentDescription("Sign in with Google").assertExists()
    }

    @Test
    fun signInScreen_displaysLogo() {
        composeTestRule.onNodeWithContentDescription("Gym App logo").assertExists()
    }

    @Test
    fun signInScreen_devSignIn_navigatesToChat() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
    }
}
