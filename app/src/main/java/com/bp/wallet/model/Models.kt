package com.bp.wallet.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAccount(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val mobileNumber: String = "",
    val password: String = "",
    val role: String = "user",
    val betproUsername: String = "Available Soon",
    val betproPassword: String = "Wait for Admin",
    val betproIdStatus: String = "Pending",
    val walletBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = true,
    val isSuperAdmin: Boolean = false
) {
    val isAdminRole: Boolean
        get() = isSuperAdmin || role.equals("admin", ignoreCase = true) || role.equals("Super Admin", ignoreCase = true) || id == "admin_super_1"
}

@Serializable
data class TransactionRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val type: String = "DEPOSIT", // "DEPOSIT" or "WITHDRAW"
    val amount: Double = 0.0,
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val gatewayName: String = "",
    val referenceNumber: String = "",
    val screenshotUri: String = "",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val adminNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PaymentGateway(
    val id: String = "",
    val name: String = "",
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val title: String = "",
    val accountNumber: String = "",
    val iban: String = "",
    val bankName: String = "",
    val instructions: String = "",
    val shortDescription: String = "",
    val logoUrl: String = "",
    val isEnabled: Boolean = true,
    val displayOrder: Int = 1,
    val minDeposit: Double = 500.0,
    val minWithdraw: Double = 1000.0
)

@Serializable
data class UserWithdrawalAccount(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AuditLog(
    val id: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val action: String = "",
    val details: String = "",
    val targetId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AppSettings(
    val isMaintenanceMode: Boolean = false,
    val minDepositPkr: Double = 500.0,
    val minWithdrawPkr: Double = 1000.0,
    val whatsappHelpline: String = "+923001234567",
    val exchangeWebsiteUrl: String = "https://betproexch.com",
    val announcementMessage: String = "",
    val isDepositEnabled: Boolean = true,
    val isWithdrawEnabled: Boolean = true
)

@Serializable
data class AdminNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // "INFO", "ALERT", "PROMO"
    val timestamp: Long = System.currentTimeMillis()
)

data class CelebrationEvent(
    val title: String = "Transaction Submitted!",
    val subtitle: String = "Your request has been placed successfully.",
    val message: String = subtitle,
    val amount: Double = 0.0,
    val currency: String = "PKR",
    val transactionType: String = "DEPOSIT",
    val referenceOrDetails: String = "",
    val type: String = transactionType
)

object CountryUtils {
    val supportedCurrencies = listOf("PKR", "INR", "BDT", "AED", "USD")
    val supportedCountries = listOf("Pakistan", "India", "Bangladesh", "UAE", "Global")

    fun getCountryForCurrency(currency: String): String {
        return when (currency.uppercase()) {
            "PKR" -> "Pakistan"
            "INR" -> "India"
            "BDT" -> "Bangladesh"
            "AED" -> "UAE"
            "USD" -> "Global"
            else -> "Pakistan"
        }
    }

    fun getCurrencyForCountry(country: String): String {
        return when (country.lowercase()) {
            "pakistan" -> "PKR"
            "india" -> "INR"
            "bangladesh" -> "BDT"
            "uae", "united arab emirates" -> "AED"
            else -> "USD"
        }
    }

    fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            "PKR" -> "Rs."
            "INR" -> "₹"
            "BDT" -> "৳"
            "AED" -> "AED"
            "USD" -> "$"
            else -> currency
        }
    }
}
