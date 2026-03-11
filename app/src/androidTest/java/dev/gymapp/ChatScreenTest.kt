package dev.gymapp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        (ApplicationProvider.getApplicationContext() as PrTracksApplication).authRepository.signOut()
    }

    @Test
    fun chatScreen_displaysDashboardButton() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithContentDescription("Dashboard").assertExists()
    }

    @Test
    fun chatScreen_displaysRecordVoiceButton() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithContentDescription("Record voice").assertExists()
    }

    @Test
    fun chatScreen_dashboardButtonOpensDashboard() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        composeTestRule.onNodeWithText("Dashboard").assertExists()
    }

    /**
     * Proves sample tap works: dev sign-in → tap sample → "Sending…" appears immediately.
     * No backend required. Use to isolate tap/UI issues from API issues.
     */
    @Test
    fun chatScreen_devSignIn_tapSample_showsSending() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithText("Close grip bench 130")
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Sending…"), 5000L)
    }

    /**
     * Full cycle: dev sign-in → send sample audio → backend processes → assistant response appears.
     * Requires: backend running at base.url (local.properties), GYM_DEV_MODE=true.
     * Run BackendConnectivityTest first to verify cert trust. See scripts/test-backend-chat.sh.
     */
    @Test
    fun chatScreen_devSignIn_sendSample_receivesResponse() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithText("Close grip bench 130")
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntilAtLeastOneExists(
            hasContentDescription("Assistant message:", substring = true),
            45000L
        )
    }
}
