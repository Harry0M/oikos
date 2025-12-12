package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a savings goal (vacation, emergency fund, gadget, etc.)
 */
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val icon: String = "Savings", // Material icon name
    val color: Long = 0xFF22C55E, // Default green
    val targetDate: Long? = null, // Optional deadline
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (targetAmount > 0) (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    val remaining: Double
        get() = (targetAmount - savedAmount).coerceAtLeast(0.0)
    
    val isOnTrack: Boolean
        get() {
            if (targetDate == null) return true
            val now = System.currentTimeMillis()
            if (now >= targetDate) return isCompleted
            
            val totalDuration = targetDate - createdAt
            val elapsed = now - createdAt
            val expectedProgress = elapsed.toFloat() / totalDuration.toFloat()
            return progress >= expectedProgress
        }
}

/**
 * Preset goal icons and colors
 */
object GoalPresets {
    val presets = listOf(
        GoalPreset("✈️ Vacation", "Flight", 0xFF3B82F6),
        GoalPreset("🚗 Car", "DirectionsCar", 0xFF8B5CF6),
        GoalPreset("🏠 Home", "Home", 0xFFF59E0B),
        GoalPreset("📱 Gadget", "PhoneAndroid", 0xFF10B981),
        GoalPreset("🎓 Education", "School", 0xFF6366F1),
        GoalPreset("💍 Wedding", "Favorite", 0xFFEC4899),
        GoalPreset("🏥 Emergency", "LocalHospital", 0xFFEF4444),
        GoalPreset("🎁 Gift", "CardGiftcard", 0xFFF97316),
        GoalPreset("💰 General", "Savings", 0xFF22C55E)
    )
}

data class GoalPreset(
    val label: String,
    val icon: String,
    val color: Long
)
