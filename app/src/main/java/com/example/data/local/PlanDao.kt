package com.example.data.local

import androidx.room.*
import com.example.data.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM subscription_plans WHERE isActive = 1 ORDER BY price ASC")
    fun getAllActivePlans(): Flow<List<SubscriptionPlan>>

    @Query("SELECT * FROM subscription_plans ORDER BY price ASC")
    fun getAllPlans(): Flow<List<SubscriptionPlan>>

    @Query("SELECT * FROM subscription_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): SubscriptionPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPlans(plans: List<SubscriptionPlan>)

    @Update
    suspend fun updatePlan(plan: SubscriptionPlan)

    @Delete
    suspend fun deletePlan(plan: SubscriptionPlan)

    @Query("DELETE FROM subscription_plans")
    suspend fun deleteAll()
}
