package com.theblankstate.epmanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.theblankstate.epmanager.data.model.Account

/**
 * Reusable account selector dropdown component.
 * Used in payment dialogs for goal contributions, debt payments, settlements, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account?) -> Unit,
    label: String = "Account",
    isOptional: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    if (accounts.isEmpty()) return
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedAccount?.name ?: if (isOptional) "Select Account (optional)" else "Select Account",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (isOptional) {
                DropdownMenuItem(
                    text = { Text("No Account") },
                    onClick = {
                        onAccountSelected(null)
                        expanded = false
                    }
                )
            }
            
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { 
                        Text("${account.name} (${formatAmount(account.balance, "₹")})")
                    },
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    }
                )
            }
        }
    }
}
