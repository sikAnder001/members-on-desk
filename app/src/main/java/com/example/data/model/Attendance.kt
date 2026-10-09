package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttendanceType {
    CHECK_IN,
    CHECK_OUT
}

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val memberCode: String,
    val memberName: String,
    val dateString: String,
    val timeString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: AttendanceType = AttendanceType.CHECK_IN,
    val notes: String = ""
)
