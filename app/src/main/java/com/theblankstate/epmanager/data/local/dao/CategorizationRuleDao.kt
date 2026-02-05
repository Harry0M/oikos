package com.theblankstate.epmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.theblankstate.epmanager.data.model.CategorizationRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationRuleDao {
    @Query("SELECT * FROM categorization_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<CategorizationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategorizationRule)

    @Delete
    suspend fun deleteRule(rule: CategorizationRule)

    /**
     * Find a rule where the text contains the pattern.
     * Use this to check if a transaction details matches any rule.
     */
    @Query("SELECT * FROM categorization_rules WHERE :text LIKE '%' || pattern || '%' LIMIT 1")
    suspend fun findRuleForText(text: String): CategorizationRule?

    @Query("SELECT * FROM categorization_rules WHERE pattern = :pattern LIMIT 1")
    suspend fun getRuleByPattern(pattern: String): CategorizationRule?
}
