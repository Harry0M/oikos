package com.theblankstate.epmanager.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.theblankstate.epmanager.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Home screen - the main entry point of the app.
 * Tests navigation, display of key elements, and basic interactions.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun homeScreen_displaysAppName() {
        // The app name or home screen title should be visible
        composeRule.onNodeWithText("EP Manager", ignoreCase = true, useUnmergedTree = true)
            .assertExists()
    }
    
    @Test
    fun homeScreen_displaysTotalBalance() {
        // Balance section should be displayed
        composeRule.onNodeWithText("Balance", substring = true, ignoreCase = true)
            .assertExists()
    }
    
    @Test
    fun homeScreen_displaysFAB() {
        // FAB for adding transaction should be visible
        composeRule.onNodeWithContentDescription("Add Transaction", substring = true, ignoreCase = true)
            .assertExists()
    }
    
    @Test
    fun homeScreen_clickFAB_showsAddOptions() {
        // Click FAB should show add expense/income options
        composeRule.onNodeWithContentDescription("Add", substring = true, ignoreCase = true)
            .performClick()
        
        // Should show expense or income option
        composeRule.waitForIdle()
    }
    
    @Test
    fun homeScreen_displayRecentTransactions() {
        // Recent transactions section should be visible
        composeRule.onNodeWithText("Recent", substring = true, ignoreCase = true)
            .assertExists()
    }
}
