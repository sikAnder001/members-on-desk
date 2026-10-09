package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Attendance
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.repository.AttendanceResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.GymViewModel
import com.example.util.DateUtils
import com.example.util.QrCodeImageAnalyzer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private val avatarColors = listOf(
    Color(0xFF1976D2), // Blue (NS)
    Color(0xFFE64A19), // Deep Orange (RG)
    Color(0xFF7B1FA2), // Purple
    Color(0xFF00796B), // Teal
    Color(0xFFD81B60), // Magenta
    Color(0xFF0288D1), // Cyan/Blue
    Color(0xFFF57C00)  // Amber
)

private fun getAvatarColor(id: Long): Color {
    val index = (id.toInt() and 0x7FFFFFFF) % avatarColors.size
    return avatarColors[index]
}

private fun getMemberInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "M"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrAttendanceScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMemberDetail: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 0 = QR Scanner, 1 = Today's Present
    var selectedTab by remember { mutableIntStateOf(0) }

    val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission needed to scan member QR codes", Toast.LENGTH_SHORT).show()
        }
    }

    // Flashlight & camera reference
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }

    // Manual input toggle and value
    var showManualInput by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }

    // Search query for members list
    var scannerSearchQuery by remember { mutableStateOf("") }
    var scannerStatusFilter by remember { mutableStateOf<MemberStatus?>(null) }

    // Search query for Today's Present list
    var presentSearchQuery by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshMembers()
                viewModel.refreshDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Set of member IDs and codes checked in today
    val checkedInMemberIds = remember(todayAttendance) {
        todayAttendance.map { it.memberId }.toSet()
    }
    val checkedInMemberCodes = remember(todayAttendance) {
        todayAttendance.map { it.memberCode }.toSet()
    }
    val attendanceTimeMap = remember(todayAttendance) {
        todayAttendance.associate { (if (it.memberId != 0L) it.memberId.toString() else it.memberCode) to it.timeString }
    }

    // Popup dialog for scan results and item highlighting
    var scanDialogResult by remember { mutableStateOf<AttendanceResult?>(null) }
    var highlightedMemberId by remember { mutableStateOf<Long?>(null) }
    var scrollToMemberId by remember { mutableStateOf<Long?>(null) }

    // Handle scan results: show in Popup dialog
    LaunchedEffect(scanResult) {
        scanResult?.let { result ->
            scanDialogResult = result
            viewModel.clearScanResult()
        }
    }

    // Blink effect timer: remove highlight after 2 seconds
    LaunchedEffect(highlightedMemberId) {
        if (highlightedMemberId != null) {
            kotlinx.coroutines.delay(2000L)
            highlightedMemberId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Attendance Scanner",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        // Torch toggle
                        IconButton(
                            onClick = {
                                val next = !isTorchEnabled
                                cameraInstance?.cameraControl?.enableTorch(next)
                                isTorchEnabled = next
                            },
                            modifier = Modifier.testTag("torch_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Filled.FlashOn else Icons.Outlined.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isTorchEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Manual Code Entry Toggle
                        IconButton(
                            onClick = { showManualInput = !showManualInput },
                            modifier = Modifier.testTag("manual_entry_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showManualInput) Icons.Filled.KeyboardHide else Icons.Filled.Keyboard,
                                contentDescription = "Manual Code Entry"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs on top: QR Scanner and Today's Present
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attendance_top_tabs"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "QR Scanner",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("qr_scanner_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Today's Present (${todayAttendance.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("todays_present_tab")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // QR Scanner tab with Scanner on top & Members list below
                    QrScannerTabContent(
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        lifecycleOwner = lifecycleOwner,
                        onCameraBound = { cam -> cameraInstance = cam },
                        onQrCodeScanned = { code -> viewModel.markAttendance(code) },
                        showManualInput = showManualInput,
                        manualCodeInput = manualCodeInput,
                        onManualCodeChange = { manualCodeInput = it },
                        onSubmitManualCode = {
                            if (manualCodeInput.isNotBlank()) {
                                viewModel.markAttendance(manualCodeInput.trim())
                                manualCodeInput = ""
                            }
                        },
                        allMembers = allMembers,
                        checkedInMemberIds = checkedInMemberIds,
                        checkedInMemberCodes = checkedInMemberCodes,
                        attendanceTimeMap = attendanceTimeMap,
                        searchQuery = scannerSearchQuery,
                        onSearchQueryChange = { scannerSearchQuery = it },
                        statusFilter = scannerStatusFilter,
                        onStatusFilterChange = { scannerStatusFilter = it },
                        highlightedMemberId = highlightedMemberId,
                        scrollToMemberId = scrollToMemberId,
                        onScrollConsumed = { scrollToMemberId = null },
                        onScanMemberClick = { member ->
                            viewModel.markAttendance(member.memberCode)
                        },
                        onMemberClick = { member ->
                            onNavigateToMemberDetail?.invoke(member.id)
                        }
                    )
                }
                1 -> {
                    // Today's Present list tab
                    TodaysPresentTabContent(
                        todayAttendance = todayAttendance,
                        searchQuery = presentSearchQuery,
                        onSearchQueryChange = { presentSearchQuery = it },
                        onMemberClick = { attendance ->
                            if (attendance.memberId != 0L) {
                                onNavigateToMemberDetail?.invoke(attendance.memberId)
                            }
                        },
                        onSwitchToScanner = { selectedTab = 0 }
                    )
                }
            }
        }

        // Scan Result Popup Dialog with Ok button
        scanDialogResult?.let { result ->
            val isSuccess = result is AttendanceResult.Success
            val isAlreadyMarked = result is AttendanceResult.AlreadyMarked

            AlertDialog(
                onDismissRequest = {
                    val targetMemberId = when (result) {
                        is AttendanceResult.Success -> result.member.id
                        is AttendanceResult.AlreadyMarked -> result.member.id
                        else -> null
                    }
                    scanDialogResult = null
                    if (targetMemberId != null) {
                        selectedTab = 0
                        scrollToMemberId = targetMemberId
                        highlightedMemberId = targetMemberId
                    }
                },
                icon = {
                    val iconVector = when (result) {
                        is AttendanceResult.Success -> Icons.Default.CheckCircle
                        is AttendanceResult.AlreadyMarked -> Icons.Default.Info
                        is AttendanceResult.MemberNotFound -> Icons.Default.Warning
                        is AttendanceResult.Error -> Icons.Default.ErrorOutline
                    }
                    val iconColor = when (result) {
                        is AttendanceResult.Success -> EmeraldGreen
                        is AttendanceResult.AlreadyMarked -> Color(0xFFF59E0B)
                        is AttendanceResult.MemberNotFound -> MaterialTheme.colorScheme.error
                        is AttendanceResult.Error -> MaterialTheme.colorScheme.error
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    val titleText = when (result) {
                        is AttendanceResult.Success -> "Attendance Marked!"
                        is AttendanceResult.AlreadyMarked -> "Already Checked In"
                        is AttendanceResult.MemberNotFound -> "Member Not Found"
                        is AttendanceResult.Error -> "Scan Error"
                    }
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    when (result) {
                        is AttendanceResult.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Attendance successfully recorded for ${result.member.name}.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldGreenContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Member: ${result.member.name}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = EmeraldGreen
                                        )
                                        Text(
                                            text = "Code: ${result.member.memberCode} • Plan: ${result.member.planName.ifBlank { "Standard" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldGreen
                                        )
                                        Text(
                                            text = "Time: ${result.attendance.timeString}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldGreen
                                        )
                                    }
                                }
                            }
                        }
                        is AttendanceResult.AlreadyMarked -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${result.member.name} has already been checked in today.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Code: ${result.member.memberCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "Checked In at: ${result.attendance.timeString}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                        is AttendanceResult.MemberNotFound -> {
                            Text(
                                text = "No active member found with code or QR token: \"${result.searchedCode}\". Please check the code and try again.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is AttendanceResult.Error -> {
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetMemberId = when (result) {
                                is AttendanceResult.Success -> result.member.id
                                is AttendanceResult.AlreadyMarked -> result.member.id
                                else -> null
                            }
                            scanDialogResult = null
                            if (targetMemberId != null) {
                                selectedTab = 0
                                scrollToMemberId = targetMemberId
                                highlightedMemberId = targetMemberId
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSuccess || isAlreadyMarked) EmeraldGreen else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("scan_result_ok_button")
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

/**
 * Tab 1: QR Scanner with Camera Viewfinder on top and Members List with status below
 */
@Composable
private fun QrScannerTabContent(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraBound: (Camera) -> Unit,
    onQrCodeScanned: (String) -> Unit,
    showManualInput: Boolean,
    manualCodeInput: String,
    onManualCodeChange: (String) -> Unit,
    onSubmitManualCode: () -> Unit,
    allMembers: List<Member>,
    checkedInMemberIds: Set<Long>,
    checkedInMemberCodes: Set<String>,
    attendanceTimeMap: Map<String, String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: MemberStatus?,
    onStatusFilterChange: (MemberStatus?) -> Unit,
    highlightedMemberId: Long? = null,
    scrollToMemberId: Long? = null,
    onScrollConsumed: () -> Unit = {},
    onScanMemberClick: (Member) -> Unit,
    onMemberClick: (Member) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Filter members based on search and status
    val filteredMembers = remember(allMembers, searchQuery, statusFilter) {
        allMembers.filter { member ->
            val matchesQuery = searchQuery.isBlank() ||
                    member.name.contains(searchQuery, ignoreCase = true) ||
                    member.memberCode.contains(searchQuery, ignoreCase = true) ||
                    member.phone.contains(searchQuery, ignoreCase = true)

            val matchesStatus = statusFilter == null || member.status == statusFilter
            matchesQuery && matchesStatus
        }
    }

    // Scroll to the item whose attendance was just marked
    LaunchedEffect(scrollToMemberId) {
        if (scrollToMemberId != null) {
            val targetIndex = filteredMembers.indexOfFirst { it.id == scrollToMemberId }
            if (targetIndex >= 0) {
                // Account for the 3 header items (Scanner card, manual input, list header)
                val scrollIndex = targetIndex + 3
                listState.animateScrollToItem(scrollIndex)
            }
            onScrollConsumed()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Scanner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val cameraExecutor = Executors.newSingleThreadExecutor()

                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also {
                                                it.setAnalyzer(cameraExecutor, QrCodeImageAnalyzer { scanned ->
                                                    onQrCodeScanned(scanned)
                                                })
                                            }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        val cam = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                        onCameraBound(cam)
                                    } catch (e: Exception) {
                                        // Camera bind error fallback
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )

                        // Animated scanning laser line
                        val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
                        val laserOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laser_animation"
                        )

                        // Viewfinder Square Overlay
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .align(Alignment.Center)
                                .border(
                                    width = 2.5.dp,
                                    color = EmeraldGreen,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            // Laser horizontal line inside viewfinder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.TopCenter)
                                    .padding(top = (175 * laserOffset).dp)
                                    .background(EmeraldGreen.copy(alpha = 0.85f))
                            )
                        }

                        // Bottom overlay badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xCC0F172A),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Align QR Code inside square to check in",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // No Camera Permission state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Camera Access Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enable camera to scan member QR cards automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Expandable Manual Code Entry
        item {
            AnimatedVisibility(visible = showManualInput) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Manual Check-In by Member Code",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = onManualCodeChange,
                                placeholder = { Text("e.g. FP-1001") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_code_input_field")
                            )
                            Button(
                                onClick = onSubmitManualCode,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(56.dp).testTag("manual_checkin_button")
                            ) {
                                Text("Check In", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Members List Header & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Members (${filteredMembers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap QR icon to mark attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search member by name, code...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_members_search")
                )

                // Quick Status Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { onStatusFilterChange(null) },
                            label = { Text("All") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == MemberStatus.ACTIVE,
                            onClick = {
                                onStatusFilterChange(if (statusFilter == MemberStatus.ACTIVE) null else MemberStatus.ACTIVE)
                            },
                            label = { Text("Active") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == MemberStatus.EXPIRING_SOON,
                            onClick = {
                                onStatusFilterChange(if (statusFilter == MemberStatus.EXPIRING_SOON) null else MemberStatus.EXPIRING_SOON)
                            },
                            label = { Text("Expiring Soon") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == MemberStatus.EXPIRED,
                            onClick = {
                                onStatusFilterChange(if (statusFilter == MemberStatus.EXPIRED) null else MemberStatus.EXPIRED)
                            },
                            label = { Text("Expired") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == MemberStatus.PAYMENT_PENDING,
                            onClick = {
                                onStatusFilterChange(if (statusFilter == MemberStatus.PAYMENT_PENDING) null else MemberStatus.PAYMENT_PENDING)
                            },
                            label = { Text("Payment Due") }
                        )
                    }
                }
            }
        }

        // 4. Members List Items
        if (filteredMembers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No members found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredMembers, key = { it.id }) { member ->
                val isCheckedInToday = member.id in checkedInMemberIds || member.memberCode in checkedInMemberCodes
                val checkInTime = attendanceTimeMap[member.id.toString()] ?: attendanceTimeMap[member.memberCode]
                val isHighlighted = member.id == highlightedMemberId

                ScannerMemberItemCard(
                    member = member,
                    isCheckedInToday = isCheckedInToday,
                    checkInTime = checkInTime,
                    isHighlighted = isHighlighted,
                    onScanClick = { onScanMemberClick(member) },
                    onCardClick = { onMemberClick(member) }
                )
            }
        }
    }
}

/**
 * Member card item inside QR Scanner screen with initials, status badges, and QR scan button
 */
@Composable
private fun ScannerMemberItemCard(
    member: Member,
    isCheckedInToday: Boolean,
    checkInTime: String?,
    isHighlighted: Boolean = false,
    onScanClick: () -> Unit,
    onCardClick: () -> Unit
) {
    // 2-second blinking animation when highlighted
    val blinkTransition = rememberInfiniteTransition(label = "member_blink")
    val blinkAlpha by if (isHighlighted) {
        blinkTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blink_alpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val cardBorder = if (isHighlighted) {
        BorderStroke(2.5.dp, EmeraldGreen.copy(alpha = blinkAlpha))
    } else {
        null
    }

    val cardContainerColor = if (isHighlighted) {
        EmeraldGreenContainer.copy(alpha = 0.35f + (0.45f * blinkAlpha))
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("member_scanner_card_${member.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Avatar + Name + Subtitle + Status Badge
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circle Avatar with initials
                val initials = getMemberInitials(member.name)
                val avatarBg = getAvatarColor(member.id)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        text = "${member.memberCode} • ${member.planName.ifBlank { "Standard Plan" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    // Status Badge (Checked In, Payment Due, Expired, Expiring Soon, Active)
                    MemberStatusBadge(
                        status = member.status,
                        statusLabel = member.statusLabel,
                        isCheckedInToday = isCheckedInToday,
                        checkInTime = checkInTime
                    )
                }
            }

            // Right Action: QR Scan Button or Checked In indicator
            Spacer(modifier = Modifier.width(8.dp))

            if (isCheckedInToday) {
                // Checked In badge & confirmation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldGreenContainer,
                    modifier = Modifier.clickable { onScanClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Checked In",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = checkInTime ?: "Present",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            } else {
                // QR Scan action button to mark attendance
                FilledTonalIconButton(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("scan_button_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan to Check In",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Status badge with matching colors for Checked In, Payment Due, Expired, Expiring Soon, Active
 */
@Composable
private fun MemberStatusBadge(
    status: MemberStatus,
    statusLabel: String = "",
    isCheckedInToday: Boolean,
    checkInTime: String?
) {
    if (isCheckedInToday) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = EmeraldGreenContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Checked In",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        when (status) {
            MemberStatus.EXPIRING_SOON -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberWarningContainer
                ) {
                    Text(
                        text = if (statusLabel.isNotBlank()) statusLabel else "Expiring Soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            MemberStatus.EXPIRED -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CrimsonDangerContainer
                ) {
                    Text(
                        text = if (statusLabel.isNotBlank()) statusLabel else "Expired",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonDanger,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            MemberStatus.PAYMENT_PENDING -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEDE9FE) // Soft Violet container
                ) {
                    Text(
                        text = if (statusLabel.isNotBlank()) statusLabel else "Payment Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7C3AED), // Violet
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            MemberStatus.ACTIVE -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldGreenContainer
                ) {
                    Text(
                        text = if (statusLabel.isNotBlank()) statusLabel else "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            MemberStatus.INACTIVE -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (statusLabel.isNotBlank()) statusLabel else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 2: Today's Present List screen with clean attendee cards, timestamps and badges
 */
@Composable
private fun TodaysPresentTabContent(
    todayAttendance: List<Attendance>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMemberClick: (Attendance) -> Unit,
    onSwitchToScanner: () -> Unit
) {
    val filteredAttendance = remember(todayAttendance, searchQuery) {
        if (searchQuery.isBlank()) {
            todayAttendance
        } else {
            todayAttendance.filter {
                it.memberName.contains(searchQuery, ignoreCase = true) ||
                        it.memberCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val todayFormattedDate = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Today's Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = todayFormattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldGreenContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${todayAttendance.size} Present",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search today's present members...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("todays_present_search")
        )

        // List of Today's Attendees
        if (filteredAttendance.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "No check-ins recorded yet today" else "No matching check-ins found",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isBlank()) {
                        Button(
                            onClick = onSwitchToScanner,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch to QR Scanner")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAttendance, key = { it.id }) { att ->
                    TodayAttendeeCard(
                        attendance = att,
                        onClick = { onMemberClick(att) }
                    )
                }
            }
        }
    }
}

/**
 * Individual attendee card for Today's Present list
 */
@Composable
private fun TodayAttendeeCard(
    attendance: Attendance,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("present_card_${attendance.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circle initials avatar
                val initials = getMemberInitials(attendance.memberName)
                val avatarBg = getAvatarColor(attendance.memberId)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = attendance.memberName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = attendance.memberCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldGreenContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Checked In",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Check-in Time Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = attendance.timeString.ifBlank { "Today" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
