package com.example.data.local

import androidx.room.*
import com.example.data.model.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE dateString = :dateStr ORDER BY timestamp DESC")
    fun getAttendanceByDate(dateStr: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getAttendanceForMember(memberId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId AND dateString = :dateStr LIMIT 1")
    suspend fun getTodayAttendanceForMember(memberId: Long, dateStr: String): Attendance?

    @Query("SELECT COUNT(*) FROM attendance WHERE dateString = :dateStr")
    fun getTodayAttendanceCount(dateStr: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()
}
