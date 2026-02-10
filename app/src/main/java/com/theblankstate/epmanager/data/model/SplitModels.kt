package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Plan types for expense groups (Trip, Groceries, etc.)
 */
enum class PlanType(val displayName: String, val emoji: String) {
    TRIP("Trip", "✈️"),
    VACATION("Vacation", "🏖️"),
    GROCERIES("Groceries", "🛒"),
    STUDY("Study Expenses", "📚"),
    SCHOOL_BAG("School Bag", "🎒"),
    ROOMMATES("Roommates", "🏠"),
    EVENT("Event", "🎉"),
    FOOD("Food & Dining", "🍕"),
    OFFICE("Office", "💼"),
    SPORTS("Sports", "⚽"),
    GAMING("Gaming", "🎮"),
    PARTY("Party", "🎊"),
    WEDDING("Wedding", "💒"),
    GIFTS("Gifts", "🎁"),
    HOME("Home", "🏡"),
    CAR_POOL("Car Pool", "🚗"),
    PROJECT("Project", "📋"),
    OTHER("Other", "📁")
}

/**
 * Represents a group for expense splitting (e.g., "Roommates", "Trip to Goa")
 */
@Entity(tableName = "split_groups")
data class SplitGroup(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val emoji: String = "👥", // Group icon
    val planType: PlanType = PlanType.OTHER, // Type of plan (Trip, Groceries, etc.)
    val enableSplit: Boolean = true, // Whether splitting is enabled for this group
    val budget: Double? = null, // Optional overall budget for the group
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a member in a split group
 */
@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(
            entity = SplitGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class GroupMember(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val linkedUserId: String? = null, // Firebase UID if linked to real user
    val isCurrentUser: Boolean = false, // "You"
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a split expense
 */
@Entity(
    tableName = "split_expenses",
    foreignKeys = [
        ForeignKey(
            entity = SplitGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMember::class,
            parentColumns = ["id"],
            childColumns = ["paidById"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("groupId"),
        Index("paidById")
    ]
)
data class SplitExpense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val description: String,
    val totalAmount: Double,
    val paidById: String?, // Who paid
    val splitType: SplitType = SplitType.EQUAL,
    val enableSplit: Boolean = true, // Whether this expense should be split
    val includedMemberIds: String? = null, // Comma-separated member IDs (null = all members)
    val customUserShare: Double? = null, // Custom share for current user (overrides auto-calc)
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class SplitType {
    EQUAL,      // Split equally among all members
    EXACT,      // Exact amounts for each member
    PERCENTAGE, // Percentage-based split
    SHARES      // Split by shares (e.g., 2:1:1)
}

/**
 * Represents individual share in a split expense
 */
@Entity(
    tableName = "expense_shares",
    foreignKeys = [
        ForeignKey(
            entity = SplitExpense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMember::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("expenseId"),
        Index("memberId")
    ]
)
data class ExpenseShare(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val expenseId: String,
    val memberId: String,
    val shareAmount: Double,    // Amount this member owes
    val sharePercentage: Double? = null,
    val shareCount: Int? = null  // For shares-based split
)

/**
 * Represents a settlement payment between members
 */
@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = SplitGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMember::class,
            parentColumns = ["id"],
            childColumns = ["fromMemberId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMember::class,
            parentColumns = ["id"],
            childColumns = ["toMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("groupId"),
        Index("fromMemberId"),
        Index("toMemberId")
    ]
)
data class Settlement(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val fromMemberId: String,  // Who paid
    val toMemberId: String,    // Who received
    val amount: Double,
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
)

/**
 * Represents the balance between two members
 */
data class MemberBalance(
    val member: GroupMember,
    val balance: Double // Positive = they owe you, Negative = you owe them
)

/**
 * Group with its members for display
 */
data class SplitGroupWithMembers(
    val group: SplitGroup,
    val members: List<GroupMember>,
    val totalExpenses: Double = 0.0,
    val yourBalance: Double = 0.0
)
