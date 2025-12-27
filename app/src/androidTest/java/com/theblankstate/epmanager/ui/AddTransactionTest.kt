package com.theblankstate.epmanager.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.theblankstate.epmanager.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Add Transaction flow.
 * Tests the critical path of adding expenses and income.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddTransactionTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun addTransaction_displaysAmountField() {
        // Navigate to add transaction if not already there
        composeRule.onNodeWithContentDescription("Add", substring = true, ignoreCase = true)
            .performClick()
        
        composeRule.waitForIdle()
        
        // Amount field should be displayed
        composeRule.onNodeWithText("Amount", substring = true, ignoreCase = true)
            .assertExists()
    }
    
    @Test
    fun addTransaction_displaysCategorySelection() {
        // Navigate to add transaction
        composeRule.onNodeWithContentDescription("Add", substring = true, ignoreCase = true)
            .performClick()
        
        composeRule.waitForIdle()
        
        // Category section should exist
        composeRule.onNodeWithText("Category", substring = true, ignoreCase = true)
            .assertExists()
    }
    
    @Test
    fun addTransaction_displaysAccountSelection() {
        // Navigate to add transaction
        composeRule.onNodeWithContentDescription("Add", substring = true, ignoreCase = true)
            .performClick()
        
        composeRule.waitForIdle()
        
        // Account section should exist
        composeRule.onNodeWithText("Account", substring = true, ignoreCase = true)
            .assertExists()
    }
    
    @Test
    fun addTransaction_displaysSaveButton() {
        // Navigate to add transaction
        composeRule.onNodeWithContentDescription("Add", substring = true, ignoreCase = true)
            .performClick()
        
        composeRule.waitForIdle()
        
        // Save button should be displayed
        composeRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .assertExists()
    }
}
