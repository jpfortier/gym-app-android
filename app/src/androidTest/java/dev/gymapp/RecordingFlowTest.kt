package dev.gymapp

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RecordingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Before
    fun signOut() {
        (ApplicationProvider.getApplicationContext() as PrTracksApplication).authRepository.signOut()
    }

    @Test
    fun recordingFlow_tapMic_showsStopRecording() {
        composeTestRule.onNodeWithContentDescription("Dev sign-in").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Record voice"), 10000L)
        composeTestRule.onNodeWithContentDescription("Record voice").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Stop recording"), 3000L)
    }
}
