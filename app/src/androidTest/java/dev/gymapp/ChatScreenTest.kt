package dev.gymapp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
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
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signOut() {
        (ApplicationProvider.getApplicationContext() as GymApplication).authRepository.signOut()
    }

    @Test
    fun chatScreen_devSignIn_sendSample_receivesResponse() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithText("Close grip bench 130").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Voice message"), 15000L)
    }
}
