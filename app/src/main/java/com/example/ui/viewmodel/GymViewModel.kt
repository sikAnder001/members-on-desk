package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GymRemoteDataSource
import com.example.data.remote.dto.*
import com.example.data.repository.AttendanceResult
import com.example.data.repository.GymRepository
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val remoteDataSource = GymRemoteDataSource(application)
    private val repository = GymRepository(
        businessDao = db.businessDao(),
        planDao = db.planDao(),
        memberDao = db.memberDao(),
        subscriptionDao = db.subscriptionDao(),
        paymentDao = db.paymentDao(),
        attendanceDao = db.attendanceDao(),
        notificationDao = db.notificationDao(),
        db = db,
        remoteDataSource = remoteDataSource
    )

    val business: StateFlow<Business> = repository.businessFlow
        .map { it ?: Business() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Business())

    val plans: StateFlow<List<SubscriptionPlan>> = repository.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlans: StateFlow<List<SubscriptionPlan>> = repository.activePlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<Member>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    fun clearError() { errorMessage.value = null }

    val dashboardData = MutableStateFlow<DashboardResponse?>(null)
    private val remoteFilteredMembers = MutableStateFlow<List<Member>?>(null)

    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow<MemberStatus?>(null)

    val filteredMembers: StateFlow<List<Member>> = combine(
        allMembers,
        searchQuery,
        selectedStatusFilter,
        remoteFilteredMembers
    ) { members, query, statusFilter, remoteList ->
        if (remoteList != null) {
            remoteList
        } else {
            members.filter { member ->
                val matchesQuery = query.isBlank() ||
                    member.name.contains(query, ignoreCase = true) ||
                    member.phone.contains(query, ignoreCase = true) ||
                    member.memberCode.contains(query, ignoreCase = true)

                val matchesStatus = statusFilter == null || member.status == statusFilter
                matchesQuery && matchesStatus
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMembersCount: StateFlow<Int> = combine(repository.totalMembersCount, dashboardData) { local, remote ->
        remote?.stats?.resolvedTotalMembers() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeMembersCount: StateFlow<Int> = combine(repository.activeMembersCount, dashboardData) { local, remote ->
        remote?.stats?.resolvedActiveMembers() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiringMembersCount: StateFlow<Int> = combine(repository.expiringMembersCount, dashboardData) { local, remote ->
        remote?.stats?.resolvedExpiringMembers() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredMembersCount: StateFlow<Int> = combine(repository.expiredMembersCount, dashboardData) { local, remote ->
        remote?.stats?.resolvedExpiredMembers() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingPaymentMembersCount: StateFlow<Int> = combine(repository.pendingPaymentMembersCount, dashboardData) { local, remote ->
        remote?.stats?.resolvedPendingPaymentsCount() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayAttendanceCount: StateFlow<Int> = combine(repository.todayAttendanceCount, dashboardData) { local, remote ->
        remote?.stats?.resolvedTodayAttendance() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyCollection: StateFlow<Double> = combine(repository.monthlyCollection, dashboardData) { local, remote ->
        remote?.stats?.resolvedRevenue() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRevenue: StateFlow<Double> = combine(repository.totalRevenue, dashboardData) { local, remote ->
        remote?.stats?.resolvedRevenue() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingPaymentsTotal: StateFlow<Double> = combine(repository.pendingPaymentsTotal, dashboardData) { local, remote ->
        remote?.stats?.resolvedPendingPaymentsTotal() ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayAttendance: StateFlow<List<Attendance>> = repository.getAttendanceForDate(DateUtils.getTodayDbDate())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val scanResult = MutableStateFlow<AttendanceResult?>(null)
    val selectedMemberId = MutableStateFlow<Long?>(null)

    val selectedMemberFlow: StateFlow<Member?> = selectedMemberId.flatMapLatest { id ->
        if (id != null) repository.getMemberByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedMemberSubscriptions: StateFlow<List<Subscription>> = selectedMemberId.flatMapLatest { id ->
        if (id != null) repository.getSubscriptionsForMember(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMemberPayments: StateFlow<List<Payment>> = selectedMemberId.flatMapLatest { id ->
        if (id != null) repository.getPaymentsForMember(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMemberAttendance: StateFlow<List<Attendance>> = selectedMemberId.flatMapLatest { id ->
        if (id != null) repository.getAttendanceForMember(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedReceiptPayment = MutableStateFlow<Payment?>(null)

    init {
        viewModelScope.launch {
            try {
                val firstMember = repository.allMembers.first().firstOrNull()
                val currentBiz = repository.businessFlow.first()
                if (repository.totalMembersCount.first() < 9 || currentBiz?.name != "FitPlus Gym" || firstMember?.memberCode?.startsWith("FP-") != true) {
                    repository.reseedSampleData()
                }
            } catch (e: Exception) {
                // Ignore seed error
            }
            checkAndNotifyExpiringMembers()
            refreshDashboard()
            refreshMembers()
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = repository.fetchRemoteDashboard()
                if (response != null) {
                    dashboardData.value = response
                    // Sync plans and members from backend
                    repository.syncFromLiveBackend()
                } else if (com.example.data.remote.ApiClient.lastApiError.value != null) {
                    errorMessage.value = com.example.data.remote.ApiClient.lastApiError.value
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to load dashboard data"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refreshMembers() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val q = searchQuery.value.trim()
                val statusParam = when (selectedStatusFilter.value) {
                    MemberStatus.ACTIVE -> "active"
                    MemberStatus.EXPIRING_SOON -> "expiring_soon"
                    MemberStatus.PAYMENT_PENDING -> "payment_pending"
                    MemberStatus.EXPIRED -> "expired"
                    MemberStatus.INACTIVE -> "inactive"
                    null -> null
                }
                val result = repository.fetchRemoteMembers(
                    query = q.ifBlank { null },
                    status = statusParam
                )
                if (result != null) {
                    remoteFilteredMembers.value = result
                } else if (com.example.data.remote.ApiClient.lastApiError.value != null) {
                    errorMessage.value = com.example.data.remote.ApiClient.lastApiError.value
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to fetch members"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refreshPlans() {
        viewModelScope.launch {
            try {
                repository.syncFromLiveBackend()
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }

    fun refreshPayments() {
        viewModelScope.launch {
            try {
                repository.fetchRemotePayments()
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }

    fun refreshMemberDetail(memberId: Long) {
        selectedMemberId.value = memberId
        viewModelScope.launch {
            try {
                repository.fetchRemoteMember(memberId)
                repository.fetchRemoteSubscriptionsForMember(memberId)
                repository.fetchRemotePaymentsForMember(memberId)
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val authMe = repository.getAuthMe()
                if (authMe != null) {
                    val currentBiz = business.value
                    val liveBiz = authMe.business
                    val liveUser = authMe.user
                    val updatedBiz = currentBiz.copy(
                        name = liveBiz?.name ?: currentBiz.name,
                        phone = liveBiz?.phone ?: liveUser?.phone ?: currentBiz.phone,
                        ownerName = liveUser?.name ?: currentBiz.ownerName,
                        email = liveBiz?.email ?: liveUser?.email ?: currentBiz.email,
                        address = liveBiz?.address ?: currentBiz.address
                    )
                    repository.updateBusiness(updatedBiz)
                }
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }

    fun updateOwnerProfile(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        businessName: String? = null,
        address: String? = null,
        upiId: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = UpdateProfileRequest(
                    name = name?.trim()?.ifBlank { null },
                    email = email?.trim()?.ifBlank { null },
                    phone = phone?.trim()?.ifBlank { null },
                    business_name = businessName?.trim()?.ifBlank { null },
                    address = address?.trim()?.ifBlank { null },
                    upi_id = upiId?.trim()?.ifBlank { null }
                )
                val response = repository.updateRemoteProfile(request)
                if (response != null && response.success != false) {
                    onComplete(true, null)
                } else {
                    val err = com.example.data.remote.ApiClient.lastApiError.value ?: response?.message ?: "Failed to update profile"
                    onComplete(false, err)
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to update profile")
            }
        }
    }

    fun checkAndNotifyExpiringMembers() {
        viewModelScope.launch {
            val expiring = allMembers.value.filter { it.status == MemberStatus.EXPIRING_SOON }
            if (expiring.isNotEmpty()) {
                NotificationHelper.showNotification(
                    getApplication(),
                    1001,
                    "Membership Expiry Alert",
                    "${expiring.size} member(s) have memberships expiring soon. Tap to view and send WhatsApp reminders."
                )
            }
        }
    }

    fun selectMember(memberId: Long?) {
        selectedMemberId.value = memberId
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
        refreshMembers()
    }

    fun setStatusFilter(status: MemberStatus?) {
        selectedStatusFilter.value = status
        refreshMembers()
    }

    fun markAttendance(memberCodeOrId: String) {
        viewModelScope.launch {
            val result = repository.markAttendanceByCode(memberCodeOrId)
            scanResult.value = result
            refreshDashboard()
        }
    }

    fun clearScanResult() {
        scanResult.value = null
    }

    fun addMember(
        name: String,
        phone: String,
        email: String,
        address: String,
        plan: SubscriptionPlan,
        paymentMode: PaymentMode,
        isPaid: Boolean,
        notes: String = "",
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val memberId = repository.addMemberWithSubscription(
                name = name,
                phone = phone,
                email = email,
                address = address,
                plan = plan,
                paymentMode = paymentMode,
                isPaid = isPaid,
                notes = notes
            )
            refreshMembers()
            refreshDashboard()
            onSuccess(memberId)
        }
    }

    fun updateMember(member: Member, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateMember(member)
            refreshMembers()
            refreshDashboard()
            onSuccess()
        }
    }

    fun deleteMember(member: Member, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMember(member)
            refreshMembers()
            refreshDashboard()
            onSuccess()
        }
    }

    fun renewSubscription(
        memberId: Long,
        plan: SubscriptionPlan,
        paymentMode: PaymentMode,
        discount: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.renewSubscription(
                memberId = memberId,
                plan = plan,
                paymentMode = paymentMode,
                discount = discount,
                notes = notes
            )
            if (success) {
                refreshMemberDetail(memberId)
                refreshMembers()
                refreshDashboard()
                onSuccess()
            }
        }
    }

    fun recordManualPayment(
        memberId: Long,
        amount: Double,
        paymentMode: PaymentMode,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordManualPayment(
                memberId = memberId,
                amount = amount,
                paymentMode = paymentMode,
                notes = notes
            )
            refreshPayments()
            refreshMemberDetail(memberId)
            refreshDashboard()
            onSuccess()
        }
    }

    fun savePlan(plan: SubscriptionPlan, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (plan.id == 0L) {
                repository.insertPlan(plan)
            } else {
                repository.updatePlan(plan)
            }
            refreshPlans()
            onSuccess()
        }
    }

    fun deletePlan(plan: SubscriptionPlan, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlan(plan)
            refreshPlans()
            onSuccess()
        }
    }

    fun updateBusiness(business: Business, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateBusiness(business)
            onSuccess()
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun sendWhatsAppExpiryReminder(member: Member, daysLeft: Int = 3, amount: Double = 499.0) {
        val biz = business.value
        val message = WhatsAppHelper.generateExpiryReminderMessage(
            memberName = member.name,
            businessType = biz.type.name,
            amount = amount,
            currencySymbol = biz.currencySymbol,
            dueDateFormatted = DateUtils.formatDisplayDate(System.currentTimeMillis() + (daysLeft * 24L * 60 * 60 * 1000)),
            daysLeft = daysLeft
        )
        WhatsAppHelper.openWhatsApp(getApplication(), member.phone, message)
    }

    fun sendWhatsAppReceipt(payment: Payment, validTillDate: String) {
        val biz = business.value
        val message = WhatsAppHelper.generatePaymentReceiptMessage(
            memberName = payment.memberName,
            businessName = biz.name,
            amount = payment.amount,
            currencySymbol = biz.currencySymbol,
            invoiceNumber = payment.invoiceNumber,
            validTill = validTillDate
        )
        val member = allMembers.value.find { it.id == payment.memberId }
        val phone = member?.phone ?: ""
        WhatsAppHelper.openWhatsApp(getApplication(), phone, message)
    }

    private val _selectedAuthUserType = MutableStateFlow(BusinessType.GYM)
    val selectedAuthUserType: StateFlow<BusinessType> = _selectedAuthUserType.asStateFlow()

    fun setSelectedAuthUserType(type: BusinessType) {
        _selectedAuthUserType.value = type
    }

    fun login(
        email: String,
        password: String,
        type: BusinessType,
        onComplete: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = repository.loginWithApi(email.trim(), password, type.toString().lowercase())) {
                is LoginApiResult.InvalidCredentials -> {
                    // Stop! Wrong credentials from server - do NOT let user in!
                    onComplete(false, result.message)
                }
                is LoginApiResult.Success -> {
                    val authMe = repository.getAuthMe()
                    val currentBiz = business.value
                    val liveBiz = authMe?.business
                    val liveUser = authMe?.user ?: result.response.user

                    val updatedBiz = currentBiz.copy(
                        name = liveBiz?.name ?: currentBiz.name,
                        email = email.trim(),
                        phone = liveUser?.phone ?: currentBiz.phone,
                        ownerName = liveUser?.name ?: currentBiz.ownerName,
                        type = type,
                        isLoggedIn = true
                    )
                    repository.updateBusiness(updatedBiz)
                    _selectedAuthUserType.value = type
                    onComplete(true, null)
                }
                is LoginApiResult.NetworkUnavailable -> {
                    // Do NOT allow login if token was not received from API!
                    val detail = if (result.message.isNotBlank()) ": ${result.message}" else ""
                    onComplete(false, "Connection error$detail. Please check your internet connection or server status.")
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logoutWithApi()
            val currentBiz = business.value
            val updated = currentBiz.copy(
                isLoggedIn = false
            )
            repository.updateBusiness(updated)
            onComplete()
        }
    }

    suspend fun fetchRemoteMembers(): List<Member>? = repository.fetchRemoteMembers()
    suspend fun fetchRemoteMembersDto(): List<MemberDto>? = remoteDataSource.getMembers()
    suspend fun fetchRemotePlans(): List<PlanDto>? = repository.fetchRemotePlans()
    suspend fun fetchRemoteSubscriptions(): List<SubscriptionDto>? = repository.fetchRemoteSubscriptions()
    suspend fun fetchRemotePayments(): List<PaymentDto>? = repository.fetchRemotePayments()
    suspend fun fetchRemotePaymentRequests(): List<PaymentRequestDto>? = repository.fetchRemotePaymentRequests()
    suspend fun createPaymentRequest(memberId: Long, amount: Double, method: String, subscriptionId: Long): PaymentRequestDto? =
        repository.createRemotePaymentRequest(memberId, amount, method, subscriptionId)
    suspend fun sendRazorpayWebhook(signature: String, jsonPayload: String): Boolean =
        repository.postRazorpayWebhook(signature, jsonPayload)

    /**
     * Attempts to fetch the exported members CSV/Excel file from the backend API.
     * If the API endpoint succeeds, streams and saves the file directly to the device.
     * If the API fails or is unavailable, generates a CSV from local members data and saves it.
     */
    fun exportMembers(
        onResult: (success: Boolean, message: String, uri: android.net.Uri?, fileName: String) -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())

            // 1. Try remote API export call first
            val remoteBody = try {
                remoteDataSource.exportMembers()
            } catch (e: Exception) {
                null
            }

            if (remoteBody != null) {
                val contentType = remoteBody.contentType()?.toString() ?: "text/csv"
                val extension = if (contentType.contains("excel") || contentType.contains("spreadsheet")) "xlsx" else "csv"
                val fileName = "members_export_$timestamp.$extension"

                val savedUri = com.example.util.FileExportHelper.saveApiFileToDevice(
                    context = context,
                    inputStream = remoteBody.byteStream(),
                    suggestedFileName = fileName,
                    mimeType = contentType
                )

                if (savedUri != null) {
                    onResult(true, "File exported from server and saved: $fileName", savedUri, fileName)
                    return@launch
                }
            }

            // 2. Fallback to local members CSV generation
            val currentMemberList = allMembers.value
            val csvContent = com.example.util.FileExportHelper.generateLocalMembersCsv(currentMemberList)
            val fileName = "members_export_$timestamp.csv"

            val localSavedUri = com.example.util.FileExportHelper.saveApiFileToDevice(
                context = context,
                inputStream = java.io.ByteArrayInputStream(csvContent.toByteArray(Charsets.UTF_8)),
                suggestedFileName = fileName,
                mimeType = "text/csv"
            )

            if (localSavedUri != null) {
                onResult(true, "Members exported (${currentMemberList.size} records) and saved: $fileName", localSavedUri, fileName)
            } else {
                onResult(false, "Failed to save exported file to device storage.", null, fileName)
            }
        }
    }

    fun reseedSampleData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.reseedSampleData()
            onSuccess()
        }
    }
}
