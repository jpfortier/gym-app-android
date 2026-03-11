package dev.gymapp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signOut() {
        (ApplicationProvider.getApplicationContext() as PrTracksApplication).authRepository.signOut()
    }

    private fun signInAndOpenDashboard() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
    }

    @Test
    fun dashboardScreen_displaysTitle() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithText("Dashboard").assertExists()
    }

    @Test
    fun dashboardScreen_displaysBackButton() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun dashboardScreen_displaysLogOutButton() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithContentDescription("Log out").assertExists()
    }

    @Test
    fun dashboardScreen_displaysLatestPrTile() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithText("Latest PR").assertExists()
    }

    @Test
    fun dashboardScreen_displaysStreakTile() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithText("Streak").assertExists()
    }

    @Test
    fun dashboardScreen_backNavigatesToChat() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 5000L)
    }

    @Test
    fun dashboardScreen_displaysVersion() {
        signInAndOpenDashboard()
        composeTestRule.onNodeWithText("v${BuildConfig.VERSION_NAME}", substring = true).assertExists()
    }

    @Test
    fun dashboardScreen_updateSection_appears() {
        signInAndOpenDashboard()
        composeTestRule.waitUntilAtLeastOneExists(
            hasContentDescription("Up to date").or(hasContentDescription("Check for update")),
            15000L
        )
    }
}
