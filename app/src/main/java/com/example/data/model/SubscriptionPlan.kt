package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val durationDays: Int = 30,
    val price: Double = 499.0,
    val description: String = "",
    val businessType: BusinessType = BusinessType.GYM,
    val isActive: Boolean = true
)
