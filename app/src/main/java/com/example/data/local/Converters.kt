package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromMemberStatus(status: MemberStatus): String = status.name

    @TypeConverter
    fun toMemberStatus(value: String): MemberStatus = try {
        MemberStatus.valueOf(value)
    } catch (e: Exception) {
        MemberStatus.ACTIVE
    }

    @TypeConverter
    fun fromSubscriptionStatus(status: SubscriptionStatus): String = status.name

    @TypeConverter
    fun toSubscriptionStatus(value: String): SubscriptionStatus = try {
        SubscriptionStatus.valueOf(value)
    } catch (e: Exception) {
        SubscriptionStatus.ACTIVE
    }

    @TypeConverter
    fun fromPaymentMode(mode: PaymentMode): String = mode.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = try {
        PaymentMode.valueOf(value)
    } catch (e: Exception) {
        PaymentMode.UPI
    }

    @TypeConverter
    fun fromPaymentStatus(status: PaymentStatus): String = status.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = try {
        PaymentStatus.valueOf(value)
    } catch (e: Exception) {
        PaymentStatus.PAID
    }

    @TypeConverter
    fun fromAttendanceType(type: AttendanceType): String = type.name

    @TypeConverter
    fun toAttendanceType(value: String): AttendanceType = try {
        AttendanceType.valueOf(value)
    } catch (e: Exception) {
        AttendanceType.CHECK_IN
    }

    @TypeConverter
    fun fromNotificationType(type: NotificationType): String = type.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = try {
        NotificationType.valueOf(value)
    } catch (e: Exception) {
        NotificationType.SYSTEM
    }

    @TypeConverter
    fun fromBusinessType(type: BusinessType): String = type.name

    @TypeConverter
    fun toBusinessType(value: String): BusinessType = try {
        BusinessType.valueOf(value)
    } catch (e: Exception) {
        BusinessType.GYM
    }
}
