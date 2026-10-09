package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppHelper {

    fun generateExpiryReminderMessage(
        memberName: String,
        businessType: String,
        amount: Double,
        currencySymbol: String,
        dueDateFormatted: String,
        daysLeft: Int
    ): String {
        return "Hi $memberName, your ${businessType.lowercase()} membership fee of $currencySymbol${amount.toInt()} is due on $dueDateFormatted ($daysLeft days remaining). Please renew your subscription to continue uninterrupted access."
    }

    fun generatePaymentReceiptMessage(
        memberName: String,
        businessName: String,
        amount: Double,
        currencySymbol: String,
        invoiceNumber: String,
        validTill: String
    ): String {
        return "Receipt from $businessName:\nReceived $currencySymbol${amount.toInt()} from $memberName.\nInvoice: $invoiceNumber\nValid till: $validTill.\nThank you!"
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
