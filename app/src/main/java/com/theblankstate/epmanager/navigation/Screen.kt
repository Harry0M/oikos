package com.theblankstate.epmanager.navigation

/**
 * Screen routes for navigation
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Transactions : Screen("transactions")
    data object AddTransaction : Screen("add_transaction")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    data object Accounts : Screen("accounts")
    data object Budget : Screen("budget")
    data object Recurring : Screen("recurring")
    data object Export : Screen("export")
    data object Goals : Screen("goals")
    data object NotificationSettings : Screen("notification_settings")
    data object AIInsights : Screen("ai_insights")
    data object Subscriptions : Screen("subscriptions")
    data object Split : Screen("split")
    data object SmsSettings : Screen("sms_settings")
    
    // Auth screens
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Profile : Screen("profile")
}
