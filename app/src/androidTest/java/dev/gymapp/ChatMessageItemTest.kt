package dev.gymapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.gymapp.ui.chat.ChatMessage
import dev.gymapp.ui.chat.ChatRole
import dev.gymapp.ui.screens.ChatMessageItem
import dev.gymapp.ui.theme.PrTracksTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ChatMessageItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun chatMessageItem_userMessage_displaysText() {
        composeTestRule.setContent {
            PrTracksTheme {
                ChatMessageItem(
                    msg = ChatMessage("1", ChatRole.USER, "bench 135x8"),
                    showSpinner = false
                )
            }
        }
        composeTestRule.onNodeWithText("bench 135x8").assertExists()
    }

    @Test
    fun chatMessageItem_assistantMessage_displaysMarkdown() {
        composeTestRule.setContent {
            PrTracksTheme {
                ChatMessageItem(
                    msg = ChatMessage("2", ChatRole.ASSISTANT, "Got it, that's in."),
                    showSpinner = false
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Assistant message: Got it, that's in.").assertExists()
    }

    @Test
    fun chatMessageItem_voiceMessage_displaysVoiceMessage() {
        composeTestRule.setContent {
            PrTracksTheme {
                ChatMessageItem(
                    msg = ChatMessage("3", ChatRole.USER, "[Voice message]"),
                    showSpinner = false
                )
            }
        }
        composeTestRule.onNodeWithText("Voice message").assertExists()
    }

    @Test
    fun chatMessageItem_sending_displaysSending() {
        composeTestRule.setContent {
            PrTracksTheme {
                ChatMessageItem(
                    msg = ChatMessage("4", ChatRole.USER, "[Voice message]"),
                    showSpinner = true
                )
            }
        }
        composeTestRule.onNodeWithText("Sending…").assertExists()
    }
}
