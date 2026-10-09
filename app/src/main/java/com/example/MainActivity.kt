package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.BusinessType
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GymViewModel

object Screen {
    const val SELECT_USER_TYPE = "select_user_type"
    const val LOGIN = "login/{userType}"
    const val REGISTRATION = "registration/{userType}"
    const val DASHBOARD = "dashboard"
    const val MEMBERS = "members"
    const val ADD_MEMBER = "add_member"
    const val MEMBER_DETAIL = "member_detail/{memberId}"
    const val QR_ATTENDANCE = "qr_attendance"
    const val PAYMENTS = "payments"
    const val RENEW = "renew/{memberId}"
    const val EXPIRING = "expiring"
    const val OVERDUE = "overdue"
    const val PLANS = "plans"
    const val REPORTS = "reports"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    fun login(type: BusinessType) = "login/${type.name}"
    fun registration(type: BusinessType) = "registration/${type.name}"
    fun memberDetail(id: Long) = "member_detail/$id"
    fun renew(id: Long) = "renew/$id"
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : BottomNavItem(Screen.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Members : BottomNavItem(Screen.MEMBERS, "Members", Icons.Filled.People, Icons.Outlined.People)
    object Attendance : BottomNavItem(Screen.QR_ATTENDANCE, "Scan QR", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner)
    object Payments : BottomNavItem(Screen.PAYMENTS, "Payments", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Reports : BottomNavItem(Screen.REPORTS, "Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

class MainActivity : ComponentActivity() {

    private val viewModel: GymViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: GymViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Members,
        BottomNavItem.Attendance,
        BottomNavItem.Payments,
        BottomNavItem.Reports
    )

    val showBottomBar = currentRoute in listOf(
        Screen.DASHBOARD,
        Screen.MEMBERS,
        Screen.QR_ATTENDANCE,
        Screen.PAYMENTS,
        Screen.REPORTS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.SELECT_USER_TYPE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.SELECT_USER_TYPE) {
                SelectUserTypeScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { type ->
                        navController.navigate(Screen.login(type))
                    }
                )
            }

            composable(
                route = Screen.LOGIN,
                arguments = listOf(navArgument("userType") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeStr = backStackEntry.arguments?.getString("userType") ?: BusinessType.GYM.name
                val userType = try {
                    BusinessType.valueOf(typeStr)
                } catch (e: Exception) {
                    BusinessType.GYM
                }
                LoginScreen(
                    userType = userType,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Screen.DASHBOARD) {
                            popUpTo(Screen.SELECT_USER_TYPE) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.REGISTRATION,
                arguments = listOf(navArgument("userType") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeStr = backStackEntry.arguments?.getString("userType") ?: BusinessType.GYM.name
                val userType = try {
                    BusinessType.valueOf(typeStr)
                } catch (e: Exception) {
                    BusinessType.GYM
                }
                RegistrationScreen(
                    userType = userType,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRegistrationSuccess = {
                        navController.navigate(Screen.DASHBOARD) {
                            popUpTo(Screen.SELECT_USER_TYPE) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMembers = { navController.navigate(Screen.MEMBERS) },
                    onNavigateToAddMember = { navController.navigate(Screen.ADD_MEMBER) },
                    onNavigateToAttendance = { navController.navigate(Screen.QR_ATTENDANCE) },
                    onNavigateToPayments = { navController.navigate(Screen.PAYMENTS) },
                    onNavigateToExpiring = { navController.navigate(Screen.EXPIRING) },
                    onNavigateToOverdue = { navController.navigate(Screen.OVERDUE) },
                    onNavigateToSettings = { navController.navigate(Screen.SETTINGS) },
                    onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                    onNavigateToReports = { navController.navigate(Screen.REPORTS) },
                    onNavigateToMemberDetail = { id -> navController.navigate(Screen.memberDetail(id)) },
                    onNavigateToRenew = { id -> navController.navigate(Screen.renew(id)) }
                )
            }

            composable(Screen.MEMBERS) {
                MembersScreen(
                    viewModel = viewModel,
                    onNavigateToAddMember = { navController.navigate(Screen.ADD_MEMBER) },
                    onNavigateToMemberDetail = { id -> navController.navigate(Screen.memberDetail(id)) }
                )
            }

            composable(Screen.ADD_MEMBER) {
                AddEditMemberScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.MEMBER_DETAIL,
                arguments = listOf(navArgument("memberId") { type = NavType.LongType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
                MemberDetailScreen(
                    memberId = memberId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRenew = { id -> navController.navigate(Screen.renew(id)) }
                )
            }

            composable(
                route = Screen.RENEW,
                arguments = listOf(navArgument("memberId") { type = NavType.LongType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
                RenewSubscriptionScreen(
                    memberId = memberId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.QR_ATTENDANCE) {
                QrAttendanceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMemberDetail = { id -> navController.navigate(Screen.memberDetail(id)) }
                )
            }

            composable(Screen.PAYMENTS) {
                PaymentsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PLANS) {
                PlansScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EXPIRING) {
                ExpiringMembersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMemberDetail = { id -> navController.navigate(Screen.memberDetail(id)) }
                )
            }

            composable(Screen.OVERDUE) {
                OverdueMembersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMemberDetail = { id -> navController.navigate(Screen.memberDetail(id)) }
                )
            }

            composable(Screen.REPORTS) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.NOTIFICATIONS) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlans = { navController.navigate(Screen.PLANS) },
                    onNavigateToAuth = {
                        navController.navigate(Screen.SELECT_USER_TYPE) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
