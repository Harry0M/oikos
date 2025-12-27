package com.theblankstate.epmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.theblankstate.epmanager.ui.add.AddTransactionScreen
import com.theblankstate.epmanager.ui.analytics.AnalyticsScreen
import com.theblankstate.epmanager.ui.analytics.AnalyticsDetailScreen
import com.theblankstate.epmanager.ui.analytics.AnalyticsDetailType
import com.theblankstate.epmanager.ui.auth.ProfileScreen
import com.theblankstate.epmanager.ui.budget.BudgetScreen
import com.theblankstate.epmanager.ui.categories.CategoriesScreen
import com.theblankstate.epmanager.ui.accounts.AccountsScreen
import com.theblankstate.epmanager.ui.debt.DebtScreen
import com.theblankstate.epmanager.ui.export.ExportScreen
import com.theblankstate.epmanager.ui.friends.FriendsScreen
import com.theblankstate.epmanager.ui.goals.GoalsScreen
import com.theblankstate.epmanager.ui.home.HomeScreen
import com.theblankstate.epmanager.ui.notifications.NotificationSettingsScreen
import com.theblankstate.epmanager.ui.notifications.NotificationCenterScreen
import com.theblankstate.epmanager.ui.onboarding.OnboardingScreen
import com.theblankstate.epmanager.ui.recurring.RecurringScreen
import com.theblankstate.epmanager.ui.settings.SettingsScreen
import com.theblankstate.epmanager.ui.sms.SmsSettingsScreen
import com.theblankstate.epmanager.ui.split.SplitScreen
import com.theblankstate.epmanager.ui.subscriptions.SubscriptionsScreen
import com.theblankstate.epmanager.ui.transactions.TransactionsScreen
import com.theblankstate.epmanager.ui.transactions.TransactionDetailScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    hasCompletedOnboarding: Boolean,
    modifier: Modifier = Modifier
) {
    val startDestination = if (hasCompletedOnboarding) Screen.Home.route else Screen.Onboarding.route
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToTransactionDetail = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToRecurring = {
                    navController.navigate(Screen.Recurring.route)
                },
                onNavigateToSubscriptions = {
                    navController.navigate(Screen.Subscriptions.route)
                },
                onNavigateToGoals = {
                    navController.navigate(Screen.Goals.route)
                },
                onNavigateToDebtCredit = {
                    navController.navigate(Screen.DebtCredit.route)
                },
                onNavigateToSplit = {
                    navController.navigate(Screen.Split.route)
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToHistory = { type, id ->
                    navController.navigate("${Screen.Transactions.route}?type=$type&id=$id")
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationCenter.route)
                }
            )
        }
        
        composable(
            route = "${Screen.Transactions.route}?type={type}&id={id}",
            arguments = listOf(
                navArgument("type") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("id") { 
                    type = NavType.StringType 
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TransactionsScreen(
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTransactionDetail = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                }
            )
        }
        
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            TransactionDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = {
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }
        
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) {
            com.theblankstate.epmanager.ui.edit.EditTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionSaved = {
                    // Pop back to transaction detail or transactions list
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionSaved = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateToDetail = { detailType ->
                    navController.navigate(Screen.AnalyticsDetail.createRoute(detailType.name))
                }
            )
        }
        
        composable(
            route = Screen.AnalyticsDetail.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val typeString = backStackEntry.arguments?.getString("type") ?: ""
            val detailType = try {
                AnalyticsDetailType.valueOf(typeString)
            } catch (e: IllegalArgumentException) {
                AnalyticsDetailType.FINANCIAL_HEALTH
            }
            AnalyticsDetailScreen(
                detailType = detailType,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Budget.route) {
            BudgetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = { categoryId ->
                    navController.navigate("${Screen.Transactions.route}?type=CATEGORY&id=$categoryId")
                }
            )
        }
        
        composable(Screen.Recurring.route) {
            RecurringScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = { recurringId ->
                    navController.navigate("${Screen.Transactions.route}?type=RECURRING&id=$recurringId")
                }
            )
        }
        
        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Goals.route) {
            GoalsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = { goalId ->
                    navController.navigate("${Screen.Transactions.route}?type=GOAL&id=$goalId")
                }
            )
        }
        
        composable(Screen.NotificationCenter.route) {
            NotificationCenterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToBudget = {
                    navController.navigate(Screen.Budget.route)
                },
                onNavigateToRecurring = {
                    navController.navigate(Screen.Recurring.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                },
                onNavigateToGoals = {
                    navController.navigate(Screen.Goals.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToSubscriptions = {
                    navController.navigate(Screen.Subscriptions.route)
                },
                onNavigateToSplit = {
                    navController.navigate(Screen.Split.route)
                },
                onNavigateToSmsSettings = {
                    navController.navigate(Screen.SmsSettings.route)
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route)
                },
                onNavigateToDebtCredit = {
                    navController.navigate(Screen.DebtCredit.route)
                },
                onNavigateToThemeCustomization = {
                    navController.navigate(Screen.ThemeCustomization.route)
                }
            )
        }
        
        composable(Screen.ThemeCustomization.route) {
            com.theblankstate.epmanager.ui.settings.ThemeCustomizationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Friends screen
        composable(Screen.Friends.route) {
            FriendsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Categories and Accounts screens
        composable(Screen.Categories.route) {
            CategoriesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Accounts.route) {
            AccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Split.route) {
            SplitScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SmsSettings.route) {
            SmsSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.DebtCredit.route) {
            DebtScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = { debtId ->
                    navController.navigate("${Screen.Transactions.route}?type=DEBT&id=$debtId")
                }
            )
        }
        
        // Auth screens
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.popBackStack()
                }
            )
        }
    }
}
