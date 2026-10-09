package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentMode {
    CASH,
    UPI,
    CARD,
    BANK_TRANSFER
}

enum class PaymentStatus {
    PAID,
    PENDING,
    PARTIAL
}

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val memberName: String,
    val subscriptionId: Long? = null,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMode: PaymentMode = PaymentMode.UPI,
    val invoiceNumber: String,
    val status: PaymentStatus = PaymentStatus.PAID,
    val notes: String = ""
)
