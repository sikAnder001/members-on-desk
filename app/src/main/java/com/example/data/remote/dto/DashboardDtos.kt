package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardStatsDto(
    @Json(name = "total_members") val total_members: Int? = null,
    @Json(name = "members") val members: Int? = null,
    @Json(name = "active_members") val active_members: Int? = null,
    @Json(name = "expiring_soon_members") val expiring_soon_members: Int? = null,
    @Json(name = "expiring_members") val expiring_members: Int? = null,
    @Json(name = "expiring") val expiring: Int? = null,
    @Json(name = "expired_members") val expired_members: Int? = null,
    @Json(name = "expired") val expired: Int? = null,
    @Json(name = "pending_payments_count") val pending_payments_count: Int? = null,
    @Json(name = "pending_payments") val pending_payments: Double? = null,
    @Json(name = "pending") val pending: Int? = null,
    @Json(name = "pending_payments_total") val pending_payments_total: Double? = null,
    @Json(name = "today_attendance_count") val today_attendance_count: Int? = null,
    @Json(name = "today_attendance") val today_attendance: Int? = null,
    @Json(name = "monthly_revenue") val monthly_revenue: Double? = null,
    @Json(name = "total_revenue") val total_revenue: Double? = null,
    @Json(name = "revenue") val revenue: Double? = null
) {
    fun resolvedTotalMembers(): Int = total_members ?: members ?: 0
    fun resolvedActiveMembers(): Int = active_members ?: 0
    fun resolvedExpiringMembers(): Int = expiring_soon_members ?: expiring_members ?: expiring ?: 0
    fun resolvedExpiredMembers(): Int = expired_members ?: expired ?: 0
    fun resolvedPendingPaymentsCount(): Int = pending_payments_count ?: pending ?: (if (pending_payments != null && pending_payments > 0) 1 else 0)
    fun resolvedTodayAttendance(): Int = today_attendance_count ?: today_attendance ?: 0
    fun resolvedRevenue(): Double = monthly_revenue ?: revenue ?: total_revenue ?: 0.0
    fun resolvedPendingPaymentsTotal(): Double = pending_payments_total ?: pending_payments ?: 0.0
}

@JsonClass(generateAdapter = true)
data class DashboardResponse(
    @Json(name = "stats") val stats: DashboardStatsDto? = null,
    @Json(name = "today_attendance") val today_attendance: List<AttendanceDto>? = null,
    @Json(name = "expiring") val expiring: List<MemberDto>? = null,
    @Json(name = "pending") val pending: List<MemberDto>? = null,
    @Json(name = "recent_payments") val recent_payments: List<PaymentDto>? = null,
    @Json(name = "recent_attendance") val recent_attendance: List<AttendanceDto>? = null
)

