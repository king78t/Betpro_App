package com.bp.uunwlm.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

object CountryUtils {
    val ALL_COUNTRIES = listOf(
        "Pakistan",
        "UAE",
        "Saudi Arabia"
    )

    val SUPPORTED_CURRENCIES = listOf(
        "PKR",
        "AED",
        "SAR"
    )
    
    fun getCountryForCurrency(currency: String): String = when (currency.uppercase()) {
        "AED" -> "UAE"
        "SAR" -> "Saudi Arabia"
        else -> "Pakistan"
    }

    fun getCurrencyForCountry(country: String): String = when (country) {
        "UAE" -> "AED"
        "Saudi Arabia" -> "SAR"
        else -> "PKR"
    }
}

@Serializable
data class UserAccount(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("username") @SerialName("username") val username: String = "",
    @SerializedName("full_name") @SerialName("full_name") val fullName: String = "",
    @SerializedName("email") @SerialName("email") val email: String = "",
    @SerializedName("currency") @SerialName("currency") val currency: String = "PKR",
    @SerializedName("country") @SerialName("country") val country: String = "Pakistan",
    @SerializedName("phone") @SerialName("phone") val mobileNumber: String = "",
    @SerializedName("password_hash") @SerialName("password_hash") val password: String = "",
    @SerializedName("role") @SerialName("role") val role: String = "user", // "user", "Super Admin"
    @SerializedName("betpro_username") @SerialName("betpro_username") val betproUsername: String = "Available Soon",
    @SerializedName("betpro_password") @SerialName("betpro_password") val betproPassword: String = "Wait for Admin",
    @SerializedName("status") @SerialName("status") val betproIdStatus: String = "Pending", // "Pending", "Active", "Blocked"
    @SerializedName("wallet_balance") @SerialName("wallet_balance") val walletBalance: Double = 0.0,
    @SerializedName("created_at") @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("is_verified") @SerialName("is_verified") val isVerified: Boolean = true
) {
    val displayCurrencySymbol: String
        get() = when (currency) {
            "PKR" -> "Rs "
            "AED" -> "AED "
            "SAR" -> "SAR "
            "INR" -> "₹ "
            "BDT" -> "৳ "
            "GBP" -> "£ "
            "USDT", "USD" -> "$ "
            else -> "Rs "
        }

    val isSuperAdmin: Boolean
        get() = role.equals("Super Admin", ignoreCase = true) ||
                role.equals("admin", ignoreCase = true) ||
                role.equals("SuperAdmin", ignoreCase = true) ||
                role.equals("ADMIN", ignoreCase = true)

    val isAdminRole: Boolean
        get() = isSuperAdmin

    val canModifyData: Boolean
        get() = isSuperAdmin

    fun hasAccessToCountry(targetCountry: String): Boolean {
        if (isSuperAdmin) return true
        return country.equals(targetCountry, ignoreCase = true)
    }
}

@Serializable
data class TransactionRequest(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("user_id") @SerialName("user_id") val userId: String = "",
    @SerializedName("user_name") @SerialName("user_name") val userName: String = "",
    @SerializedName("user_email") @SerialName("user_email") val userEmail: String = "",
    @SerializedName("type") @SerialName("type") val type: String = "DEPOSIT", // "DEPOSIT" or "WITHDRAW"
    @SerializedName("amount") @SerialName("amount") val amount: Double = 0.0,
    @SerializedName("currency") @SerialName("currency") val currency: String = "PKR",
    @SerializedName("country") @SerialName("country") val country: String = "Pakistan",
    @SerializedName("gateway_name") @SerialName("gateway_name") val gatewayName: String = "EasyPaisa",
    @SerializedName("account_title") @SerialName("account_title") val accountTitle: String = "",
    @SerializedName("account_number") @SerialName("account_number") val accountNumber: String = "",
    @SerializedName("reference_number") @SerialName("reference_number") val referenceNumber: String = "",
    @SerializedName("screenshot_uri") @SerialName("screenshot_uri") val screenshotUri: String = "", // Optional screenshot URI or proof image
    @SerializedName("admin_notes") @SerialName("admin_notes") val adminNotes: String = "", // Admin notes upon approval/rejection
    @SerializedName("status") @SerialName("status") val status: String = "Pending", // "Pending", "Approved", "Rejected"
    @SerializedName("timestamp") @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PaymentGateway(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("name") @SerialName("name") val name: String = "",
    @SerializedName("currency") @SerialName("currency") val currency: String = "PKR",
    @SerializedName("country") @SerialName("country") val country: String = "Pakistan",
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("account_number") @SerialName("account_number") val accountNumber: String = "",
    @SerializedName("iban") @SerialName("iban") val iban: String = "",
    @SerializedName("bank_name") @SerialName("bank_name") val bankName: String = "",
    @SerializedName("instructions") @SerialName("instructions") val instructions: String = "Please transfer the exact amount to the account details below and attach a screenshot of your payment receipt.",
    @SerializedName("short_description") @SerialName("short_description") val shortDescription: String = "Instant 24/7 Digital Transfer",
    @SerializedName("logo_url") @SerialName("logo_url") val logoUrl: String = "",
    @SerializedName("is_enabled") @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerializedName("display_order") @SerialName("display_order") val displayOrder: Int = 1,
    @SerializedName("min_deposit") @SerialName("min_deposit") val minDeposit: Double = 500.0,
    @SerializedName("min_withdraw") @SerialName("min_withdraw") val minWithdraw: Double = 1000.0
)


@Serializable
data class UserWithdrawalAccount(
    @SerializedName("id") @SerialName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("user_id") @SerialName("user_id") val userId: String = "",
    @SerializedName("type") @SerialName("type") val type: String = "EasyPaisa", // "EasyPaisa", "JazzCash", "Bank Account"
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("account_number") @SerialName("account_number") val accountNumber: String = "",
    @SerializedName("iban") @SerialName("iban") val iban: String = "",
    @SerializedName("bank_name") @SerialName("bank_name") val bankName: String = ""
)

@Serializable
data class AuditLog(
    @SerializedName("id") @SerialName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("admin_id") @SerialName("admin_id") val adminId: String = "",
    @SerializedName("admin_name") @SerialName("admin_name") val adminName: String = "",
    @SerializedName("action") @SerialName("action") val action: String = "", // "LOGIN", "LOGOUT", "DEPOSIT_APPROVAL", "WITHDRAW_APPROVAL", "USER_CREATION", "USER_SUSPENSION"
    @SerializedName("details") @SerialName("details") val details: String = "",
    @SerializedName("target_id") @SerialName("target_id") val targetId: String = "",
    @SerializedName("timestamp") @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AppSettings(
    @SerializedName("app_name") @SerialName("app_name") val appName: String = "BP Wallet",
    @SerializedName("support_whatsapp") @SerialName("support_whatsapp") val supportWhatsapp: String = "",
    @SerializedName("support_email") @SerialName("support_email") val supportEmail: String = "",
    @SerializedName("terms_url") @SerialName("terms_url") val termsUrl: String = "",
    @SerializedName("privacy_policy_url") @SerialName("privacy_policy_url") val privacyPolicyUrl: String = "",
    @SerializedName("last_updated") @SerialName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class AdminNotification(
    @SerializedName("id") @SerialName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("message") @SerialName("message") val message: String = "",
    @SerializedName("type") @SerialName("type") val type: String = "ANNOUNCEMENT", // "ANNOUNCEMENT", "MAINTENANCE", "SECURITY"
    @SerializedName("timestamp") @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class CelebrationEvent(
    val title: String,
    val subtitle: String,
    val amount: Double,
    val currency: String,
    val transactionType: String, // "DEPOSIT" or "WITHDRAW"
    val referenceOrDetails: String = ""
)


