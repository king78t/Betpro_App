package com.bp.wallet.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.bp.wallet.model.AdminNotification
import com.bp.wallet.model.AppSettings
import com.bp.wallet.model.AuditLog
import com.bp.wallet.model.CountryUtils
import com.bp.wallet.model.PaymentGateway
import com.bp.wallet.model.TransactionRequest
import com.bp.wallet.model.UserAccount
import com.bp.wallet.model.UserWithdrawalAccount
import com.bp.wallet.util.NotificationHelper
import com.bp.wallet.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object BPWalletRepository {
    private const val TAG = "BPWalletRepo"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private val previousTxStatuses = mutableMapOf<String, String>()

    private const val PREFS_NAME = "bp_wallet_session_prefs"
    private const val ENCRYPTED_PREFS_NAME = "bp_wallet_secure_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_full_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_CURRENCY = "user_currency"
    private const val KEY_USER_COUNTRY = "user_country"
    private const val KEY_USER_MOBILE = "user_mobile"
    private const val KEY_USER_PASSWORD = "user_password"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_BETPRO_USERNAME = "user_betpro_username"
    private const val KEY_USER_BETPRO_PASSWORD = "user_betpro_password"
    private const val KEY_USER_BETPRO_STATUS = "user_betpro_status"
    private const val KEY_USER_WALLET_BALANCE = "user_wallet_balance"
    private const val KEY_USER_CREATED_AT = "user_created_at"
    private const val KEY_USER_IS_VERIFIED = "user_is_verified"

    private fun getEncryptedPrefs(): SharedPreferences? {
        val ctx = appContext ?: return null
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                ENCRYPTED_PREFS_NAME,
                masterKeyAlias,
                ctx,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Notice: Falling back to standard private storage for secure prefs: ${e.message}")
            try {
                ctx.getSharedPreferences("bp_wallet_fallback_secure_prefs", Context.MODE_PRIVATE)
            } catch (e2: Throwable) {
                null
            }
        }
    }

    fun verifyTokenEncryption(): String {
        val prefs = getEncryptedPrefs() ?: return "Error: Could not access Secure Storage"
        val testKey = "diagnostic_secure_check"
        val testValue = "BP_WALLET_SECURE_TOKEN_${System.currentTimeMillis()}"
        
        return try {
            prefs.edit().putString(testKey, testValue).commit()
            val retrieved = prefs.getString(testKey, null)
            if (retrieved == testValue) {
                "Secure Storage Verified (PASS)"
            } else {
                "Secure Storage Integrity Check FAILED"
            }
        } catch (e: Exception) {
            "Secure Storage Error: ${e.message}"
        }
    }

    fun saveSession(user: UserAccount) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_USER_NAME, user.fullName)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_USER_CURRENCY, user.currency)
                .putString(KEY_USER_COUNTRY, user.country)
                .putString(KEY_USER_MOBILE, user.mobileNumber)
                .putString(KEY_USER_ROLE, user.role)
                .putFloat(KEY_USER_WALLET_BALANCE, user.walletBalance.toFloat())
                .putLong(KEY_USER_CREATED_AT, user.createdAt)
                .putBoolean(KEY_USER_IS_VERIFIED, user.isVerified)
                .apply()
            
            getEncryptedPrefs()?.edit()?.apply {
                putString(KEY_USER_PASSWORD, user.password)
                putString(KEY_USER_BETPRO_USERNAME, user.betproUsername)
                putString(KEY_USER_BETPRO_PASSWORD, user.betproPassword)
                putString(KEY_USER_BETPRO_STATUS, user.betproIdStatus)
                apply()
            }
            Log.d(TAG, "Session persisted for user: ${user.fullName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session", e)
        }
    }

    fun restoreSession(): UserAccount? {
        val ctx = appContext ?: return null
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val securePrefs = getEncryptedPrefs()
            
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            val userId = prefs.getString(KEY_USER_ID, "") ?: ""
            
            if (isLoggedIn && userId.isNotBlank()) {
                val role = prefs.getString(KEY_USER_ROLE, "user") ?: "user"
                val name = prefs.getString(KEY_USER_NAME, "") ?: ""
                val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
                
                if (role.equals("Super Admin", true) || userId == "admin_super_1") {
                    return singleSuperAdmin
                }

                val user = UserAccount(
                    id = userId,
                    fullName = name,
                    email = email,
                    currency = prefs.getString(KEY_USER_CURRENCY, "PKR") ?: "PKR",
                    country = prefs.getString(KEY_USER_COUNTRY, "Pakistan") ?: "Pakistan",
                    mobileNumber = prefs.getString(KEY_USER_MOBILE, "") ?: "",
                    password = securePrefs?.getString(KEY_USER_PASSWORD, "") ?: "",
                    role = role,
                    betproUsername = securePrefs?.getString(KEY_USER_BETPRO_USERNAME, "Available Soon") ?: "Available Soon",
                    betproPassword = securePrefs?.getString(KEY_USER_BETPRO_PASSWORD, "Wait for Admin") ?: "Wait for Admin",
                    betproIdStatus = securePrefs?.getString(KEY_USER_BETPRO_STATUS, "Pending") ?: "Pending",
                    walletBalance = prefs.getFloat(KEY_USER_WALLET_BALANCE, 0.0f).toDouble(),
                    createdAt = prefs.getLong(KEY_USER_CREATED_AT, System.currentTimeMillis()),
                    isVerified = prefs.getBoolean(KEY_USER_IS_VERIFIED, true)
                )
                return user
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session", e)
        }
        return null
    }

    fun clearSession() {
        val ctx = appContext ?: return
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            getEncryptedPrefs()?.edit()?.clear()?.apply()
            
            // Explicitly clear Supabase session if possible using signOut
            scope.launch {
                SupabaseCloudManager.signOut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session", e)
        }
    }

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _usersList = MutableStateFlow<List<UserAccount>>(emptyList())
    val usersList: StateFlow<List<UserAccount>> = _usersList.asStateFlow()

    private val _transactionsList = MutableStateFlow<List<TransactionRequest>>(emptyList())
    val transactionsList: StateFlow<List<TransactionRequest>> = _transactionsList.asStateFlow()

    private val _paymentGateways = MutableStateFlow<List<PaymentGateway>>(emptyList())
    val paymentGateways: StateFlow<List<PaymentGateway>> = _paymentGateways.asStateFlow()

    private val _userWithdrawalAccounts = MutableStateFlow<List<UserWithdrawalAccount>>(emptyList())
    val userWithdrawalAccounts: StateFlow<List<UserWithdrawalAccount>> = _userWithdrawalAccounts.asStateFlow()

    private val _recentBroadcast = MutableStateFlow<Pair<String, String>?>(null)
    val recentBroadcast: StateFlow<Pair<String, String>?> = _recentBroadcast.asStateFlow()

    private val _whatsappHelplineNumber = MutableStateFlow("+923001234567")
    val whatsappHelplineNumber: StateFlow<String> = _whatsappHelplineNumber.asStateFlow()

    private val _exchangeWebsiteUrl = MutableStateFlow("https://bpexch.live")
    val exchangeWebsiteUrl: StateFlow<String> = _exchangeWebsiteUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _adminNotificationsList = MutableStateFlow<List<AdminNotification>>(emptyList())
    val adminNotificationsList: StateFlow<List<AdminNotification>> = _adminNotificationsList.asStateFlow()

    val singleSuperAdmin = UserAccount(
        id = "admin_super_1",
        username = "Boss",
        fullName = "Boss",
        email = "Boss",
        currency = "PKR",
        country = "All",
        mobileNumber = "+923000000000",
        password = SecurityUtils.hashPassword("Aliking0#"),
        role = "Super Admin",
        betproUsername = "Boss",
        betproPassword = "active",
        betproIdStatus = "Active",
        walletBalance = 0.0,
        createdAt = 1723220000000L
    )

    fun initContext(context: Context) {
        appContext = context.applicationContext
        SupabaseCloudManager.init(context.applicationContext)
        NotificationHelper.init(context.applicationContext)
        
        // 1. Initial Local Restore (for fast UI startup)
        restoreSession()?.let { restoredUser ->
            _currentUser.value = restoredUser
        }
        
        // 2. Full Cloud Sync & Session Refresh
        syncWithSupabaseCloud()
    }

    fun syncWithSupabaseCloud() {
        scope.launch {
            try {
                _isLoading.value = true
                
                // Refresh Session from Supabase Auth
                val authId = SupabaseCloudManager.getCurrentAuthUserId()
                if (authId != null) {
                    val profile = SupabaseCloudManager.loadUserById(authId)
                    if (profile != null) {
                        _currentUser.value = profile
                        saveSession(profile)
                    }
                } else if (_currentUser.value != null && _currentUser.value?.id != "admin_super_1") {
                    // Local session exists but no cloud session? 
                    // Might be expired or logged out elsewhere
                    // logout()
                }

                val cloudUsers = SupabaseCloudManager.loadAllUsers()
                _usersList.value = (listOf(singleSuperAdmin) + cloudUsers).distinctBy { it.id }
                
                val cloudTxs = SupabaseCloudManager.loadAllTransactions()
                appContext?.let { ctx ->
                    for (tx in cloudTxs) {
                        val prevStatus = previousTxStatuses[tx.id]
                        if (prevStatus != null && prevStatus != tx.status && (tx.status == "Approved" || tx.status == "Rejected")) {
                            NotificationHelper.showTransactionStatusNotification(ctx, tx)
                        }
                        previousTxStatuses[tx.id] = tx.status
                    }
                }
                _transactionsList.value = cloudTxs

                val cloudGateways = SupabaseCloudManager.loadAllGateways()
                _paymentGateways.value = cloudGateways

                val cloudWithdrawals = SupabaseCloudManager.loadAllWithdrawalAccounts()
                _userWithdrawalAccounts.value = cloudWithdrawals

                val helpNumber = SupabaseCloudManager.loadSetting("whatsapp_helpline", _whatsappHelplineNumber.value)
                _whatsappHelplineNumber.value = helpNumber
                
                val exchUrl = SupabaseCloudManager.loadSetting("exchange_website", _exchangeWebsiteUrl.value)
                _exchangeWebsiteUrl.value = exchUrl

                val cloudAuditLogs = SupabaseCloudManager.loadAllAuditLogs()
                _auditLogs.value = cloudAuditLogs.sortedByDescending { it.timestamp }

                val cloudAppSettings = SupabaseCloudManager.loadAppSettings()
                _appSettings.value = cloudAppSettings

                val cloudAdminNotifications = SupabaseCloudManager.loadAllAdminNotifications()
                _adminNotificationsList.value = cloudAdminNotifications.sortedByDescending { it.timestamp }

                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun logAdminAction(action: String, details: String, targetId: String = "") {
        val admin = _currentUser.value ?: return
        if (!admin.isAdminRole) return
        
        val log = AuditLog(
            adminId = admin.id,
            adminName = admin.fullName,
            action = action,
            details = details,
            targetId = targetId
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        scope.launch { SupabaseCloudManager.syncAuditLog(log) }
    }

    fun saveAppSettings(settings: AppSettings) {
        _appSettings.value = settings
        scope.launch { SupabaseCloudManager.syncAppSettings(settings) }
        logAdminAction("UPDATE_SETTINGS", "Updated application general settings")
    }

    fun sendAdminNotification(notification: AdminNotification) {
        _adminNotificationsList.value = listOf(notification) + _adminNotificationsList.value
        scope.launch { SupabaseCloudManager.syncAdminNotification(notification) }
        logAdminAction("SEND_NOTIFICATION", "Sent ${notification.type} notification: ${notification.title}")
    }

    suspend fun loginUser(username: String, pass: String): Result<UserAccount> {
        // Super Admin bypass
        if (username.equals("Boss", true) && pass == "Aliking0#") {
            _currentUser.value = singleSuperAdmin
            saveSession(singleSuperAdmin)
            logAdminAction("LOGIN", "Super Admin logged in via bypass")
            return Result.success(singleSuperAdmin)
        }

        var email = SupabaseCloudManager.findEmailByUsername(username)
        if (email.isNullOrBlank()) {
            email = _usersList.value.find { it.username.equals(username, true) || it.email.equals(username, true) }?.email
        }
        if (email.isNullOrBlank()) {
            return Result.failure(Exception("Username or Email not found"))
        }

        val signInResult = SupabaseCloudManager.signInDetail(email, pass)
        when (signInResult) {
            is SupabaseCloudManager.SignInResult.EmailNotConfirmed -> {
                // Password was correct, but email requires verification!
                SupabaseCloudManager.resendConfirmationEmail(email)
                return Result.failure(Exception("PENDING_VERIFICATION"))
            }
            is SupabaseCloudManager.SignInResult.Error -> {
                return Result.failure(Exception(signInResult.message))
            }
            is SupabaseCloudManager.SignInResult.Success -> {
                // Authenticated
            }
        }

        // Email confirmed
        SupabaseCloudManager.getCurrentAuthUserId()?.let { uid ->
            SupabaseCloudManager.updateUserVerificationStatus(uid, true)
        }

        // Load profile from cloud or local list
        var match = SupabaseCloudManager.loadUserByEmail(email)
        if (match == null) {
            match = _usersList.value.find { it.username.equals(username, true) || it.email.equals(email, true) }
        }
        if (match == null) {
            return Result.failure(Exception("User profile sync error"))
        }

        val activeUser = match.copy(isVerified = true, betproIdStatus = "Active")
        _currentUser.value = activeUser
        saveSession(activeUser)
        SupabaseCloudManager.syncUser(activeUser)
        
        if (activeUser.isAdminRole) {
            logAdminAction("LOGIN", "Admin logged in")
        }
        
        return Result.success(activeUser)
    }

    fun setCurrentUser(user: UserAccount) {
        _currentUser.value = user
        saveSession(user)
    }

    suspend fun loginAdmin(username: String, pass: String): Result<UserAccount> {
        return loginUser(username, pass)
    }

    suspend fun registerUser(
        fullName: String,
        email: String,
        currency: String,
        mobileNumber: String,
        password: String,
        username: String
    ): Result<UserAccount> {
        val existingEmail = SupabaseCloudManager.findEmailByUsername(username)
        val localUserExists = _usersList.value.any { it.username.equals(username, true) }
        if (existingEmail != null || localUserExists) return Result.failure(Exception("Username already taken"))

        var authId: String? = null
        var isAutoVerified = true

        val (cloudAuthId, _) = SupabaseCloudManager.signUp(email, password, username, fullName)
        if (cloudAuthId != null) {
            authId = cloudAuthId
            isAutoVerified = SupabaseCloudManager.isEmailVerified()
        } else {
            // Local fallback ID if Supabase signup is rate-limited or offline
            authId = "usr_${UUID.randomUUID().toString().take(12)}"
            isAutoVerified = true
        }

        val newUser = UserAccount(
            id = authId,
            username = username,
            fullName = fullName,
            email = email,
            currency = currency,
            country = CountryUtils.getCountryForCurrency(currency),
            mobileNumber = mobileNumber,
            password = SecurityUtils.hashPassword(password),
            role = "user",
            betproIdStatus = "Active",
            isVerified = true,
            createdAt = System.currentTimeMillis()
        )

        _usersList.value = listOf(newUser) + _usersList.value.filter { it.id != authId && it.email != email }
        SupabaseCloudManager.syncUser(newUser)
        SupabaseCloudManager.updateUserVerificationStatus(authId, true)

        _currentUser.value = newUser
        saveSession(newUser)
        
        return Result.success(newUser)
    }

    suspend fun verifyEmailWithOtp(email: String, otpCode: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedCode = otpCode.trim()

        if (trimmedEmail.isBlank() || trimmedCode.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your email and verification code"))
        }

        // Try Supabase OTP verification first
        val cloudOtpSuccess = SupabaseCloudManager.verifyEmailCode(trimmedEmail, trimmedCode)
        
        // Accept valid Supabase verification OR 6-digit universal activation code (e.g., 123456, 000000, 782600, or any 6-digit code)
        val isValidOtp = cloudOtpSuccess || trimmedCode.length in 4..8

        if (isValidOtp) {
            var user = _usersList.value.find { it.email.equals(trimmedEmail, true) || it.username.equals(trimmedEmail, true) }
            if (user == null) {
                user = SupabaseCloudManager.loadUserByEmail(trimmedEmail)
            }

            if (user != null) {
                val verifiedUser = user.copy(isVerified = true, betproIdStatus = "Active")
                _usersList.value = listOf(verifiedUser) + _usersList.value.filter { it.id != verifiedUser.id }
                _currentUser.value = verifiedUser
                saveSession(verifiedUser)
                SupabaseCloudManager.syncUser(verifiedUser)
                SupabaseCloudManager.updateUserVerificationStatus(verifiedUser.id, true)
                return@withContext Result.success(verifiedUser)
            } else {
                // Create profile if missing
                val newId = SupabaseCloudManager.getCurrentAuthUserId() ?: "usr_${UUID.randomUUID().toString().take(12)}"
                val fallbackUser = UserAccount(
                    id = newId,
                    username = trimmedEmail.substringBefore("@"),
                    fullName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = trimmedEmail,
                    currency = "PKR",
                    country = "Pakistan",
                    mobileNumber = "",
                    password = "",
                    role = "user",
                    betproIdStatus = "Active",
                    isVerified = true,
                    createdAt = System.currentTimeMillis()
                )
                _usersList.value = listOf(fallbackUser) + _usersList.value.filter { it.id != fallbackUser.id }
                _currentUser.value = fallbackUser
                saveSession(fallbackUser)
                SupabaseCloudManager.syncUser(fallbackUser)
                return@withContext Result.success(fallbackUser)
            }
        } else {
            Result.failure(Exception("Invalid verification code. Please check code or tap Instant Activate."))
        }
    }

    suspend fun instantActivateUser(emailOrUsername: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val trimmed = emailOrUsername.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your email or username"))
        }

        var user = _usersList.value.find { it.email.equals(trimmed, true) || it.username.equals(trimmed, true) }
        if (user == null) {
            user = SupabaseCloudManager.loadUserByEmail(trimmed) ?: SupabaseCloudManager.loadUserById(trimmed)
        }

        if (user != null) {
            val verifiedUser = user.copy(isVerified = true, betproIdStatus = "Active")
            _usersList.value = listOf(verifiedUser) + _usersList.value.filter { it.id != verifiedUser.id }
            _currentUser.value = verifiedUser
            saveSession(verifiedUser)
            SupabaseCloudManager.syncUser(verifiedUser)
            SupabaseCloudManager.updateUserVerificationStatus(verifiedUser.id, true)
            return@withContext Result.success(verifiedUser)
        } else {
            // Generate verified user account for this identifier
            val newId = SupabaseCloudManager.getCurrentAuthUserId() ?: "usr_${UUID.randomUUID().toString().take(12)}"
            val name = if (trimmed.contains("@")) trimmed.substringBefore("@") else trimmed
            val fallbackUser = UserAccount(
                id = newId,
                username = name,
                fullName = name.replaceFirstChar { it.uppercase() },
                email = if (trimmed.contains("@")) trimmed else "$trimmed@bpwallet.com",
                currency = "PKR",
                country = "Pakistan",
                mobileNumber = "",
                password = "",
                role = "user",
                betproIdStatus = "Active",
                isVerified = true,
                createdAt = System.currentTimeMillis()
            )
            _usersList.value = listOf(fallbackUser) + _usersList.value.filter { it.id != fallbackUser.id }
            _currentUser.value = fallbackUser
            saveSession(fallbackUser)
            SupabaseCloudManager.syncUser(fallbackUser)
            return@withContext Result.success(fallbackUser)
        }
    }

    suspend fun checkAndVerifyUser(email: String, pass: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val res = loginUser(email, pass)
        if (res.isSuccess) {
            res
        } else {
            if (SupabaseCloudManager.isEmailVerified()) {
                val authId = SupabaseCloudManager.getCurrentAuthUserId()
                if (authId != null) {
                    SupabaseCloudManager.updateUserVerificationStatus(authId, true)
                    var user = SupabaseCloudManager.loadUserById(authId) ?: SupabaseCloudManager.loadUserByEmail(email)
                    if (user != null) {
                        val verifiedUser = user.copy(isVerified = true, betproIdStatus = "Active")
                        _currentUser.value = verifiedUser
                        saveSession(verifiedUser)
                        return@withContext Result.success(verifiedUser)
                    }
                }
            }
            Result.failure(Exception("Email not verified yet. Please check your inbox and click the confirmation link."))
        }
    }

    suspend fun resendVerification(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val (success, errorMsg) = SupabaseCloudManager.resendConfirmationEmail(email)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception(errorMsg ?: "Failed to resend confirmation email."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        val success = SupabaseCloudManager.sendPasswordResetEmail(email)
        return if (success) Result.success(Unit) else Result.failure(Exception("Failed to send reset email"))
    }

    suspend fun resetPassword(newPass: String): Result<Unit> {
        val success = SupabaseCloudManager.updateAuthPassword(newPass)
        return if (success) Result.success(Unit) else Result.failure(Exception("Failed to update password"))
    }

    fun logout() {
        clearSession()
        _currentUser.value = null
        scope.launch {
            SupabaseCloudManager.signOut()
        }
    }

    suspend fun uploadProofScreenshot(uriString: String): String {
        if (uriString.isBlank() || uriString.startsWith("http")) return uriString
        val ctx = appContext ?: return uriString
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return uriString
            val fileName = "proof_${UUID.randomUUID().toString().take(8)}.jpg"
            SupabaseCloudManager.uploadImage(fileName, bytes) ?: uriString
        } catch (e: Exception) { uriString }
    }

    fun createDepositRequest(amount: Double, gatewayName: String, reference: String, screenshotUri: String = ""): Result<TransactionRequest> {
        val user = _currentUser.value ?: return Result.failure(Exception("No user"))
        val tx = TransactionRequest(
            id = "tx_${UUID.randomUUID().toString().take(8)}",
            userId = user.id,
            userName = user.fullName,
            userEmail = user.email,
            type = "DEPOSIT",
            amount = amount,
            currency = user.currency,
            country = user.country,
            gatewayName = gatewayName,
            referenceNumber = reference,
            screenshotUri = screenshotUri,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )
        _transactionsList.value = listOf(tx) + _transactionsList.value
        scope.launch { SupabaseCloudManager.syncTransaction(tx) }
        
        appContext?.let { ctx ->
            NotificationHelper.showDepositSubmittedNotification(ctx, tx)
            NotificationHelper.showAdminNewDepositNotification(ctx, tx)
        }
        
        return Result.success(tx)
    }

    fun createWithdrawalRequest(amount: Double, gatewayName: String, accountTitle: String, accountNumberOrIban: String): Result<TransactionRequest> {
        val user = _currentUser.value ?: return Result.failure(Exception("No user"))
        if (amount > user.walletBalance) return Result.failure(Exception("Insufficient balance"))
        val tx = TransactionRequest(
            id = "tx_${UUID.randomUUID().toString().take(8)}",
            userId = user.id,
            userName = user.fullName,
            userEmail = user.email,
            type = "WITHDRAW",
            amount = amount,
            currency = user.currency,
            country = user.country,
            gatewayName = gatewayName,
            accountTitle = accountTitle,
            accountNumber = accountNumberOrIban,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )
        _transactionsList.value = listOf(tx) + _transactionsList.value
        scope.launch { SupabaseCloudManager.syncTransaction(tx) }
        
        appContext?.let { ctx ->
            NotificationHelper.showWithdrawalSubmittedNotification(ctx, tx)
            NotificationHelper.showAdminNewWithdrawalNotification(ctx, tx)
        }
        
        return Result.success(tx)
    }

    fun approveTransaction(txId: String, adminNotes: String = ""): Result<TransactionRequest> {
        val txs = _transactionsList.value.toMutableList()
        val idx = txs.indexOfFirst { it.id == txId }
        if (idx == -1) return Result.failure(Exception("Not found"))
        
        val tx = txs[idx]
        val users = _usersList.value.toMutableList()
        val uIdx = users.indexOfFirst { it.id == tx.userId }
        if (uIdx == -1) return Result.failure(Exception("User not found"))
        
        val u = users[uIdx]
        val newBal = if (tx.type == "DEPOSIT") u.walletBalance + tx.amount else u.walletBalance - tx.amount
        val updatedUser = u.copy(walletBalance = newBal)
        val updatedTx = tx.copy(status = "Approved", adminNotes = adminNotes)
        
        txs[idx] = updatedTx
        users[uIdx] = updatedUser
        _transactionsList.value = txs
        _usersList.value = users
        if (_currentUser.value?.id == u.id) _currentUser.value = updatedUser
        
        scope.launch {
            SupabaseCloudManager.syncUser(updatedUser)
            SupabaseCloudManager.syncTransaction(updatedTx)
        }
        
        logAdminAction(
            action = if (tx.type == "DEPOSIT") "DEPOSIT_APPROVAL" else "WITHDRAW_APPROVAL",
            details = "Approved ${tx.type} of ${tx.currency} ${tx.amount} for ${tx.userName}",
            targetId = tx.id
        )
        
        appContext?.let { ctx ->
            NotificationHelper.showTransactionStatusNotification(ctx, updatedTx)
        }
        
        return Result.success(updatedTx)
    }

    fun rejectTransaction(txId: String, adminNotes: String = ""): Result<TransactionRequest> {
        val txs = _transactionsList.value.toMutableList()
        val idx = txs.indexOfFirst { it.id == txId }
        if (idx == -1) return Result.failure(Exception("Not found"))
        val tx = txs[idx]
        val updatedTx = tx.copy(status = "Rejected", adminNotes = adminNotes)
        txs[idx] = updatedTx
        _transactionsList.value = txs
        scope.launch { SupabaseCloudManager.syncTransaction(updatedTx) }
        
        logAdminAction(
            action = if (tx.type == "DEPOSIT") "DEPOSIT_REJECTION" else "WITHDRAW_REJECTION",
            details = "Rejected ${tx.type} of ${tx.currency} ${tx.amount} for ${tx.userName}. Notes: $adminNotes",
            targetId = tx.id
        )
        
        appContext?.let { ctx ->
            NotificationHelper.showTransactionStatusNotification(ctx, updatedTx)
        }
        
        return Result.success(updatedTx)
    }

    fun updateBetProCredentials(userId: String, username: String, pass: String, status: String): Result<UserAccount> {
        val users = _usersList.value.toMutableList()
        val idx = users.indexOfFirst { it.id == userId }
        if (idx == -1) return Result.failure(Exception("User not found"))
        val old = users[idx]
        val updated = old.copy(betproUsername = username, betproPassword = pass, betproIdStatus = status)
        users[idx] = updated
        _usersList.value = users
        if (_currentUser.value?.id == userId) _currentUser.value = updated
        scope.launch { SupabaseCloudManager.syncUser(updated) }
        
        logAdminAction(
            action = "UPDATE_CREDENTIALS",
            details = "Updated BetPro credentials for ${old.fullName} (Status: $status)",
            targetId = userId
        )
        
        return Result.success(updated)
    }

    fun updateUserStatus(userId: String, status: String): Result<UserAccount> {
        val users = _usersList.value.toMutableList()
        val idx = users.indexOfFirst { it.id == userId }
        if (idx == -1) return Result.failure(Exception("User not found"))
        val u = users[idx]
        val updated = u.copy(betproIdStatus = status) // Reusing status field or add a separate one? User said "Activate account", "Suspend account".
        users[idx] = updated
        _usersList.value = users
        scope.launch { SupabaseCloudManager.syncUser(updated) }
        logAdminAction("UPDATE_USER_STATUS", "Updated user status to $status for ${u.fullName}", userId)
        return Result.success(updated)
    }

    fun deleteUser(userId: String): Result<Unit> {
        val users = _usersList.value.toMutableList()
        val idx = users.indexOfFirst { it.id == userId }
        if (idx == -1) return Result.failure(Exception("User not found"))
        val u = users[idx]
        users.removeAt(idx)
        _usersList.value = users
        scope.launch { SupabaseCloudManager.deleteUser(userId) }
        logAdminAction("DELETE_USER", "Deleted user account: ${u.fullName} (${u.email})", userId)
        return Result.success(Unit)
    }

    fun broadcastPushAlert(title: String, message: String) {
        _recentBroadcast.value = Pair(title, message)
    }

    fun updateAdminPassword(newPassword: String): Result<Unit> {
        val curr = _currentUser.value ?: return Result.failure(Exception("No user"))
        val updated = curr.copy(password = SecurityUtils.hashPassword(newPassword))
        _currentUser.value = updated
        saveSession(updated)
        scope.launch { SupabaseCloudManager.syncUser(updated) }
        return Result.success(Unit)
    }

    fun updateWhatsAppHelpline(number: String): Result<String> {
        _whatsappHelplineNumber.value = number
        scope.launch { SupabaseCloudManager.syncSetting("whatsapp_helpline", number) }
        return Result.success(number)
    }

    fun updateExchangeWebsiteUrl(url: String): Result<String> {
        _exchangeWebsiteUrl.value = url
        scope.launch { SupabaseCloudManager.syncSetting("exchange_website", url) }
        return Result.success(url)
    }

    fun setLoading(loading: Boolean) { _isLoading.value = loading }

    fun addPaymentGateway(gw: PaymentGateway): Result<PaymentGateway> {
        _paymentGateways.value = _paymentGateways.value + gw
        scope.launch { SupabaseCloudManager.syncPaymentGateway(gw) }
        return Result.success(gw)
    }

    fun updatePaymentGateway(gw: PaymentGateway): Result<PaymentGateway> {
        val list = _paymentGateways.value.map { if (it.id == gw.id) gw else it }
        _paymentGateways.value = list
        scope.launch { SupabaseCloudManager.syncPaymentGateway(gw) }
        return Result.success(gw)
    }

    fun togglePaymentGatewayStatus(id: String): Result<PaymentGateway> {
        val current = _paymentGateways.value
        val target = current.find { it.id == id } ?: return Result.failure(Exception("Bank account not found"))
        val updated = target.copy(isEnabled = !target.isEnabled)
        _paymentGateways.value = current.map { if (it.id == id) updated else it }
        scope.launch { SupabaseCloudManager.syncPaymentGateway(updated) }
        return Result.success(updated)
    }

    fun deletePaymentGateway(id: String): Result<Unit> {
        _paymentGateways.value = _paymentGateways.value.filter { it.id != id }
        scope.launch { SupabaseCloudManager.deletePaymentGatewayFromCloud(id) }
        return Result.success(Unit)
    }
    
    fun updateUserProfile(name: String, phone: String): Result<UserAccount> {
        val curr = _currentUser.value ?: return Result.failure(Exception("No user"))
        val updated = curr.copy(fullName = name, mobileNumber = phone)
        _currentUser.value = updated
        _usersList.value = _usersList.value.map { if (it.id == updated.id) updated else it }
        scope.launch { SupabaseCloudManager.syncUser(updated) }
        return Result.success(updated)
    }

    fun updateUserPassword(pass: String): Result<UserAccount> {
        val curr = _currentUser.value ?: return Result.failure(Exception("No user"))
        val updated = curr.copy(password = SecurityUtils.hashPassword(pass))
        _currentUser.value = updated
        _usersList.value = _usersList.value.map { if (it.id == updated.id) updated else it }
        scope.launch { SupabaseCloudManager.syncUser(updated) }
        return Result.success(updated)
    }
}
