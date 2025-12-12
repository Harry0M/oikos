# SMS Parsing & Bank Account Linking - Solution Documentation

## Overview

This document explains how the expense manager handles SMS parsing for auto-detecting transactions and linking them to the correct user accounts.

## The Problem

1. **Generic Accounts**: Default accounts (Cash, Bank Account, UPI, Credit Card) are not linked to actual bank accounts
2. **No Account Matching**: When SMS arrives with transaction info, there's no way to know which account it belongs to
3. **Multiple Bank Accounts**: Users can have multiple accounts from the same bank (e.g., 2 HDFC accounts, 1 SBI account)

## The Solution

### 1. Enhanced Account Model

The `Account` model now includes bank linking fields:

```kotlin
data class Account(
    // ... existing fields ...
    
    // Bank linking fields for SMS parsing
    val accountNumber: String? = null,    // Last 4 digits (e.g., "1234")
    val bankCode: String? = null,         // Bank identifier (e.g., "HDFC", "SBI")
    val linkedSenderIds: String? = null,  // SMS sender IDs (e.g., "HDFCBK,HDFC")
    val isLinked: Boolean = false         // Whether this account is linked
)
```

### 2. Bank Registry

A comprehensive registry of Indian banks with their SMS sender ID patterns:

```kotlin
object BankRegistry {
    val banks = listOf(
        BankInfo("HDFC Bank", "HDFC", listOf("HDFCBK", "HDFC"), 0xFF004B8D),
        BankInfo("State Bank of India", "SBI", listOf("SBIINB", "SBIPSG"), 0xFF22409A),
        BankInfo("ICICI Bank", "ICICI", listOf("ICICIB", "ICICI"), 0xFFF58220),
        // ... more banks
    )
}
```

### 3. Matching Algorithm

When an SMS arrives:

1. **Parse SMS**: Extract amount, transaction type, bank, account hint (last 4 digits)
2. **Score Accounts**: Each linked account gets a score:
   - Sender ID match: +40 points
   - Bank code match: +30 points
   - Account number match (last 4 digits): +50 points
3. **Select Best Match**: Account with highest score ≥50 is selected

```kotlin
fun getMatchScore(senderId: String, accountHint: String?): Int {
    var score = 0
    
    if (linkedSenderIds matches senderId) score += 40
    if (bankCode matches senderId) score += 30
    if (accountNumber == accountHint) score += 50
    
    return score.coerceAtMost(100)
}
```

### 4. Confidence Levels

- **HIGH (80-100)**: Auto-assign to account, mark as "[Auto-linked]"
- **MEDIUM (50-79)**: Auto-assign but flag for review
- **LOW (20-49)**: Don't auto-assign, suggest account
- **NONE (<20)**: Save as uncategorized

## How to Link Accounts (User Flow)

### Option 1: Create New Linked Account
```kotlin
accountRepository.createLinkedAccount(
    name = "HDFC Salary Account",
    bankCode = "HDFC",
    accountNumber = "1234",  // Last 4 digits
    type = AccountType.BANK
)
```

### Option 2: Link Existing Account
```kotlin
accountRepository.linkAccount(
    accountId = existingAccount.id,
    bankCode = "HDFC",
    accountNumber = "1234",
    senderIds = listOf("HDFCBK", "HDFC")
)
```

## Open Source Approaches & Libraries

### 1. SMS Parsing Libraries

#### a) **Expense SMS Parser** (Recommended)
- GitHub: `nickmeinhold/expense_sms_parser` (Dart/Flutter)
- Pattern-based regex parsing for multiple banks
- Approach: Use similar regex patterns adapted for Kotlin

#### b) **Android SMS Reader Patterns**
Common open source patterns used:
```kotlin
// Amount patterns
"(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)"

// Account number patterns  
"(?:a/c|account|card|ac)[\\s*:]*[Xx*]*([0-9]{4})"

// Debit keywords
listOf("debited", "spent", "paid", "purchase", "withdrawn")

// Credit keywords
listOf("credited", "received", "deposited", "refund", "cashback")
```

### 2. Bank Sender ID Databases

Open source bank sender ID databases:
- India: HDFCBK, SBIINB, ICICIB, AXISBK, etc.
- Format: Usually 6-letter codes (XX-XXXXXX)

### 3. Machine Learning Approaches

For more advanced parsing, consider:

#### a) **NER (Named Entity Recognition)**
- Libraries: Stanford NLP, spaCy
- Extract: Amount, Merchant, Date entities from unstructured text

#### b) **Gemini/GPT for Parsing**
```kotlin
// Use AI to parse complex SMS formats
suspend fun parseWithAI(smsBody: String): ParsedTransaction {
    val prompt = "Extract transaction details from: $smsBody"
    return geminiApi.generateContent(prompt)
}
```

### 4. Account Matching Strategies

#### Strategy 1: Rule-Based (Current Implementation)
- Simple, fast, predictable
- Works for 90%+ of cases
- Easy to add new banks

#### Strategy 2: ML-Based Classification
```kotlin
// Train a classifier on SMS -> Account mapping
class AccountClassifier {
    fun predict(sms: String, accounts: List<Account>): Account
}
```

#### Strategy 3: User Learning
- Track user corrections
- Improve matching over time
- "You usually assign HDFC SMS to 'HDFC Salary'"

## Handling Edge Cases

### 1. Multiple Accounts Same Bank
When user has multiple accounts from same bank (e.g., 2 HDFC accounts):
- **Solution**: Use last 4 digits for disambiguation
- SMS: "HDFC Bank: Rs.5000 debited from A/c **1234"
- Match: Account with `bankCode="HDFC"` AND `accountNumber="1234"`

### 2. UPI Transactions
UPI transactions may come from:
- Bank SMS (HDFCBK)
- UPI app SMS (GPAY, PHONEPE)

**Solution**: Create separate linked account for each UPI source

### 3. Credit Card vs Debit Card
Same bank, different card types:
- Parse card type from SMS ("Credit Card", "Debit Card")
- Store as separate accounts with `type = CREDIT_CARD` vs `BANK`

### 4. New Bank Not Recognized
When SMS sender not in registry:
- Save transaction as uncategorized
- Store sender ID for later linking
- Prompt user to create linked account

## Sample SMS Formats (India)

### HDFC Bank
```
HDFC Bank: Rs.1,234.00 debited from A/c **5678 on 11-12-24. Avl Bal: Rs.50,000.00. Info: UPI/merchant@upi
```

### SBI
```
SBI: Your A/c X1234 credited by Rs.5000.00 on 11Dec24. Avl Bal: Rs.25000.00. Ref: UTR123456789
```

### Google Pay
```
Rs.500 paid to Merchant Name via Google Pay. UPI Ref: 123456789012. For issues: gpay.app/help
```

### PhonePe
```
Paid Rs.200 to Shop Name from PhonePe. UPI Ref No. 123456789012.
```

## Future Improvements

1. **SMS Permission Request Flow**: Guide users through SMS permission
2. **Bulk Import Old SMS**: Parse historical SMS on first setup
3. **Smart Notifications**: "New transaction from unknown account. Link it now?"
4. **Export/Import Linked Accounts**: For backup/restore
5. **Bank API Integration**: Use bank APIs where available (Account Aggregator framework in India)

## Files Modified

1. `data/model/Account.kt` - Enhanced with bank linking fields and BankRegistry
2. `sms/SmsParser.kt` - Enhanced parsing with bank detection
3. `sms/AccountMatcher.kt` - New service for matching accounts
4. `sms/SmsBroadcastReceiver.kt` - Updated to use AccountMatcher
5. `data/repository/AccountRepository.kt` - New linked account operations
6. `data/local/ExpenseDatabase.kt` - Version incremented to 5

## Testing

Test with sample SMS messages:
```kotlin
val testSms = listOf(
    "HDFCBK: Rs.1000 debited from A/c **1234" to "HDFC-1234",
    "SBI: Rs.500 credited to A/c **5678" to "SBI-5678",
    "GPAY: Paid Rs.200 to Merchant" to "GPAY",
)

testSms.forEach { (sms, expectedAccount) ->
    val parsed = parser.parse(sms, sender)
    val matched = accountMatcher.findMatchingAccount(parsed, sender)
    assert(matched.name == expectedAccount)
}
```
