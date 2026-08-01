package com.example.model

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

data class UserAccount(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val mobileNumber: String = "",
    val password: String = "",
    val role: String = "user", // "user", "Super Admin", "Country Super Master", "Support Staff", "Read Only User"
    val betproUsername: String = "Available Soon",
    val betproPassword: String = "Wait for Admin",
    val betproIdStatus: String = "Pending", // "Pending", "Active", "Blocked"
    val walletBalance: Double = 0.0,
    val masterAgentName: String = "Pakistan Super Master",
    val assignedMasterId: String = "ma_pk",
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = true
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
        get() = role == "Super Admin" || role == "admin" || role == "SuperAdmin"

    val isCountrySuperMaster: Boolean
        get() = role == "Country Super Master"

    val isSupportStaff: Boolean
        get() = role == "Support Staff"

    val isReadOnlyUser: Boolean
        get() = role == "Read Only User"

    val canModifyData: Boolean
        get() = isSuperAdmin || isCountrySuperMaster || isSupportStaff

    fun hasAccessToCountry(targetCountry: String): Boolean {
        if (isSuperAdmin) return true
        return country.equals(targetCountry, ignoreCase = true)
    }
}

data class TransactionRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val type: String = "DEPOSIT", // "DEPOSIT" or "WITHDRAW"
    val amount: Double = 0.0,
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val gatewayName: String = "EasyPaisa",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val referenceNumber: String = "",
    val screenshotUri: String = "", // Optional screenshot URI or proof image
    val adminNotes: String = "", // Admin notes upon approval/rejection
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val timestamp: Long = System.currentTimeMillis()
)

data class MasterAgent(
    val id: String = "",
    val name: String = "",
    val loginPassword: String = "",
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val role: String = "Country Super Master", // "Country Super Master", "Super Master", "Master Agent", "Sub Agent"
    val creditLimit: Double = 200000.0,
    val marginShare: Double = 80.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class PaymentGateway(
    val id: String = "",
    val name: String = "",
    val currency: String = "PKR",
    val country: String = "Pakistan",
    val title: String = "",
    val accountNumber: String = "",
    val iban: String = "",
    val bankName: String = "",
    val instructions: String = "Please transfer the exact amount to the account details below and attach a screenshot of your payment receipt.",
    val shortDescription: String = "Instant 24/7 Digital Transfer",
    val logoUrl: String = "",
    val isEnabled: Boolean = true,
    val displayOrder: Int = 1,
    val minDeposit: Double = 500.0,
    val minWithdraw: Double = 1000.0
)


data class UserWithdrawalAccount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val type: String = "EasyPaisa", // "EasyPaisa", "JazzCash", "Bank Account"
    val title: String = "",
    val accountNumber: String = "",
    val iban: String = "",
    val bankName: String = ""
)


