package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a user-defined rule for automatic transaction categorization.
 * If a transaction's merchant name, UPI ID, or sender contains the pattern,
 * it will be automatically assigned to the specified category.
 */
@Entity(
    tableName = "categorization_rules",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class CategorizationRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val pattern: String, // The text to match (e.g., "swiggy", "upi@hdfc")
    val categoryId: String,
    val createdAt: Long = System.currentTimeMillis()
)
