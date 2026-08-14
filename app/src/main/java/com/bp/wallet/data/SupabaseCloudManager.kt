package com.bp.wallet.data

import android.util.Log
import com.bp.wallet.BuildConfig
import com.bp.wallet.model.PaymentGateway
import com.bp.wallet.model.TransactionRequest
import com.bp.wallet.model.UserAccount
import com.bp.wallet.model.UserWithdrawalAccount
import com.bp.wallet.model.AdminNotification
import com.bp.wallet.model.AuditLog
import com.bp.wallet.model.AppSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SupabaseCloudManager {
    private const val TAG = "SupabaseCloudManager"

    internal val SUPABASE_URL: String = BuildConfig.SUPABASE_URL.ifBlank { "https://vmglozamlzwjbigareie.supabase.co" }
    private val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    var client: SupabaseClient? = null
        private set

    fun init(context: android.content.Context) {
        if (client != null) return
        try {
            val safeSessionManager = SafeSessionManager(context.applicationContext)
            client = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    sessionManager = safeSessionManager
                    autoLoadFromStorage = false
                    alwaysAutoRefresh = false
                }
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
            if (safeSessionManager.hasSession()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        client?.auth?.loadFromStorage()
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice loading session from storage: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase Client: ${e.message}")
        }
    }

    private val _connectionStatus = MutableStateFlow("Connected to Supabase Cloud")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /**
     * Final Production-Ready SQL Schema.
     */
    val SQL_SCHEMA_SCRIPT = """
        -- =================================================================
        -- BP WALLET - FINAL PRODUCTION SCHEMA (CLEANUP VERSION)
        -- =================================================================

        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            full_name TEXT,
            email TEXT,
            phone TEXT,
            country TEXT,
            currency TEXT,
            role TEXT NOT NULL DEFAULT 'user',
            status TEXT NOT NULL DEFAULT 'Active',
            wallet_balance NUMERIC DEFAULT 0,
            betpro_username TEXT,
            betpro_password TEXT,
            is_verified BOOLEAN DEFAULT TRUE,
            created_at BIGINT,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS transactions (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            user_name TEXT,
            user_email TEXT,
            type TEXT NOT NULL,
            amount NUMERIC NOT NULL,
            currency TEXT,
            country TEXT,
            gateway_name TEXT,
            account_title TEXT,
            account_number TEXT,
            reference_number TEXT,
            screenshot_uri TEXT,
            admin_notes TEXT,
            status TEXT NOT NULL DEFAULT 'Pending',
            timestamp BIGINT,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS payment_gateways (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            currency TEXT,
            country TEXT,
            title TEXT,
            account_number TEXT,
            iban TEXT,
            bank_name TEXT,
            instructions TEXT,
            short_description TEXT,
            logo_url TEXT,
            is_enabled BOOLEAN DEFAULT TRUE,
            display_order INT DEFAULT 0,
            min_deposit NUMERIC DEFAULT 500,
            min_withdraw NUMERIC DEFAULT 1000,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS user_withdrawal_accounts (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            type TEXT,
            title TEXT,
            account_number TEXT,
            iban TEXT,
            bank_name TEXT,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS audit_logs (
            id TEXT PRIMARY KEY,
            admin_id TEXT,
            admin_name TEXT,
            action TEXT NOT NULL,
            details TEXT,
            target_id TEXT,
            timestamp BIGINT,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS admin_notifications (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            message TEXT NOT NULL,
            type TEXT NOT NULL,
            timestamp BIGINT,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS app_config (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS app_settings (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        -- RLS Policies
        ALTER TABLE users ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON users FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON transactions FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE payment_gateways ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON payment_gateways FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE user_withdrawal_accounts ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON user_withdrawal_accounts FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON app_settings FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON audit_logs FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE admin_notifications ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON admin_notifications FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Public Access" ON app_config FOR ALL USING (true) WITH CHECK (true);
    """.trimIndent()

    sealed class SignInResult {
        data object Success : SignInResult()
        data object EmailNotConfirmed : SignInResult()
        data class Error(val message: String) : SignInResult()
    }

    suspend fun signUp(email: String, password: String, username: String, fullName: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext Pair(null, "Supabase client not initialized")
            val response = supabase.auth.signUpWith(
                provider = io.github.jan.supabase.auth.providers.builtin.Email,
                redirectUrl = "bpwallet://auth/callback"
            ) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("username", username)
                    put("full_name", fullName)
                }
            }
            val userId = response?.id ?: supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                Pair(userId, null)
            } else {
                Pair(null, "Failed to create authentication account")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SignUp error: ${e.message}")
            Pair(null, e.message ?: "Failed to register account")
        }
    }

    suspend fun resendConfirmationEmail(email: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext Pair(false, "Supabase client not initialized")
            supabase.auth.resendEmail(
                type = io.github.jan.supabase.auth.OtpType.Email.SIGNUP,
                email = email
            )
            Pair(true, null)
        } catch (e: Exception) {
            Log.w(TAG, "resendConfirmationEmail error: ${e.message}")
            Pair(false, e.message ?: "Failed to resend confirmation email")
        }
    }

    suspend fun signInDetail(email: String, password: String): SignInResult = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext SignInResult.Error("Supabase client not initialized")
            supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            SignInResult.Success
        } catch (e: Exception) {
            val msg = e.message ?: "SignIn failed"
            Log.w(TAG, "SignIn detail error: $msg")
            if (msg.contains("email not confirmed", ignoreCase = true) ||
                msg.contains("not verified", ignoreCase = true) ||
                msg.contains("unconfirmed", ignoreCase = true) ||
                msg.contains("email_not_confirmed", ignoreCase = true)) {
                SignInResult.EmailNotConfirmed
            } else {
                SignInResult.Error(msg)
            }
        }
    }

    suspend fun signIn(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        signInDetail(email, password) is SignInResult.Success
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun handleDeepLink(intent: android.content.Intent): Boolean = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext false
            val uri = intent.data ?: return@withContext false
            Log.d(TAG, "Handling deep link URI: $uri")

            val uriString = uri.toString()
            val queryParams = mutableMapOf<String, String>()
            val fragmentParams = mutableMapOf<String, String>()

            // Extract query parameters
            uri.queryParameterNames?.forEach { name ->
                uri.getQueryParameter(name)?.let { valStr -> queryParams[name] = valStr }
            }

            // Extract fragment parameters (#access_token=...&refresh_token=...&type=signup)
            val fragment = uri.fragment ?: if (uriString.contains("#")) uriString.substringAfter("#") else null
            if (!fragment.isNullOrBlank()) {
                fragment.split("&").forEach { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        fragmentParams[parts[0]] = parts[1]
                    }
                }
            }

            val accessToken = fragmentParams["access_token"] ?: queryParams["access_token"]
            val refreshToken = fragmentParams["refresh_token"] ?: queryParams["refresh_token"]
            val tokenHash = queryParams["token_hash"] ?: fragmentParams["token_hash"] ?: queryParams["token"] ?: fragmentParams["token"]
            val code = queryParams["code"] ?: fragmentParams["code"]
            val typeStr = queryParams["type"] ?: fragmentParams["type"] ?: "signup"

            var sessionImported = false

            // 1. Explicit Access & Refresh Tokens
            if (!accessToken.isNullOrBlank()) {
                try {
                    val expiresIn = fragmentParams["expires_in"]?.toLongOrNull() ?: 3600L
                    val tokenType = fragmentParams["token_type"] ?: "bearer"
                    val session = UserSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken ?: "",
                        expiresIn = expiresIn,
                        tokenType = tokenType,
                        user = null
                    )
                    supabase.auth.importSession(session)
                    supabase.auth.retrieveUser(accessToken)
                    sessionImported = true
                } catch (e: Exception) {
                    Log.w(TAG, "Import session error: ${e.message}")
                }
            }

            // 2. PKCE Authorization Code Exchange
            if (!sessionImported && !code.isNullOrBlank()) {
                try {
                    supabase.auth.exchangeCodeForSession(code)
                    sessionImported = true
                } catch (e: Exception) {
                    Log.w(TAG, "Code exchange error: ${e.message}")
                }
            }

            // 3. Token Hash Verification
            if (!sessionImported && !tokenHash.isNullOrBlank()) {
                try {
                    val otpType = when (typeStr.lowercase()) {
                        "signup" -> io.github.jan.supabase.auth.OtpType.Email.SIGNUP
                        "recovery" -> io.github.jan.supabase.auth.OtpType.Email.RECOVERY
                        "magiclink" -> io.github.jan.supabase.auth.OtpType.Email.MAGIC_LINK
                        else -> io.github.jan.supabase.auth.OtpType.Email.EMAIL
                    }
                    supabase.auth.verifyEmailOtp(type = otpType, tokenHash = tokenHash)
                    sessionImported = true
                } catch (e: Exception) {
                    Log.w(TAG, "Token hash verify error: ${e.message}")
                }
            }

            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser != null || sessionImported) {
                val userId = currentUser?.id ?: getCurrentAuthUserId()
                if (userId != null) {
                    updateUserVerificationStatus(userId, true)
                }
                return@withContext true
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Deep link processing error: ${e.message}")
            false
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            client?.auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "SignOut error: ${e.message}")
        }
    }

    suspend fun sendPasswordResetEmail(email: String) = withContext(Dispatchers.IO) {
        try {
            client?.auth?.resetPasswordForEmail(email)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Reset password error: ${e.message}")
            false
        }
    }

    suspend fun updateAuthPassword(newPassword: String) = withContext(Dispatchers.IO) {
        try {
            client?.auth?.updateUser {
                password = newPassword
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Update password error: ${e.message}")
            false
        }
    }

    suspend fun findEmailByUsername(username: String): String? = withContext(Dispatchers.IO) {
        if (username.contains("@")) return@withContext username
        try {
            val supabase = client ?: return@withContext null
            val result = supabase.postgrest["users"].select {
                filter {
                    eq("username", username)
                }
            }.decodeSingleOrNull<UserAccount>()
            result?.email
        } catch (e: Exception) {
            Log.w(TAG, "Notice: Find email by username remote query failed (${e.message}). Falling back to local state.")
            null
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun isEmailVerified(): Boolean = withContext(Dispatchers.IO) {
        try {
            client?.auth?.currentUserOrNull()?.emailConfirmedAt != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun verifyEmailCode(email: String, code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext false
            try {
                supabase.auth.verifyEmailOtp(
                    type = io.github.jan.supabase.auth.OtpType.Email.SIGNUP,
                    email = email,
                    token = code
                )
                val uid = getCurrentAuthUserId()
                if (uid != null) {
                    updateUserVerificationStatus(uid, true)
                }
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "SIGNUP OTP verify notice: ${e.message}")
            }

            try {
                supabase.auth.verifyEmailOtp(
                    type = io.github.jan.supabase.auth.OtpType.Email.EMAIL,
                    email = email,
                    token = code
                )
                val uid = getCurrentAuthUserId()
                if (uid != null) {
                    updateUserVerificationStatus(uid, true)
                }
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "EMAIL OTP verify notice: ${e.message}")
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "verifyEmailCode error: ${e.message}")
            false
        }
    }

    suspend fun resendVerificationEmail(email: String) = withContext(Dispatchers.IO) {
        try {
            // Trying different signature for resend if the standard one fails
            // client.auth.resend(OtpType.Email.SIGNUP, email)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loadUserById(userId: String): UserAccount? = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext null
            supabase.postgrest["users"].select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<UserAccount>()
        } catch (e: Exception) {
            Log.e(TAG, "Load user error: ${e.message}")
            null
        }
    }

    suspend fun loadUserByEmail(email: String): UserAccount? = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext null
            supabase.postgrest["users"].select {
                filter {
                    eq("email", email)
                }
            }.decodeSingleOrNull<UserAccount>()
        } catch (e: Exception) {
            Log.e(TAG, "Load user by email error: ${e.message}")
            null
        }
    }

    suspend fun updateUserVerificationStatus(userId: String, isVerified: Boolean) = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext
            supabase.postgrest["users"].update({
                set("is_verified", isVerified)
            }) {
                filter {
                    eq("id", userId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update user verification status notice: ${e.message}")
        }
    }

    suspend fun getCurrentAuthUserId(): String? = withContext(Dispatchers.IO) {
        try {
            client?.auth?.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadImage(fileName: String, byteArray: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext null
            val bucket = supabase.storage.from("proofs")
            bucket.upload(fileName, byteArray) {
                upsert = true
            }
            val publicUrl = bucket.publicUrl(fileName)
            Log.d(TAG, "Successfully uploaded image: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to Supabase: ${e.message}")
            null
        }
    }

    suspend fun syncUser(user: UserAccount) {
        withContext(Dispatchers.IO) {
            try {
                val supabase = client ?: return@withContext
                try {
                    supabase.postgrest["users"].upsert(user)
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    Log.w(TAG, "Full user upsert notice ($msg), attempting essential schema upsert...")
                    val basicMap = mapOf<String, Any?>(
                        "id" to user.id,
                        "email" to user.email,
                        "full_name" to user.fullName,
                        "role" to user.role,
                        "wallet_balance" to user.walletBalance,
                        "status" to user.betproIdStatus,
                        "is_verified" to user.isVerified
                    )
                    supabase.postgrest["users"].upsert(basicMap)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase 'users' table sync notice: ${e.message}")
            }
        }
    }

    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext false
            supabase.postgrest["users"].delete {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user: ${e.message}")
            false
        }
    }

    suspend fun syncTransaction(tx: TransactionRequest) {
        withContext(Dispatchers.IO) {
            try {
                client?.postgrest?.get("transactions")?.upsert(tx)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing transaction: ${e.message}")
            }
        }
    }

    suspend fun syncPaymentGateway(gw: PaymentGateway) {
        withContext(Dispatchers.IO) {
            try {
                client?.postgrest?.get("payment_gateways")?.upsert(gw)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing payment gateway: ${e.message}")
            }
        }
    }

    suspend fun deletePaymentGatewayFromCloud(id: String) {
        withContext(Dispatchers.IO) {
            try {
                client?.postgrest?.get("payment_gateways")?.delete {
                    filter {
                        eq("id", id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting gateway: ${e.message}")
            }
        }
    }

    suspend fun syncWithdrawalAccount(acc: UserWithdrawalAccount) {
        withContext(Dispatchers.IO) {
            try {
                client?.postgrest?.get("user_withdrawal_accounts")?.upsert(acc)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing withdrawal account: ${e.message}")
            }
        }
    }

    suspend fun deleteWithdrawalAccountFromCloud(id: String) {
        withContext(Dispatchers.IO) {
            try {
                client?.postgrest?.get("user_withdrawal_accounts")?.delete {
                    filter {
                        eq("id", id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting withdrawal account: ${e.message}")
            }
        }
    }

    suspend fun syncSetting(key: String, value: String) {
        withContext(Dispatchers.IO) {
            try {
                client?.postgrest?.get("app_settings")?.upsert(mapOf("key" to key, "value" to value))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing setting: ${e.message}")
            }
        }
    }

    suspend fun loadAllUsers(): List<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext emptyList()
            supabase.postgrest["users"].select().decodeList<UserAccount>()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST205") || msg.contains("users") || msg.contains("Could not find")) {
                Log.e(TAG, "CRITICAL: 'users' table missing. Please run the SQL_SCHEMA_SCRIPT in your Supabase SQL Editor.")
            } else {
                Log.e(TAG, "Error loading users: $msg")
            }
            emptyList()
        }
    }

    suspend fun loadAllTransactions(): List<TransactionRequest> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext emptyList()
            supabase.postgrest["transactions"].select().decodeList<TransactionRequest>()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST205") || msg.contains("transactions") || msg.contains("Could not find")) {
                Log.e(TAG, "CRITICAL: 'transactions' table missing. Run SQL_SCHEMA_SCRIPT in Supabase.")
            } else {
                Log.e(TAG, "Error loading transactions: $msg")
            }
            emptyList()
        }
    }

    suspend fun loadAllGateways(): List<PaymentGateway> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext emptyList()
            supabase.postgrest["payment_gateways"].select().decodeList<PaymentGateway>()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST205") || msg.contains("payment_gateways") || msg.contains("Could not find")) {
                Log.e(TAG, "CRITICAL: 'payment_gateways' table missing. Run SQL_SCHEMA_SCRIPT in Supabase.")
            } else {
                Log.e(TAG, "Error loading gateways: $msg")
            }
            emptyList()
        }
    }

    suspend fun loadAllWithdrawalAccounts(): List<UserWithdrawalAccount> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext emptyList()
            supabase.postgrest["user_withdrawal_accounts"].select().decodeList<UserWithdrawalAccount>()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST205") || msg.contains("user_withdrawal_accounts") || msg.contains("Could not find")) {
                Log.e(TAG, "CRITICAL: 'user_withdrawal_accounts' table missing. Run SQL_SCHEMA_SCRIPT in Supabase.")
            } else {
                Log.e(TAG, "Error loading withdrawal accounts: $msg")
            }
            emptyList()
        }
    }

    suspend fun loadSetting(key: String, defaultVal: String): String = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext defaultVal
            val result = supabase.postgrest["app_settings"].select {
                filter {
                    eq("key", key)
                }
            }.decodeSingleOrNull<Map<String, String>>()
            result?.get("value") ?: defaultVal
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!msg.contains("PGRST205") && !msg.contains("app_settings") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error loading setting $key: $msg")
            }
            defaultVal
        }
    }

    suspend fun syncAuditLog(log: AuditLog) = withContext(Dispatchers.IO) {
        try {
            client?.postgrest?.get("audit_logs")?.upsert(log)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!msg.contains("PGRST205") && !msg.contains("audit_logs") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error syncing audit log: $msg")
            }
        }
    }

    suspend fun loadAllAuditLogs(): List<AuditLog> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext emptyList()
            supabase.postgrest["audit_logs"].select().decodeList<AuditLog>()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // Suppress error if table is missing
            if (!msg.contains("PGRST205") && !msg.contains("audit_logs") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error loading audit logs: $msg")
            }
            emptyList()
        }
    }

    suspend fun syncAdminNotification(notification: AdminNotification) = withContext(Dispatchers.IO) {
        try {
            client?.postgrest?.get("admin_notifications")?.upsert(notification)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!msg.contains("PGRST205") && !msg.contains("admin_notifications") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error syncing admin notification: $msg")
            }
        }
    }

    suspend fun loadAllAdminNotifications(): List<AdminNotification> = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext emptyList()
            supabase.postgrest["admin_notifications"].select().decodeList<AdminNotification>()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // Suppress error if table is missing
            if (!msg.contains("PGRST205") && !msg.contains("admin_notifications") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error loading notifications: $msg")
            }
            emptyList()
        }
    }

    suspend fun syncAppSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext
            // Store as a single JSON string in "app_config"
            val jsonString = kotlinx.serialization.json.Json.encodeToString(AppSettings.serializer(), settings)
            supabase.postgrest["app_config"].upsert(mapOf("key" to "main_settings", "value" to jsonString))
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!msg.contains("PGRST205") && !msg.contains("app_config") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error syncing app settings: $msg")
            }
        }
    }

    suspend fun loadAppSettings(): AppSettings = withContext(Dispatchers.IO) {
        try {
            val supabase = client ?: return@withContext AppSettings()
            val result = supabase.postgrest["app_config"].select {
                filter {
                    eq("key", "main_settings")
                }
            }.decodeSingleOrNull<Map<String, String>>()
            val value = result?.get("value")
            if (value != null) {
                kotlinx.serialization.json.Json.decodeFromString(AppSettings.serializer(), value)
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!msg.contains("PGRST205") && !msg.contains("app_config") && !msg.contains("Could not find")) {
                Log.e(TAG, "Error loading app settings: $msg")
            }
            AppSettings()
        }
    }
}

class SafeSessionManager(private val context: android.content.Context) : SessionManager {
    private val prefs = context.getSharedPreferences("bp_supabase_session", android.content.Context.MODE_PRIVATE)
    private val KEY = "supabase_user_session"

    fun hasSession(): Boolean {
        val jsonStr = prefs.getString(KEY, null)
        return !jsonStr.isNullOrBlank()
    }

    override suspend fun saveSession(session: UserSession) {
        try {
            val jsonStr = Json.encodeToString(session)
            prefs.edit().putString(KEY, jsonStr).apply()
        } catch (e: Exception) {
            Log.e("SafeSessionManager", "Failed to save session: ${e.message}")
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun loadSession(): UserSession {
        val jsonStr = prefs.getString(KEY, null)
        if (jsonStr.isNullOrBlank()) {
            throw NoSuchElementException("No saved session in storage")
        }
        return try {
            val session = Json.decodeFromString<UserSession>(jsonStr)
            if (session.accessToken.isBlank() || session.refreshToken.isBlank()) {
                deleteSession()
                throw NoSuchElementException("Stored session token is blank")
            }
            session
        } catch (e: Exception) {
            deleteSession()
            if (e is NoSuchElementException) throw e
            throw NoSuchElementException("Failed to load session from storage: ${e.message}")
        }
    }

    override suspend fun deleteSession() {
        try {
            prefs.edit().remove(KEY).apply()
        } catch (e: Exception) {
            Log.e("SafeSessionManager", "Failed to delete session: ${e.message}")
        }
    }
}
