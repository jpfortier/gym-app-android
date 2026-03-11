package dev.gymapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signOut() {
        (ApplicationProvider.getApplicationContext() as PrTracksApplication).authRepository.signOut()
    }

    @Test
    fun app_launchesWithSignInScreen() {
        composeTestRule.onNodeWithContentDescription("Sign in with Google").assertExists()
    }
}
