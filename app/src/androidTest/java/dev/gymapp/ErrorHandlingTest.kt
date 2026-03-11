package dev.gymapp

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.gymapp.ui.chat.ChatViewModel
import dev.gymapp.ui.screens.ChatScreen
import dev.gymapp.ui.theme.PrTracksTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ErrorHandlingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun chatScreen_errorSnackbar_detailsOpensDialog() {
        var viewModel: ChatViewModel? = null
        composeTestRule.setContent {
            PrTracksTheme {
                val app = LocalContext.current.applicationContext as PrTracksApplication
                val vm = viewModel<ChatViewModel>(
                    key = "chat_error_test",
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ChatViewModel(FakeGymApi("Test API error")).also {
                                viewModel = it
                            } as T
                        }
                    }
                )
                ChatScreen(app = app, viewModel = vm)
            }
        }
        composeTestRule.runOnIdle {
            viewModel?.sendAudio("aGVsbG8=")
        }
        composeTestRule.waitUntilAtLeastOneExists(hasText("Test API error"), 5000L)
        composeTestRule.onNodeWithText("Details").performClick()
        composeTestRule.onNodeWithText("Error details").assertExists()
    }
}
