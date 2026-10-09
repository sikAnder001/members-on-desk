package com.example.data.repository

import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.GymRemoteDataSource
import com.example.data.remote.dto.*
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed class AttendanceResult {
    data class Success(val member: Member, val attendance: Attendance) : AttendanceResult()
    data class AlreadyMarked(val member: Member, val attendance: Attendance) : AttendanceResult()
    data class MemberNotFound(val searchedCode: String) : AttendanceResult()
    data class Error(val message: String) : AttendanceResult()
}

class GymRepository(
    private val businessDao: BusinessDao,
    private val planDao: PlanDao,
    private val memberDao: MemberDao,
    private val subscriptionDao: SubscriptionDao,
    private val paymentDao: PaymentDao,
    private val attendanceDao: AttendanceDao,
    private val notificationDao: NotificationDao,
    private val db: AppDatabase,
    private val remoteDataSource: GymRemoteDataSource? = null
) {
    private val TAG = "GymRepository"

    val businessFlow: Flow<Business?> = businessDao.getBusinessFlow()
    suspend fun getBusiness(): Business? = businessDao.getBusiness()
    suspend fun updateBusiness(business: Business) = businessDao.insertOrUpdate(business)

    suspend fun loginWithApi(email: String, password: String,type: String ,deviceName: String = "android"): LoginApiResult {
        return try {
            val result = remoteDataSource?.login(email, password,type, deviceName)
                ?: LoginApiResult.NetworkUnavailable("No remote data source available")
            if (result is LoginApiResult.Success) {
                // Sync plans and members from live API
                syncFromLiveBackend()
            }
            result
        } catch (e: Exception) {
            Log.d(TAG, "API login error: ${e.message}")
            LoginApiResult.NetworkUnavailable(e.message ?: "Network error")
        }
    }

    suspend fun syncFromLiveBackend() {
        try {
            // Sync plans
            val remotePlans = remoteDataSource?.getPlans()
            if (!remotePlans.isNullOrEmpty()) {
                val entityPlans = remotePlans.map { p ->
                    SubscriptionPlan(
                        id = p.id ?: 0L,
                        name = p.name ?: "Standard Plan",
                        durationDays = p.duration_days ?: 30,
                        price = p.price ?: 1500.0,
                        description = p.description ?: "",
                        businessType = BusinessType.GYM,
                        isActive = p.is_active ?: true
                    )
                }
                planDao.insertAllPlans(entityPlans)
            }

            // Sync members
            val remoteMembers = remoteDataSource?.getMembers()
            if (!remoteMembers.isNullOrEmpty()) {
                val entityMembers = remoteMembers.mapIndexed { idx, m ->
                    val serverStatus = m.status?.lowercase()?.trim() ?: "active"
                    val mappedStatus = when (serverStatus) {
                        "active" -> MemberStatus.ACTIVE
                        "expiring_soon", "expiring" -> MemberStatus.EXPIRING_SOON
                        "payment_pending", "pending" -> MemberStatus.PAYMENT_PENDING
                        "expired" -> MemberStatus.EXPIRED
                        "inactive" -> MemberStatus.INACTIVE
                        else -> MemberStatus.ACTIVE
                    }
                    val label = m.status_label ?: m.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Active"
                    Member(
                        id = m.id ?: (idx + 1).toLong(),
                        memberCode = m.member_code ?: m.code ?: "FP-${1001 + idx}",
                        name = m.name ?: "Member",
                        phone = m.phone ?: "",
                        email = m.email ?: "",
                        address = m.address ?: "",
                        status = mappedStatus,
                        statusLabel = label,
                        qrToken = m.qr_token ?: ""
                    )
                }
                memberDao.insertAllMembers(entityMembers)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sync from live backend exception: ${e.message}")
        }
    }

    suspend fun logoutWithApi(): Boolean {
        return try {
            remoteDataSource?.logout() ?: true
        } catch (e: Exception) {
            Log.d(TAG, "API logout error: ${e.message}")
            true
        }
    }

    suspend fun getCurrentUser(): UserDto? {
        return try {
            remoteDataSource?.getCurrentUser()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAuthMe(): AuthMeResponse? {
        return try {
            remoteDataSource?.getAuthMe()
        } catch (e: Exception) {
            null
        }
    }

    fun hasAuthToken(): Boolean = remoteDataSource?.hasAuthToken() ?: false
    fun getStoredToken(): String? = remoteDataSource?.getStoredToken()

    val activePlans: Flow<List<SubscriptionPlan>> = planDao.getAllActivePlans()
    val allPlans: Flow<List<SubscriptionPlan>> = planDao.getAllPlans()
    suspend fun getPlanById(id: Long) = planDao.getPlanById(id)

    suspend fun fetchRemotePlans(): List<PlanDto>? = remoteDataSource?.getPlans()
    suspend fun fetchRemotePlan(id: Long): PlanDto? = remoteDataSource?.getPlanById(id)

    suspend fun insertPlan(plan: SubscriptionPlan): Long {
        try {
            remoteDataSource?.createPlan(
                CreatePlanRequest(
                    name = plan.name,
                    duration_days = plan.durationDays,
                    price = plan.price,
                    description = plan.description.ifBlank { null },
                    is_active = plan.isActive
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createPlan failed: ${e.message}")
        }
        return planDao.insertPlan(plan)
    }

    suspend fun updatePlan(plan: SubscriptionPlan) = planDao.updatePlan(plan)
    suspend fun deletePlan(plan: SubscriptionPlan) {
        try {
            remoteDataSource?.deletePlan(plan.id)
        } catch (e: Exception) {
            Log.d(TAG, "Remote deletePlan failed: ${e.message}")
        }
        planDao.deletePlan(plan)
    }

    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    fun getMembersByStatus(status: MemberStatus): Flow<List<Member>> = memberDao.getMembersByStatus(status)
    fun getMemberByIdFlow(id: Long): Flow<Member?> = memberDao.getMemberByIdFlow(id)
    suspend fun getMemberById(id: Long): Member? = memberDao.getMemberById(id)
    suspend fun getMemberByCode(code: String): Member? = memberDao.getMemberByCode(code.trim())
    fun searchMembers(query: String): Flow<List<Member>> = memberDao.searchMembers(query)

    val totalMembersCount: Flow<Int> = memberDao.getTotalMembersCount()
    val activeMembersCount: Flow<Int> = memberDao.getActiveMembersCount()
    val expiringMembersCount: Flow<Int> = memberDao.getExpiringMembersCount()
    val expiredMembersCount: Flow<Int> = memberDao.getExpiredMembersCount()
    val pendingPaymentMembersCount: Flow<Int> = memberDao.getPendingPaymentMembersCount()

    suspend fun fetchRemoteMembers(
        query: String? = null,
        status: String? = null,
        page: Int? = null
    ): List<Member>? {
        val remoteList = remoteDataSource?.getMembers(query, status, page) ?: return null
        val mappedMembers = remoteList.mapIndexed { idx, m ->
            val serverStatus = m.status?.lowercase()?.trim() ?: "active"
            val mappedStatus = when (serverStatus) {
                "active" -> MemberStatus.ACTIVE
                "expiring_soon", "expiring" -> MemberStatus.EXPIRING_SOON
                "payment_pending", "pending" -> MemberStatus.PAYMENT_PENDING
                "expired" -> MemberStatus.EXPIRED
                "inactive" -> MemberStatus.INACTIVE
                else -> MemberStatus.ACTIVE
            }
            val label = m.status_label ?: m.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Active"
            Member(
                id = m.id ?: (idx + 1).toLong(),
                memberCode = m.member_code ?: m.code ?: "FP-${1001 + idx}",
                name = m.name ?: "Member",
                phone = m.phone ?: "",
                email = m.email ?: "",
                address = m.address ?: "",
                status = mappedStatus,
                statusLabel = label,
                planName = m.plan_name ?: "",
                planId = m.plan_id,
                qrToken = m.qr_token ?: ""
            )
        }
        if (query.isNullOrBlank() && status.isNullOrBlank()) {
            memberDao.insertAllMembers(mappedMembers)
        }
        return mappedMembers
    }

    suspend fun fetchRemoteDashboard(): DashboardResponse? =
        remoteDataSource?.getDashboard()

    suspend fun fetchRemoteMember(id: Long): MemberDto? = remoteDataSource?.getMemberById(id)

    suspend fun addMemberWithSubscription(
        name: String,
        phone: String,
        email: String,
        address: String,
        plan: SubscriptionPlan,
        paymentMode: PaymentMode,
        isPaid: Boolean,
        notes: String = ""
    ): Long {
        val count = memberDao.getAllMembers().first().size
        val nextCode = "FP-${1001 + count}"
        val currentTime = System.currentTimeMillis()
        val durationMillis = plan.durationDays.toLong() * 24 * 60 * 60 * 1000

        val initialStatus = if (isPaid) MemberStatus.ACTIVE else MemberStatus.PAYMENT_PENDING

        try {
            remoteDataSource?.createMember(
                CreateMemberRequest(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim().ifBlank { null },
                    address = address.trim().ifBlank { null },
                    status = if (isPaid) "active" else "inactive",
                    joined_at = DateUtils.getTodayDbDate(),
                    notes = notes.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createMember failed: ${e.message}")
        }

        val member = Member(
            memberCode = nextCode,
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            registrationDate = currentTime,
            status = initialStatus,
            planId = plan.id,
            planName = plan.name,
            avatarColorIndex = (count % 6),
            notes = notes
        )
        val memberId = memberDao.insertMember(member)

        try {
            remoteDataSource?.createSubscription(
                CreateSubscriptionRequest(
                    member_id = memberId,
                    membership_plan_id = plan.id,
                    starts_at = DateUtils.getTodayDbDate(),
                    notes = "Initial membership",
                    collect_cash = (paymentMode == PaymentMode.CASH)
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createSubscription failed: ${e.message}")
        }

        val subscription = Subscription(
            memberId = memberId,
            planId = plan.id,
            planName = plan.name,
            price = plan.price,
            discount = 0.0,
            finalAmount = plan.price,
            startDate = currentTime,
            endDate = currentTime + durationMillis,
            status = if (isPaid) SubscriptionStatus.ACTIVE else SubscriptionStatus.PENDING_PAYMENT,
            notes = "Joined on ${DateUtils.formatDisplayDate(currentTime)}"
        )
        val subscriptionId = subscriptionDao.insertSubscription(subscription)

        val invoiceNumber = "INV-${System.currentTimeMillis() % 100000}"

        try {
            val remoteMethod = when (paymentMode) {
                PaymentMode.CASH -> "cash"
                PaymentMode.UPI -> "upi"
                PaymentMode.CARD -> "gateway"
                PaymentMode.BANK_TRANSFER -> "online"
            }
            remoteDataSource?.createPayment(
                CreatePaymentRequest(
                    member_id = memberId,
                    amount = plan.price,
                    method = remoteMethod,
                    subscription_id = subscriptionId,
                    reference = invoiceNumber,
                    notes = if (isPaid) "Initial registration fee" else "Payment pending"
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createPayment failed: ${e.message}")
        }

        val payment = Payment(
            memberId = memberId,
            memberName = name,
            subscriptionId = subscriptionId,
            amount = plan.price,
            paymentDate = currentTime,
            paymentMode = paymentMode,
            invoiceNumber = invoiceNumber,
            status = if (isPaid) PaymentStatus.PAID else PaymentStatus.PENDING,
            notes = if (isPaid) "Initial registration fee" else "Payment pending"
        )
        paymentDao.insertPayment(payment)

        return memberId
    }

    suspend fun updateMember(member: Member) {
        try {
            remoteDataSource?.updateMember(
                id = member.id,
                request = UpdateMemberRequest(
                    name = member.name,
                    phone = member.phone,
                    email = member.email.ifBlank { null },
                    address = member.address.ifBlank { null },
                    status = when (member.status) {
                        MemberStatus.ACTIVE -> "active"
                        MemberStatus.EXPIRING_SOON -> "expiring_soon"
                        MemberStatus.PAYMENT_PENDING -> "payment_pending"
                        MemberStatus.EXPIRED -> "expired"
                        MemberStatus.INACTIVE -> "inactive"
                    },
                    notes = member.notes.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote updateMember failed: ${e.message}")
        }
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(member: Member) {
        try {
            remoteDataSource?.deleteMember(member.id)
        } catch (e: Exception) {
            Log.d(TAG, "Remote deleteMember failed: ${e.message}")
        }
        memberDao.deleteMember(member)
    }

    suspend fun updateRemoteProfile(request: UpdateProfileRequest): ProfileResponse? {
        val response = remoteDataSource?.updateProfile(request) ?: remoteDataSource?.patchProfile(request)
        if (response != null && response.success != false) {
            val liveBiz = response.data?.business
            val liveUser = response.data?.user
            val currentBiz = getBusiness() ?: Business()
            val updated = currentBiz.copy(
                name = liveBiz?.name ?: request.business_name ?: currentBiz.name,
                ownerName = liveUser?.name ?: request.name ?: currentBiz.ownerName,
                email = liveBiz?.email ?: liveUser?.email ?: request.email ?: currentBiz.email,
                phone = liveBiz?.phone ?: liveUser?.phone ?: request.phone ?: currentBiz.phone,
                address = liveBiz?.address ?: request.address ?: currentBiz.address
            )
            updateBusiness(updated)
        }
        return response
    }

    suspend fun fetchRemoteSubscriptionsForMember(memberId: Long): List<Subscription>? {
        val dtos = remoteDataSource?.getSubscriptions(memberId = memberId) ?: return null
        val mapped = dtos.map { dto ->
            val amount = dto.amount ?: 1500.0
            val subStatus = when (dto.status?.lowercase()) {
                "active" -> SubscriptionStatus.ACTIVE
                "expired" -> SubscriptionStatus.EXPIRED
                "pending", "payment_pending" -> SubscriptionStatus.PENDING_PAYMENT
                else -> SubscriptionStatus.ACTIVE
            }
            Subscription(
                id = dto.id ?: 0L,
                memberId = dto.member_id ?: memberId,
                planId = dto.plan_id ?: dto.membership_plan_id ?: 0L,
                planName = dto.plan_name ?: "Membership Plan",
                price = amount,
                discount = 0.0,
                finalAmount = amount,
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                status = subStatus,
                notes = dto.notes ?: ""
            )
        }
        if (mapped.isNotEmpty()) {
            mapped.forEach { subscriptionDao.insertSubscription(it) }
        }
        return mapped
    }

    suspend fun fetchRemotePaymentsForMember(memberId: Long): List<Payment>? {
        val dtos = remoteDataSource?.getPayments(memberId = memberId) ?: return null
        val mapped = dtos.map { dto ->
            val mode = when (dto.method?.lowercase() ?: dto.payment_mode?.lowercase()) {
                "upi" -> PaymentMode.UPI
                "cash" -> PaymentMode.CASH
                "card", "gateway" -> PaymentMode.CARD
                else -> PaymentMode.BANK_TRANSFER
            }
            val status = if (dto.status?.equals("pending", ignoreCase = true) == true) PaymentStatus.PENDING else PaymentStatus.PAID
            Payment(
                id = dto.id ?: 0L,
                memberId = dto.member_id ?: memberId,
                memberName = dto.member_name ?: "Member",
                subscriptionId = dto.subscription_id,
                amount = dto.amount ?: 0.0,
                paymentDate = System.currentTimeMillis(),
                paymentMode = mode,
                invoiceNumber = dto.invoice_number ?: dto.reference ?: "INV-${dto.id ?: 0}",
                status = status,
                notes = dto.notes ?: ""
            )
        }
        if (mapped.isNotEmpty()) {
            mapped.forEach { paymentDao.insertPayment(it) }
        }
        return mapped
    }

    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    fun getSubscriptionsForMember(memberId: Long): Flow<List<Subscription>> =
        subscriptionDao.getSubscriptionsForMember(memberId)

    suspend fun getLatestSubscriptionForMember(memberId: Long): Subscription? =
        subscriptionDao.getLatestSubscriptionForMember(memberId)

    suspend fun fetchRemoteSubscriptions(page: Int? = null): List<SubscriptionDto>? =
        remoteDataSource?.getSubscriptions(memberId = null, page = page)

    suspend fun fetchRemoteSubscription(id: Long): SubscriptionDto? =
        remoteDataSource?.getSubscriptionById(id)

    suspend fun renewSubscription(
        memberId: Long,
        plan: SubscriptionPlan,
        paymentMode: PaymentMode,
        discount: Double = 0.0,
        notes: String = ""
    ): Boolean {
        val member = memberDao.getMemberById(memberId) ?: return false
        val currentSub = subscriptionDao.getLatestSubscriptionForMember(memberId)
        val currentTime = System.currentTimeMillis()
        val durationMillis = plan.durationDays.toLong() * 24 * 60 * 60 * 1000

        val startDate = if (currentSub != null && currentSub.endDate > currentTime) {
            currentSub.endDate
        } else {
            currentTime
        }
        val endDate = startDate + durationMillis
        val finalAmount = (plan.price - discount).coerceAtLeast(0.0)

        try {
            remoteDataSource?.createSubscription(
                CreateSubscriptionRequest(
                    member_id = memberId,
                    membership_plan_id = plan.id,
                    starts_at = DateUtils.formatDisplayDate(startDate),
                    notes = notes.ifBlank { "Renewed: ${plan.name}" },
                    collect_cash = (paymentMode == PaymentMode.CASH)
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createSubscription for renewal failed: ${e.message}")
        }

        val newSubscription = Subscription(
            memberId = memberId,
            planId = plan.id,
            planName = plan.name,
            price = plan.price,
            discount = discount,
            finalAmount = finalAmount,
            startDate = startDate,
            endDate = endDate,
            status = SubscriptionStatus.ACTIVE,
            notes = if (notes.isNotBlank()) notes else "Renewed: ${plan.name}"
        )
        val subId = subscriptionDao.insertSubscription(newSubscription)

        val invoiceNumber = "INV-${System.currentTimeMillis() % 100000}"

        try {
            val remoteMethod = when (paymentMode) {
                PaymentMode.CASH -> "cash"
                PaymentMode.UPI -> "upi"
                PaymentMode.CARD -> "gateway"
                PaymentMode.BANK_TRANSFER -> "online"
            }
            remoteDataSource?.createPayment(
                CreatePaymentRequest(
                    member_id = memberId,
                    amount = finalAmount,
                    method = remoteMethod,
                    subscription_id = subId,
                    reference = invoiceNumber,
                    notes = "Renewal payment"
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createPayment for renewal failed: ${e.message}")
        }

        val payment = Payment(
            memberId = memberId,
            memberName = member.name,
            subscriptionId = subId,
            amount = finalAmount,
            paymentDate = currentTime,
            paymentMode = paymentMode,
            invoiceNumber = invoiceNumber,
            status = PaymentStatus.PAID,
            notes = "Renewal payment"
        )
        paymentDao.insertPayment(payment)

        val updatedMember = member.copy(
            status = MemberStatus.ACTIVE,
            planId = plan.id,
            planName = plan.name
        )
        memberDao.updateMember(updatedMember)

        return true
    }

    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()
    fun getPaymentsForMember(memberId: Long): Flow<List<Payment>> = paymentDao.getPaymentsForMember(memberId)
    val monthlyCollection: Flow<Double> = paymentDao.getMonthlyCollection(DateUtils.getStartOfMonthTimestamp())
        .map { it ?: 0.0 }
    val totalRevenue: Flow<Double> = paymentDao.getTotalRevenue().map { it ?: 0.0 }
    val pendingPaymentsTotal: Flow<Double> = paymentDao.getPendingPaymentsTotal().map { it ?: 0.0 }

    suspend fun fetchRemotePayments(page: Int? = null): List<PaymentDto>? =
        remoteDataSource?.getPayments(memberId = null, page = page)

    suspend fun recordManualPayment(
        memberId: Long,
        amount: Double,
        paymentMode: PaymentMode,
        notes: String
    ): Long {
        val member = memberDao.getMemberById(memberId) ?: return -1
        val invoiceNumber = "INV-${System.currentTimeMillis() % 100000}"

        try {
            val remoteMethod = when (paymentMode) {
                PaymentMode.CASH -> "cash"
                PaymentMode.UPI -> "upi"
                PaymentMode.CARD -> "gateway"
                PaymentMode.BANK_TRANSFER -> "online"
            }
            remoteDataSource?.createPayment(
                CreatePaymentRequest(
                    member_id = memberId,
                    amount = amount,
                    method = remoteMethod,
                    subscription_id = null,
                    reference = invoiceNumber,
                    notes = notes.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "Remote createPayment failed: ${e.message}")
        }

        val payment = Payment(
            memberId = memberId,
            memberName = member.name,
            amount = amount,
            paymentDate = System.currentTimeMillis(),
            paymentMode = paymentMode,
            invoiceNumber = invoiceNumber,
            status = PaymentStatus.PAID,
            notes = notes
        )
        val id = paymentDao.insertPayment(payment)
        if (member.status == MemberStatus.PAYMENT_PENDING) {
            memberDao.updateMember(member.copy(status = MemberStatus.ACTIVE))
        }
        return id
    }

    suspend fun fetchRemotePaymentRequests(page: Int? = null): List<PaymentRequestDto>? =
        remoteDataSource?.getPaymentRequests(page)

    suspend fun createRemotePaymentRequest(
        memberId: Long,
        amount: Double,
        method: String,
        subscriptionId: Long
    ): PaymentRequestDto? =
        remoteDataSource?.createPaymentRequest(
            CreatePaymentRequestDto(
                member_id = memberId,
                amount = amount,
                method = method,
                subscription_id = subscriptionId
            )
        )

    suspend fun fetchRemotePaymentRequest(id: Long): PaymentRequestDto? =
        remoteDataSource?.getPaymentRequestById(id)

    suspend fun postRazorpayWebhook(signature: String, jsonPayload: String): Boolean =
        remoteDataSource?.sendRazorpayWebhook(signature, jsonPayload) ?: false

    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()
    fun getAttendanceForDate(dateStr: String): Flow<List<Attendance>> = attendanceDao.getAttendanceByDate(dateStr)
    fun getAttendanceForMember(memberId: Long): Flow<List<Attendance>> = attendanceDao.getAttendanceForMember(memberId)
    val todayAttendanceCount: Flow<Int> = attendanceDao.getTodayAttendanceCount(DateUtils.getTodayDbDate())

    suspend fun fetchRemoteAttendance(date: String? = null, page: Int? = null): List<AttendanceDto>? =
        remoteDataSource?.getAttendance(date, page)

    suspend fun markAttendanceByCode(codeOrId: String): AttendanceResult {
        val trimmed = codeOrId.trim()

        try {
            remoteDataSource?.markAttendance(trimmed)
        } catch (e: Exception) {
            Log.d(TAG, "Remote attendance mark failed: ${e.message}")
        }

        val member = memberDao.getMemberByCode(trimmed)
            ?: run {
                val idLong = trimmed.toLongOrNull()
                if (idLong != null) memberDao.getMemberById(idLong) else null
            }
            ?: return AttendanceResult.MemberNotFound(trimmed)

        val todayStr = DateUtils.getTodayDbDate()
        val existing = attendanceDao.getTodayAttendanceForMember(member.id, todayStr)
        if (existing != null) {
            return AttendanceResult.AlreadyMarked(member, existing)
        }

        val currentTime = System.currentTimeMillis()
        val attendance = Attendance(
            memberId = member.id,
            memberCode = member.memberCode,
            memberName = member.name,
            dateString = todayStr,
            timeString = DateUtils.getCurrentTimeString(),
            timestamp = currentTime,
            type = AttendanceType.CHECK_IN,
            notes = "QR Verified"
        )
        val attId = attendanceDao.insertAttendance(attendance)
        return AttendanceResult.Success(member, attendance.copy(id = attId))
    }

    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = notificationDao.getUnreadCount()
    suspend fun markNotificationRead(id: Long) = notificationDao.markAsRead(id)
    suspend fun markAllNotificationsRead() = notificationDao.markAllAsRead()
    suspend fun addNotification(title: String, message: String, type: NotificationType, memberId: Long? = null) {
        notificationDao.insertNotification(
            AppNotification(
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                memberId = memberId
            )
        )
    }

    suspend fun reseedSampleData() {
        db.notificationDao().deleteAll()
        db.attendanceDao().deleteAll()
        db.paymentDao().deleteAll()
        db.subscriptionDao().deleteAll()
        db.memberDao().deleteAll()
        db.planDao().deleteAll()
        InitialDataSeeder.seedDatabase(db)
    }
}
