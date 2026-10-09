package com.example.data.local

import com.example.data.model.*
import com.example.util.DateUtils

object InitialDataSeeder {

    suspend fun seedDatabase(database: AppDatabase) {
        val bizDao = database.businessDao()
        val planDao = database.planDao()
        val memberDao = database.memberDao()
        val subDao = database.subscriptionDao()
        val payDao = database.paymentDao()
        val attDao = database.attendanceDao()
        val notifDao = database.notificationDao()

        // Seed Business
        val business = Business(
            id = 1,
            name = "FitPlus Gym",
            type = BusinessType.GYM,
            ownerName = "Rahul Sharma",
            phone = "9876543210",
            email = "fitplus@example.com",
            address = "Commercial Complex, Sector 18",
            branch = "Main Branch",
            currencySymbol = "₹",
            isLoggedIn = true
        )
        bizDao.insertOrUpdate(business)

        // Seed Plans
        val plan1 = SubscriptionPlan(id = 1, name = "Monthly Gym", durationDays = 30, price = 1500.0, description = "Full gym floor access", businessType = BusinessType.GYM, isActive = true)
        val plan2 = SubscriptionPlan(id = 2, name = "Quarterly Gym", durationDays = 90, price = 4000.0, description = "3-month access with trainer support", businessType = BusinessType.GYM, isActive = true)
        val plan3 = SubscriptionPlan(id = 3, name = "Annual VIP", durationDays = 365, price = 12000.0, description = "Unlimited access & personal locker", businessType = BusinessType.GYM, isActive = true)
        planDao.insertPlan(plan1)
        planDao.insertPlan(plan2)
        planDao.insertPlan(plan3)

        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        // Seed Members
        val m1 = Member(id = 1, memberCode = "FP-1001", name = "Naveen Sharma", phone = "9820011221", email = "naveen.sharma@example.com", status = MemberStatus.ACTIVE, planId = 1, planName = "Monthly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rg")
        val m2 = Member(id = 2, memberCode = "FP-1002", name = "Rohit Gupta", phone = "9820011222", email = "rohit.gupta@example.com", status = MemberStatus.ACTIVE, planId = 2, planName = "Quarterly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rh")
        val m3 = Member(id = 3, memberCode = "FP-1003", name = "Priya Kulkarni", phone = "9820011223", email = "priya.kulkarni@example.com", status = MemberStatus.ACTIVE, planId = 3, planName = "Annual VIP", qrToken = "01m11ncqtpjqfzcwe8anapg6ri")
        val m4 = Member(id = 4, memberCode = "FP-1004", name = "Sneha Joshi", phone = "9820011224", email = "sneha.joshi@example.com", status = MemberStatus.ACTIVE, planId = 1, planName = "Monthly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rj")
        val m5 = Member(id = 5, memberCode = "FP-1005", name = "Rahul Patil", phone = "9820011225", email = "rahul.patil@example.com", status = MemberStatus.EXPIRING_SOON, planId = 1, planName = "Monthly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rk")
        val m6 = Member(id = 6, memberCode = "FP-1006", name = "Amit Verma", phone = "9820011226", email = "amit.verma@example.com", status = MemberStatus.EXPIRING_SOON, planId = 2, planName = "Quarterly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rl")
        val m7 = Member(id = 7, memberCode = "FP-1007", name = "Karan Mehta", phone = "9820011227", email = "karan.mehta@example.com", status = MemberStatus.EXPIRED, planId = 1, planName = "Monthly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rm")
        val m8 = Member(id = 8, memberCode = "FP-1008", name = "Rajesh Khanna", phone = "9820011228", email = "rajesh.khanna@example.com", status = MemberStatus.EXPIRED, planId = 1, planName = "Monthly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6rn")
        val m9 = Member(id = 9, memberCode = "FP-1009", name = "Deepak Sharma", phone = "9820011229", email = "deepak.sharma@example.com", status = MemberStatus.PAYMENT_PENDING, planId = 1, planName = "Monthly Gym", qrToken = "01m11ncqtpjqfzcwe8anapg6ro")

        val members = listOf(m1, m2, m3, m4, m5, m6, m7, m8, m9)
        memberDao.insertAllMembers(members)

        // Subscriptions
        subDao.insertSubscription(Subscription(id = 1, memberId = 1, planId = 1, planName = "Monthly Gym", price = 1500.0, startDate = now - (5L * oneDay), endDate = now + (25L * oneDay), status = SubscriptionStatus.ACTIVE))
        subDao.insertSubscription(Subscription(id = 2, memberId = 2, planId = 2, planName = "Quarterly Gym", price = 4000.0, startDate = now - (10L * oneDay), endDate = now + (80L * oneDay), status = SubscriptionStatus.ACTIVE))
        subDao.insertSubscription(Subscription(id = 3, memberId = 3, planId = 3, planName = "Annual VIP", price = 12000.0, startDate = now - (30L * oneDay), endDate = now + (335L * oneDay), status = SubscriptionStatus.ACTIVE))
        subDao.insertSubscription(Subscription(id = 4, memberId = 4, planId = 1, planName = "Monthly Gym", price = 1500.0, startDate = now - (2L * oneDay), endDate = now + (28L * oneDay), status = SubscriptionStatus.ACTIVE))
        subDao.insertSubscription(Subscription(id = 5, memberId = 5, planId = 1, planName = "Monthly Gym", price = 1500.0, startDate = now - (28L * oneDay), endDate = now + (2L * oneDay), status = SubscriptionStatus.EXPIRING_SOON))
        subDao.insertSubscription(Subscription(id = 6, memberId = 6, planId = 2, planName = "Quarterly Gym", price = 4000.0, startDate = now - (87L * oneDay), endDate = now + (3L * oneDay), status = SubscriptionStatus.EXPIRING_SOON))
        subDao.insertSubscription(Subscription(id = 7, memberId = 7, planId = 1, planName = "Monthly Gym", price = 1500.0, startDate = now - (35L * oneDay), endDate = now - (5L * oneDay), status = SubscriptionStatus.EXPIRED))
        subDao.insertSubscription(Subscription(id = 8, memberId = 8, planId = 1, planName = "Monthly Gym", price = 1500.0, startDate = now - (40L * oneDay), endDate = now - (10L * oneDay), status = SubscriptionStatus.EXPIRED))
        subDao.insertSubscription(Subscription(id = 9, memberId = 9, planId = 1, planName = "Monthly Gym", price = 1500.0, startDate = now - (6L * oneDay), endDate = now + (24L * oneDay), status = SubscriptionStatus.PENDING_PAYMENT))

        // Payments (totals ₹ 45,000 for monthly collection display)
        payDao.insertPayment(Payment(id = 1, memberId = 1, memberName = "Naveen Sharma", subscriptionId = 1, amount = 1500.0, paymentDate = now - (5L * oneDay), paymentMode = PaymentMode.CASH, invoiceNumber = "INV-1001", status = PaymentStatus.PAID))
        payDao.insertPayment(Payment(id = 2, memberId = 2, memberName = "Rohit Gupta", subscriptionId = 2, amount = 4000.0, paymentDate = now - (10L * oneDay), paymentMode = PaymentMode.UPI, invoiceNumber = "INV-1002", status = PaymentStatus.PAID))
        payDao.insertPayment(Payment(id = 3, memberId = 3, memberName = "Priya Kulkarni", subscriptionId = 3, amount = 12000.0, paymentDate = now - (3L * oneDay), paymentMode = PaymentMode.UPI, invoiceNumber = "INV-1003", status = PaymentStatus.PAID))
        payDao.insertPayment(Payment(id = 4, memberId = 4, memberName = "Sneha Joshi", subscriptionId = 4, amount = 1500.0, paymentDate = now - (2L * oneDay), paymentMode = PaymentMode.CASH, invoiceNumber = "INV-1004", status = PaymentStatus.PAID))
        payDao.insertPayment(Payment(id = 5, memberId = 5, memberName = "Rahul Patil", subscriptionId = 5, amount = 14000.0, paymentDate = now - (1L * oneDay), paymentMode = PaymentMode.CARD, invoiceNumber = "INV-1005", status = PaymentStatus.PAID))
        payDao.insertPayment(Payment(id = 6, memberId = 6, memberName = "Amit Verma", subscriptionId = 6, amount = 12000.0, paymentDate = now - (4L * oneDay), paymentMode = PaymentMode.UPI, invoiceNumber = "INV-1006", status = PaymentStatus.PAID))
        payDao.insertPayment(Payment(id = 7, memberId = 9, memberName = "Deepak Sharma", subscriptionId = 9, amount = 1500.0, paymentDate = now - (6L * oneDay), paymentMode = PaymentMode.CASH, invoiceNumber = "INV-1007", status = PaymentStatus.PENDING))

        // Today's Attendance (4 check-ins for today!)
        val todayStr = DateUtils.getTodayDbDate()
        attDao.insertAttendance(Attendance(id = 1, memberId = 1, memberCode = "FP-1001", memberName = "Naveen Sharma", dateString = todayStr, timeString = "08:15 AM", timestamp = now - 7200000L, type = AttendanceType.CHECK_IN))
        attDao.insertAttendance(Attendance(id = 2, memberId = 2, memberCode = "FP-1002", memberName = "Rohit Gupta", dateString = todayStr, timeString = "08:45 AM", timestamp = now - 5400000L, type = AttendanceType.CHECK_IN))
        attDao.insertAttendance(Attendance(id = 3, memberId = 3, memberCode = "FP-1003", memberName = "Priya Kulkarni", dateString = todayStr, timeString = "09:10 AM", timestamp = now - 3600000L, type = AttendanceType.CHECK_IN))
        attDao.insertAttendance(Attendance(id = 4, memberId = 4, memberCode = "FP-1004", memberName = "Sneha Joshi", dateString = todayStr, timeString = "09:30 AM", timestamp = now - 1800000L, type = AttendanceType.CHECK_IN))
    }
}
