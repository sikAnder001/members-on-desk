package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BusinessType {
    GYM, LIBRARY
}

@Entity(tableName = "business")
data class Business(
    @PrimaryKey val id: Int = 1,
    val name: String = "Members on Desk",
    val type: BusinessType = BusinessType.GYM,
    val ownerName: String = "Business Owner",
    val phone: String = "+91 98765 43210",
    val email: String = "owner@membersondesk.com",
    val address: String = "Main Street, Sector 1",
    val branch: String = "Branch 1",
    val isLoggedIn: Boolean = false,
    val currencySymbol: String = "₹",
    val reminderDaysBefore: Int = 7,
    val whatsappMessageTemplate: String = "Hi {name}, your {business_type} membership payment of {currency}{amount} is due on {due_date}. Please renew your membership to continue uninterrupted access."
)
