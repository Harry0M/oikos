package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Records when a user accepts the Terms & Conditions.
 * This is stored locally for legal compliance.
 */
@Entity(tableName = "terms_acceptances")
data class TermsAcceptance(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val termsVersion: String, // Version of T&C accepted (e.g., "1.0")
    val acceptedAt: Long = System.currentTimeMillis(),
    val userId: String? = null, // Firebase UID if logged in, null if guest
    val deviceId: String? = null // Device identifier for tracking
)
