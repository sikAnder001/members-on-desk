package com.example.data.local

import androidx.room.*
import com.example.data.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE memberId = :memberId ORDER BY endDate DESC")
    fun getSubscriptionsForMember(memberId: Long): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE memberId = :memberId ORDER BY endDate DESC LIMIT 1")
    suspend fun getLatestSubscriptionForMember(memberId: Long): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}
