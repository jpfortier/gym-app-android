package dev.gymapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.gymapp.ui.screens.BottomBarMic
import dev.gymapp.ui.theme.PrTracksTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class BottomBarMicTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun bottomBarMic_idle_displaysRecordVoice() {
        composeTestRule.setContent {
            PrTracksTheme {
                BottomBarMic(isRecording = false, onVoiceTap = {})
            }
        }
        composeTestRule.onNodeWithContentDescription("Record voice").assertExists()
    }

    @Test
    fun bottomBarMic_recording_displaysStopRecording() {
        composeTestRule.setContent {
            PrTracksTheme {
                BottomBarMic(isRecording = true, onVoiceTap = {})
            }
        }
        composeTestRule.onNodeWithContentDescription("Stop recording").assertExists()
    }

    @Test
    fun bottomBarMic_click_invokesCallback() {
        val tapped = mutableListOf<Unit>()
        composeTestRule.setContent {
            PrTracksTheme {
                BottomBarMic(isRecording = false) { tapped.add(Unit) }
            }
        }
        composeTestRule.onNodeWithContentDescription("Record voice").performClick()
        composeTestRule.runOnIdle {
            assert(tapped.size == 1) { "onVoiceTap should have been invoked exactly once" }
        }
    }
}
