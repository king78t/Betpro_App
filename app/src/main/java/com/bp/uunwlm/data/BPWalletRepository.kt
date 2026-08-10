package com.bp.uunwlm.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.bp.uunwlm.model.AdminNotification
import com.bp.uunwlm.model.AppSettings
import com.bp.uunwlm.model.AuditLog
import com.bp.uunwlm.model.CountryUtils
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import com.bp.uunwlm.model.UserWithdrawalAccount
import com.bp.uunwlm.util.NotificationHelper
import com.bp.uunwlm.util.SecurityUtils
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
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

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
        } catch (e: Exception) {
            Log.e(TAG, "Critical: Error creating EncryptedSharedPreferences", e)
            null
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

    fun isBiometricEnabled(): Boolean {
        val ctx = appContext ?: return false
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun clearSession() {
        val ctx = appContext ?: return
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            getEncryptedPrefs()?.edit()?.clear()?.apply()
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

        val email = SupabaseCloudManager.findEmailByUsername(username)
            ?: return Result.failure(Exception("Username not found"))

        val success = SupabaseCloudManager.signIn(email, pass)
        if (!success) {
            return Result.failure(Exception("Invalid password"))
        }

        val isVerified = SupabaseCloudManager.isEmailVerified()
        
        // Load user data from DB
        val match = _usersList.value.find { it.username.equals(username, true) }
            ?: return Result.failure(Exception("User profile sync error"))

        if (!isVerified) {
            return Result.failure(Exception("PENDING_VERIFICATION"))
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
        // Check if username exists in DB for production reliability
        val existingEmail = SupabaseCloudManager.findEmailByUsername(username)
        if (existingEmail != null) return Result.failure(Exception("Username already taken"))

        val authId = SupabaseCloudManager.signUp(email, password, username, fullName)
            ?: return Result.failure(Exception("Failed to create authentication account"))

        val newUser = UserAccount(
            id = authId,
            username = username,
            fullName = fullName,
            email = email,
            currency = currency,
            country = CountryUtils.getCountryForCurrency(currency),
            mobileNumber = mobileNumber,
            password = "", // Don't store local password hash if using Supabase Auth properly
            role = "user",
            betproIdStatus = "Pending Verification",
            isVerified = false,
            createdAt = System.currentTimeMillis()
        )

        _usersList.value = listOf(newUser) + _usersList.value
        SupabaseCloudManager.syncUser(newUser)
        
        return Result.success(newUser)
    }

    suspend fun verifyOtp(email: String, otp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = SupabaseCloudManager.verifyOtp(email, otp)
            if (success) {
                syncWithSupabaseCloud()
                Result.success(true)
            } else {
                Result.failure(Exception("Invalid or expired OTP code."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerification(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = SupabaseCloudManager.resendOtp(email)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to resend verification code."))
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
    
    fun setBiometricAuthenticated(auth: Boolean) {}

    fun addPaymentGateway(vararg args: Any): Result<Unit> = Result.success(Unit)
    fun updatePaymentGateway(vararg args: Any): Result<Unit> = Result.success(Unit)
    fun togglePaymentGatewayStatus(id: String): Result<PaymentGateway> = Result.failure(Exception("Not implemented"))
    fun deletePaymentGateway(id: String): Result<Unit> = Result.success(Unit)
    
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
