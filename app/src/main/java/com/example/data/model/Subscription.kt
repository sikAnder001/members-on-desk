package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SubscriptionStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    PENDING_PAYMENT,
    CANCELLED
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val planId: Long,
    val planName: String,
    val price: Double,
    val discount: Double = 0.0,
    val finalAmount: Double = price - discount,
    val startDate: Long,
    val endDate: Long,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
