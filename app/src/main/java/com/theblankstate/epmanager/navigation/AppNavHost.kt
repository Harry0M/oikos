package com.theblankstate.epmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.theblankstate.epmanager.ui.add.AddTransactionScreen
import com.theblankstate.epmanager.ui.ai.AIInsightsScreen
import com.theblankstate.epmanager.ui.analytics.AnalyticsScreen
import com.theblankstate.epmanager.ui.auth.LoginScreen
import com.theblankstate.epmanager.ui.auth.ProfileScreen
import com.theblankstate.epmanager.ui.auth.SignUpScreen
import com.theblankstate.epmanager.ui.budget.BudgetScreen
import com.theblankstate.epmanager.ui.categories.CategoriesScreen
import com.theblankstate.epmanager.ui.accounts.AccountsScreen
import com.theblankstate.epmanager.ui.export.ExportScreen
import com.theblankstate.epmanager.ui.goals.GoalsScreen
import com.theblankstate.epmanager.ui.home.HomeScreen
import com.theblankstate.epmanager.ui.notifications.NotificationSettingsScreen
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
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
                }
            )
        }
        
        composable(Screen.Transactions.route) {
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
            AnalyticsScreen()
        }
        
        composable(Screen.Budget.route) {
            BudgetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Recurring.route) {
            RecurringScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
        
        composable(Screen.AIInsights.route) {
            AIInsightsScreen(
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
                onNavigateToAIInsights = {
                    navController.navigate(Screen.AIInsights.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
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
        
        // Auth screens
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onLoginSuccess = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    navController.popBackStack(Screen.Settings.route, inclusive = false)
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
