package com.example.data.local

import androidx.room.*
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY paymentDate DESC")
    fun getPaymentsForMember(memberId: Long): Flow<List<Payment>>

    @Query("SELECT SUM(amount) FROM payments WHERE status = 'PAID' AND paymentDate >= :startTimestamp")
    fun getMonthlyCollection(startTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE status = 'PAID'")
    fun getTotalRevenue(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE status = 'PENDING'")
    fun getPendingPaymentsTotal(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
