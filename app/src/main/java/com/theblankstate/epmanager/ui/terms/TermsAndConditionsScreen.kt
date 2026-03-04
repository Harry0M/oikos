package com.theblankstate.epmanager.ui.terms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.theblankstate.epmanager.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    showAcceptDecline: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    
    // Check if user has scrolled near bottom
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 100) {
            hasScrolledToBottom = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    if (!showAcceptDecline) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showAcceptDecline) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasScrolledToBottom
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("I Accept the Terms & Conditions")
                        }
                        
                        if (!hasScrolledToBottom) {
                            Text(
                                text = "Please scroll to read all terms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.xs)
                            )
                        }
                        
                        TextButton(
                            onClick = onDecline,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Decline & Exit")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // App Header
            Text(
                text = "Oikos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Premium expense tracking. ₹0.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            
            // Version Info
            Text(
                text = "Terms of Service v1.0",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Last updated: December 2024",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Section 1: Acceptance
            TermsSection(
                icon = Icons.Default.CheckCircle,
                title = "1. Acceptance of Terms",
                content = """
                    By using Oikos ("the App"), you agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the App.
                    
                    Your acceptance of these terms is recorded with a timestamp for compliance purposes. You must accept these terms before using the app.
                """.trimIndent()
            )
            
            // Section 2: Privacy & Data
            TermsSection(
                icon = Icons.Default.Lock,
                title = "2. Privacy & Data Storage",
                content = """
                    LOCAL-FIRST STORAGE
                    All your financial data including transactions, budgets, recurring expenses, savings goals, and account balances are stored locally on your device. We do not have access to your personal financial information.
                    
                    NO DATA SELLING
                    We do not sell, share, rent, or monetize your personal data in any way. Your financial information is yours alone.
                    
                    CLOUD SYNC (Optional)
                    If you choose to sign in with Google, your data may be synced to Google Firebase for backup purposes. This enables:
                    • Cross-device synchronization
                    • Data recovery after reinstallation
                    • Sharing split expenses with friends
                    
                    This is entirely optional and under your control. You can use the app without signing in.
                    
                    ANONYMOUS ANALYTICS
                    We may collect anonymous, non-personally-identifiable usage analytics to improve the app experience. No financial data is ever collected or transmitted.
                """.trimIndent()
            )
            
            // Section 3: Security
            TermsSection(
                icon = Icons.Default.Security,
                title = "3. Security Notice",
                content = """
                    DEVICE SECURITY
                    The security of your data depends on your device's security. We strongly recommend:
                    • Using device lock (PIN, pattern, fingerprint, or face unlock)
                    • Keeping your device software up to date
                    • Only installing apps from trusted sources
                    
                    ⚠️ THIRD-PARTY APPS WARNING
                    Malicious applications installed on your device may potentially access data stored by Oikos. We are not responsible for data breaches caused by:
                    • Third-party malware or spyware
                    • Compromised or rooted devices
                    • Unauthorized physical access to your device
                    
                    ENCRYPTION
                    Data stored locally is protected by Android's security measures. Cloud-synced data is encrypted in transit using industry-standard TLS.
                """.trimIndent()
            )
            
            // Section 4: SMS Permissions
            TermsSection(
                icon = Icons.Default.PhoneAndroid,
                title = "4. SMS Permissions",
                content = """
                    AUTO-TRACKING FEATURE
                    Oikos can optionally read your SMS messages to automatically detect and record transactions from bank notifications. This is a convenience feature.
                    
                    LOCAL PROCESSING ONLY
                    SMS messages are processed entirely on your device. Message content is:
                    • Never sent to any server
                    • Never stored beyond parsing
                    • Never shared with any third party
                    
                    MANUAL ALTERNATIVE
                    You can use the app fully without granting SMS permissions. Manual expense entry is always available.
                    
                    SUPPORTED BANKS
                    The SMS parsing feature works with most major Indian banks.
                """.trimIndent()
            )
            
            // Section 5: Disclaimers
            TermsSection(
                icon = Icons.Default.Warning,
                title = "5. Disclaimers & Limitations",
                content = """
                    NO WARRANTY
                    The App is provided "as is" and "as available" without any warranties of any kind, either express or implied, including but not limited to merchantability, fitness for a particular purpose, or non-infringement.
                    
                    DATA LOSS DISCLAIMER
                    We are NOT responsible for any loss of data due to:
                    • Device failure, damage, or theft
                    • App uninstallation (data is lost if not synced)
                    • Operating system updates or factory reset
                    • Cyberattacks, hacking, or malware
                    • User error or accidental deletion
                    • Cloud service outages
                    • Any other circumstances beyond our control
                    
                    FINANCIAL ADVICE
                    Oikos is a tracking and management tool only. It does NOT provide:
                    • Financial advice or recommendations
                    • Investment guidance
                    • Tax advice or calculations
                    • Legal counsel
                    
                    Please consult qualified professionals for such services.
                    
                    ACCURACY
                    While we strive for accuracy in transaction detection and calculations, we are not responsible for errors in:
                    • Automatic SMS transaction parsing
                    • Currency conversions
                    • Budget calculations
                    • Split expense calculations
                """.trimIndent()
            )
            
            // Section 6: Open Source
            TermsSection(
                icon = Icons.Default.Lock,
                title = "6. Open Source License",
                content = """
                    APACHE LICENSE 2.0
                    Oikos is open-source software licensed under the Apache License, Version 2.0.
                    
                    You may obtain a copy of the License at:
                    https://www.apache.org/licenses/LICENSE-2.0
                    
                    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                    
                    BUILT WITH AI
                    This app was developed with the assistance of AI assistant tools.
                    
                    THIRD-PARTY LIBRARIES
                    The app uses various open-source libraries, each with their own licenses. A full list of dependencies and their licenses is available in the app's About section.
                    
                    CONTRIBUTIONS
                    Contributions to the project are welcome under the same Apache 2.0 license terms.
                """.trimIndent()
            )
            
            // Section 7: User Responsibilities
            TermsSection(
                icon = Icons.Default.CheckCircle,
                title = "7. User Responsibilities",
                content = """
                    By using Oikos, you agree to:
                    • Provide accurate information when creating accounts
                    • Keep your login credentials secure
                    • Not use the app for any illegal purposes
                    • Not attempt to reverse engineer or tamper with the app
                    • Backup your data regularly if using without cloud sync
                """.trimIndent()
            )
            
            // Section 8: Changes to Terms
            TermsSection(
                icon = Icons.Default.CheckCircle,
                title = "8. Changes to Terms",
                content = """
                    We reserve the right to modify these Terms at any time. When we make changes:
                    • Significant changes will be notified within the app
                    • The version number will be updated
                    • Your continued use after changes constitutes acceptance
                    
                    We recommend reviewing these terms periodically for updates.
                """.trimIndent()
            )
            
            // Section 9: Contact & Support
            TermsSection(
                icon = Icons.Default.Email,
                title = "9. Contact & Support",
                content = """
                    For support, questions, bug reports, feature requests, or privacy inquiries, please contact us:
                    
                    📧 Email: theblankstateteam@gmail.com
                    
                    We aim to respond to all queries within 48-72 hours.
                    
                    For security vulnerabilities, please email with "SECURITY" in the subject line for priority handling.
                """.trimIndent()
            )
            
            // Section 10: Governing Law
            TermsSection(
                icon = Icons.Default.Security,
                title = "10. Governing Law",
                content = """
                    These Terms shall be governed by and construed in accordance with the laws of India, without regard to its conflict of law provisions.
                    
                    Any disputes arising from these terms shall be subject to the exclusive jurisdiction of the courts in Ahmedabad, Gujarat, India.
                """.trimIndent()
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            
            // Final Agreement
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Agreement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "By clicking 'I Accept', you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Extra space at bottom
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun TermsSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
