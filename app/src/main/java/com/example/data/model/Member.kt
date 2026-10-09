package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemberStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    PAYMENT_PENDING,
    INACTIVE
}

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberCode: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val status: MemberStatus = MemberStatus.ACTIVE,
    val statusLabel: String = "",
    val planId: Long? = null,
    val planName: String = "",
    val avatarColorIndex: Int = 0,
    val emergencyContact: String = "",
    val qrToken: String = "",
    val notes: String = ""
)
