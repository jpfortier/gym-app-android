package dev.gymapp

import androidx.activity.ComponentActivity

/**
 * Empty Activity for composable-only tests. Does not set content in onCreate,
 * allowing tests to call composeTestRule.setContent { }.
 */
class ComposeTestActivity : ComponentActivity()
