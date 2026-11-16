package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `app name and games asset load properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FrameCheck", appName)

    val json = context.assets.open("games.json").bufferedReader().use { it.readText() }
    val games = GameRepository(context).parseGamesJson(json)
    assertTrue("Expected at least 10 games in games.json", games.size >= 10)
    assertTrue(games.any { it.name == "Genshin Impact" })
    assertTrue(games.any { it.name == "PUBG Mobile" })
  }

  @Test
  fun `main screen shows hardware deep scan tiles, sign in button, and community verdict sheet`() {
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithTag("hardware_telemetry_card")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hardware_telemetry_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cpu_cores_tile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("thermal_tile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("total_ram_tile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("storage_speed_tile").assertIsDisplayed()

    composeTestRule.onNodeWithTag("game_search_input").performTextInput("Genshin")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("game_card_genshin_impact").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Genshin Impact").assertIsDisplayed()

    composeTestRule.onNodeWithTag("game_card_genshin_impact").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("community_verdict_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_detail_button").performScrollTo().assertIsDisplayed()
  }
}
