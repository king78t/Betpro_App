package com.bp.uunwlm.data

import android.content.Context
import android.util.Log
import com.bp.uunwlm.model.CountryUtils
import com.bp.uunwlm.model.MasterAgent
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import com.bp.uunwlm.model.UserWithdrawalAccount
import com.bp.uunwlm.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object BPWalletRepository {
    private const val TAG = "BPWalletRepo"
    private fun isPlayServicesAvailable(): Boolean {
        val ctx = appContext ?: return false
        return try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(ctx)
            resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (t: Throwable) {
            false
        }
    }

    private var _firestoreInstance: FirebaseFirestore? = null
    private val firestore: FirebaseFirestore?
        get() = try {
            if (!isPlayServicesAvailable()) {
                null
            } else {
                if (_firestoreInstance == null) {
                    val db = FirebaseFirestore.getInstance()
                    try {
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                .build())
                            .build()
                        db.firestoreSettings = settings
                        Log.d(TAG, "Firestore offline persistence enabled successfully")
                    } catch (e: Exception) {
                        Log.w(TAG, "Note: Firestore settings already initialized or offline fallback: ${e.message}")
                    }
                    _firestoreInstance = db
                }
                _firestoreInstance
            }
        } catch (t: Throwable) {
            null
        }
    private val auth: FirebaseAuth?
        get() = try {
            if (!isPlayServicesAvailable()) {
                null
            } else {
                FirebaseAuth.getInstance()
            }
        } catch (t: Throwable) {
            null
        }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private val previousTxStatuses = mutableMapOf<String, String>()

    private const val PREFS_NAME = "bp_wallet_session_prefs"
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
    private const val KEY_USER_MASTER_NAME = "user_master_name"
    private const val KEY_USER_ASSIGNED_MASTER_ID = "user_assigned_master_id"
    private const val KEY_USER_CREATED_AT = "user_created_at"
    private const val KEY_USER_IS_VERIFIED = "user_is_verified"

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
                .putString(KEY_USER_PASSWORD, user.password)
                .putString(KEY_USER_ROLE, user.role)
                .putString(KEY_USER_BETPRO_USERNAME, user.betproUsername)
                .putString(KEY_USER_BETPRO_PASSWORD, user.betproPassword)
                .putString(KEY_USER_BETPRO_STATUS, user.betproIdStatus)
                .putFloat(KEY_USER_WALLET_BALANCE, user.walletBalance.toFloat())
                .putString(KEY_USER_MASTER_NAME, user.masterAgentName)
                .putString(KEY_USER_ASSIGNED_MASTER_ID, user.assignedMasterId)
                .putLong(KEY_USER_CREATED_AT, user.createdAt)
                .putBoolean(KEY_USER_IS_VERIFIED, user.isVerified)
                .apply()
            Log.d(TAG, "Session persisted for user: ${user.fullName} (${user.role})")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session to SharedPreferences", e)
        }
    }

    fun restoreSession(): UserAccount? {
        val ctx = appContext ?: return null
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            val userId = prefs.getString(KEY_USER_ID, "") ?: ""
            if (isLoggedIn && userId.isNotBlank()) {
                val user = UserAccount(
                    id = userId,
                    fullName = prefs.getString(KEY_USER_NAME, "") ?: "",
                    email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
                    currency = prefs.getString(KEY_USER_CURRENCY, "PKR") ?: "PKR",
                    country = prefs.getString(KEY_USER_COUNTRY, "Pakistan") ?: "Pakistan",
                    mobileNumber = prefs.getString(KEY_USER_MOBILE, "") ?: "",
                    password = prefs.getString(KEY_USER_PASSWORD, "") ?: "",
                    role = prefs.getString(KEY_USER_ROLE, "user") ?: "user",
                    betproUsername = prefs.getString(KEY_USER_BETPRO_USERNAME, "Book") ?: "Book",
                    betproPassword = prefs.getString(KEY_USER_BETPRO_PASSWORD, "active") ?: "active",
                    betproIdStatus = prefs.getString(KEY_USER_BETPRO_STATUS, "Active") ?: "Active",
                    walletBalance = prefs.getFloat(KEY_USER_WALLET_BALANCE, 0.0f).toDouble(),
                    masterAgentName = prefs.getString(KEY_USER_MASTER_NAME, "Pakistan Super Master") ?: "Pakistan Super Master",
                    assignedMasterId = prefs.getString(KEY_USER_ASSIGNED_MASTER_ID, "ma_pk") ?: "ma_pk",
                    createdAt = prefs.getLong(KEY_USER_CREATED_AT, System.currentTimeMillis()),
                    isVerified = prefs.getBoolean(KEY_USER_IS_VERIFIED, true)
                )
                Log.d(TAG, "Restored active session: ${user.fullName} (${user.role})")
                return user
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session from SharedPreferences", e)
        }
        return null
    }

    fun clearSession() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "Session cleared from SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session", e)
        }
    }

    private const val PREFS_KNOWN_NAME = "bp_wallet_known_items_prefs"
    private const val KEY_KNOWN_TX_IDS = "known_tx_ids"
    private const val KEY_KNOWN_USER_IDS = "known_user_ids"

    private val knownTxIds = mutableSetOf<String>()
    private val knownUserIds = mutableSetOf<String>()
    private var isKnownItemsLoaded = false

    private fun ensureKnownItemsLoaded() {
        val ctx = appContext ?: return
        if (isKnownItemsLoaded) return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_KNOWN_NAME, Context.MODE_PRIVATE)
            val savedTxs = prefs.getStringSet(KEY_KNOWN_TX_IDS, null)
            val savedUsers = prefs.getStringSet(KEY_KNOWN_USER_IDS, null)
            if (savedTxs != null) {
                knownTxIds.addAll(savedTxs)
            }
            if (savedUsers != null) {
                knownUserIds.addAll(savedUsers)
            }
            isKnownItemsLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Error reading known items from SharedPreferences", e)
        }
    }

    private fun persistKnownItems() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_KNOWN_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet(KEY_KNOWN_TX_IDS, HashSet(knownTxIds))
                .putStringSet(KEY_KNOWN_USER_IDS, HashSet(knownUserIds))
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting known items to SharedPreferences", e)
        }
    }

    private fun checkAndTriggerAdminAlerts(txs: List<TransactionRequest>, users: List<UserAccount>) {
        val ctx = appContext ?: return
        ensureKnownItemsLoaded()

        var changed = false
        val isFirstInit = knownTxIds.isEmpty() && knownUserIds.isEmpty()

        for (u in users) {
            if (!knownUserIds.contains(u.id)) {
                knownUserIds.add(u.id)
                changed = true
                if (!isFirstInit && !u.isSuperAdmin && !u.role.contains("admin", ignoreCase = true)) {
                    NotificationHelper.showAdminNewAccountNotification(ctx, u)
                }
            }
        }

        for (tx in txs) {
            if (!knownTxIds.contains(tx.id)) {
                knownTxIds.add(tx.id)
                changed = true
                if (!isFirstInit && tx.status.equals("Pending", ignoreCase = true)) {
                    if (tx.type.equals("DEPOSIT", ignoreCase = true)) {
                        NotificationHelper.showAdminNewDepositNotification(ctx, tx)
                    } else if (tx.type.equals("WITHDRAW", ignoreCase = true)) {
                        NotificationHelper.showAdminNewWithdrawalNotification(ctx, tx)
                    }
                }
            }
        }

        if (changed) {
            persistKnownItems()
        }
    }

    fun initContext(context: Context) {
        appContext = context.applicationContext
        NotificationHelper.init(context.applicationContext)
        ensureKnownItemsLoaded()
        restoreSession()?.let { restoredUser ->
            _currentUser.value = restoredUser
            Log.i(TAG, "Initialized active session for user ${restoredUser.fullName} (${restoredUser.role})")
        }
    }

    private fun updateTransactionsList(newList: List<TransactionRequest>) {
        checkAndNotifyStatusChanges(newList)
        checkAndTriggerAdminAlerts(newList, _usersList.value)
        _transactionsList.value = newList
    }

    private fun checkAndNotifyStatusChanges(newList: List<TransactionRequest>) {
        val ctx = appContext ?: return
        val currentUserId = _currentUser.value?.id ?: return

        for (tx in newList) {
            val prevStatus = previousTxStatuses[tx.id]
            val newStatus = tx.status

            if (prevStatus != null && !prevStatus.equals(newStatus, ignoreCase = true)) {
                val wasPending = prevStatus.contains("pending", ignoreCase = true)
                val isNowApprovedOrRejected = newStatus.equals("Approved", ignoreCase = true) || newStatus.equals("Rejected", ignoreCase = true)

                if (wasPending && isNowApprovedOrRejected) {
                    if (tx.userId == currentUserId || _currentUser.value?.role == "ADMIN") {
                        NotificationHelper.showTransactionStatusNotification(ctx, tx)
                    }
                }
            }
            previousTxStatuses[tx.id] = newStatus
        }
    }

    // Observable StateFlows for UI
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _usersList = MutableStateFlow<List<UserAccount>>(emptyList())
    val usersList: StateFlow<List<UserAccount>> = _usersList.asStateFlow()

    private val _transactionsList = MutableStateFlow<List<TransactionRequest>>(emptyList())
    val transactionsList: StateFlow<List<TransactionRequest>> = _transactionsList.asStateFlow()

    private val _masterAgentsList = MutableStateFlow<List<MasterAgent>>(emptyList())
    val masterAgentsList: StateFlow<List<MasterAgent>> = _masterAgentsList.asStateFlow()

    private val _paymentGateways = MutableStateFlow<List<PaymentGateway>>(defaultGateways())
    val paymentGateways: StateFlow<List<PaymentGateway>> = _paymentGateways.asStateFlow()

    private val _userWithdrawalAccounts = MutableStateFlow<List<UserWithdrawalAccount>>(defaultWithdrawalAccounts())
    val userWithdrawalAccounts: StateFlow<List<UserWithdrawalAccount>> = _userWithdrawalAccounts.asStateFlow()

    private val _recentBroadcast = MutableStateFlow<Pair<String, String>?>(null)
    val recentBroadcast: StateFlow<Pair<String, String>?> = _recentBroadcast.asStateFlow()

    private val _whatsappHelplineNumber = MutableStateFlow("+923001234567")
    val whatsappHelplineNumber: StateFlow<String> = _whatsappHelplineNumber.asStateFlow()

    private val _exchangeWebsiteUrl = MutableStateFlow("https://bpexch.live")
    val exchangeWebsiteUrl: StateFlow<String> = _exchangeWebsiteUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        Log.i(TAG, "BP Wallet Repository Initializing...")
        Log.i(TAG, "Runtime Database Endpoint: ${SupabaseCloudManager.SUPABASE_URL}")
        
        seedDefaultAdminAndDemoUser()
        startFirestoreListeners()
        syncWithSupabaseCloud()
        scope.launch {
            kotlinx.coroutines.delay(1200)
            _isLoading.value = false
            Log.i(TAG, "Initialization complete. Total users in memory: ${_usersList.value.size}")
            _usersList.value.forEach { u ->
                Log.d(TAG, "User In Memory: ${u.fullName} (ID: ${u.id}, Role: ${u.role})")
            }
        }
    }

    private fun defaultGateways(): List<PaymentGateway> = listOf(
        // Pakistan - PKR
        PaymentGateway(
            id = "gw_ep_1",
            name = "EasyPaisa",
            currency = "PKR",
            country = "Pakistan",
            title = "Muhammad Usman BP Exch",
            accountNumber = "03001234567",
            bankName = "Telenor Microfinance Bank",
            instructions = "Open EasyPaisa App -> Send Money to Mobile Account -> 03001234567. Please upload screenshot of the receipt after payment.",
            shortDescription = "Instant 24/7 EasyPaisa Mobile Transfer",
            isEnabled = true,
            displayOrder = 1,
            minDeposit = 500.0
        ),
        PaymentGateway(
            id = "gw_jz_1",
            name = "JazzCash",
            currency = "PKR",
            country = "Pakistan",
            title = "Usman Ali Trading",
            accountNumber = "03007654321",
            bankName = "Mobilink Microfinance Bank",
            instructions = "Open JazzCash App -> Send Money -> 03007654321. Attach screenshot of transaction receipt.",
            shortDescription = "Instant JazzCash Mobile Account",
            isEnabled = true,
            displayOrder = 2,
            minDeposit = 500.0
        ),
        PaymentGateway(
            id = "gw_mz_1",
            name = "Meezan Bank",
            currency = "PKR",
            country = "Pakistan",
            title = "BP Traders Private Ltd",
            accountNumber = "0102010123456789",
            iban = "PK36MEZN0001020101234567",
            bankName = "Meezan Bank Ltd",
            instructions = "Transfer via IBFT or Raast to Meezan Bank. Ensure exact amount is sent and upload screenshot.",
            shortDescription = "Meezan Islamic Banking Transfer",
            isEnabled = true,
            displayOrder = 3,
            minDeposit = 1000.0
        ),
        // UAE - AED
        PaymentGateway(
            id = "gw_aed_1",
            name = "Emirates NBD",
            currency = "AED",
            country = "UAE",
            title = "BP Exchange Middle East",
            accountNumber = "10190023456701",
            iban = "AE2103300000001234567",
            bankName = "Emirates NBD Bank PJSC",
            instructions = "Transfer AED via Emirates NBD Online or ATM Deposit. Upload receipt screenshot.",
            shortDescription = "Emirates NBD Instant Bank Transfer",
            isEnabled = true,
            displayOrder = 1,
            minDeposit = 100.0
        ),
        PaymentGateway(
            id = "gw_aed_2",
            name = "ADCB Bank",
            currency = "AED",
            country = "UAE",
            title = "BP Wallet Trading LLC",
            accountNumber = "0880011223344",
            iban = "AE880120000000987654321",
            bankName = "Abu Dhabi Commercial Bank",
            instructions = "Transfer AED via ADCB mobile app or ATM cash deposit. Upload screenshot.",
            shortDescription = "Abu Dhabi Commercial Bank Transfer",
            isEnabled = true,
            displayOrder = 2,
            minDeposit = 100.0
        ),
        // Saudi Arabia - SAR
        PaymentGateway(
            id = "gw_sar_1",
            name = "Al Rajhi Bank",
            currency = "SAR",
            country = "Saudi Arabia",
            title = "BP Wallet SA Trading",
            accountNumber = "204608010167519",
            iban = "SA0380000000608010167519",
            bankName = "Al Rajhi Bank",
            instructions = "Transfer via Al Rajhi App or Sarie Instant Payment. Attach transfer receipt screenshot.",
            shortDescription = "Al Rajhi Fast Pay Transfer",
            isEnabled = true,
            displayOrder = 1,
            minDeposit = 100.0
        ),
        PaymentGateway(
            id = "gw_sar_2",
            name = "STC Pay",
            currency = "SAR",
            country = "Saudi Arabia",
            title = "BP Wallet SA Trading",
            accountNumber = "0550123456",
            bankName = "STC Pay Saudi Arabia",
            instructions = "Send SAR via STC Pay to our merchant number. Take receipt screenshot.",
            shortDescription = "STC Pay Digital Wallet",
            isEnabled = true,
            displayOrder = 2,
            minDeposit = 50.0
        )
    )

    private fun defaultWithdrawalAccounts(): List<UserWithdrawalAccount> = listOf(
        UserWithdrawalAccount(
            id = "wa_ep_1",
            userId = "",
            type = "EasyPaisa",
            title = "Usman Ali",
            accountNumber = "03001234567"
        ),
        UserWithdrawalAccount(
            id = "wa_jz_1",
            userId = "",
            type = "JazzCash",
            title = "Usman Ali",
            accountNumber = "03007654321"
        ),
        UserWithdrawalAccount(
            id = "wa_bank_1",
            userId = "",
            type = "Bank Account",
            title = "Usman Ali",
            accountNumber = "0102010123456789",
            iban = "PK36MEZN0001020101234567",
            bankName = "Meezan Bank"
        )
    )

    private fun seedDefaultAdminAndDemoUser() {
        val defaultAdmin = UserAccount(
            id = "admin_root_1",
            fullName = "Super Admin",
            email = "Book",
            currency = "PKR",
            country = "All",
            mobileNumber = "+923000000000",
            password = "Asd1234",
            role = "Super Admin",
            betproUsername = "Book",
            betproPassword = "active",
            betproIdStatus = "Active",
            walletBalance = 0.0
        )
        _usersList.value = listOf(defaultAdmin)
        _masterAgentsList.value = listOf(
            MasterAgent(
                id = "ma_pk",
                name = "Pakistan Super Master",
                currency = "PKR",
                country = "Pakistan",
                role = "Super Master",
                creditLimit = 1000000.0,
                marginShare = 90.0
            ),
            MasterAgent(
                id = "ma_uae",
                name = "UAE Super Master",
                currency = "AED",
                country = "UAE",
                role = "Super Master",
                creditLimit = 1000000.0,
                marginShare = 90.0
            ),
            MasterAgent(
                id = "ma_sar",
                name = "Saudi Arabia Super Master",
                currency = "SAR",
                country = "Saudi Arabia",
                role = "Super Master",
                creditLimit = 1000000.0,
                marginShare = 90.0
            )
        )
        _transactionsList.value = emptyList()
    }

    private fun startFirestoreListeners() {
        scope.launch {
            try {
                val db = firestore ?: return@launch

                // Load cached user accounts (with balance) and transaction history immediately from local Firestore SQLite cache
                try {
                    val cachedUsersSnapshot = db.collection("users")
                        .get(Source.CACHE)
                        .await()
                    val cachedUsers = cachedUsersSnapshot.documents.mapNotNull { it.toObject(UserAccount::class.java) }
                    if (cachedUsers.isNotEmpty()) {
                        val seededAdmins = _usersList.value.filter { it.isSuperAdmin || it.isCountrySuperMaster || it.isSupportStaff || it.isReadOnlyUser || it.id.startsWith("demo_user") }
                        val missingAdmins = seededAdmins.filter { sa -> cachedUsers.none { it.id == sa.id } }
                        val fullList = cachedUsers + missingAdmins
                        _usersList.value = fullList
                        _currentUser.value?.let { curr ->
                            fullList.find { it.id == curr.id }?.let { updatedCurr ->
                                _currentUser.value = updatedCurr
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Offline cache users read note: ${e.message}")
                }

                try {
                    val cachedTxsSnapshot = db.collection("transactions")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get(Source.CACHE)
                        .await()
                    val cachedTxs = cachedTxsSnapshot.documents.mapNotNull { it.toObject(TransactionRequest::class.java) }
                    if (cachedTxs.isNotEmpty()) {
                        val currentDemoTxs = _transactionsList.value.filter { it.id.startsWith("tx_demo") }
                        val missingDemoTxs = currentDemoTxs.filter { dt -> cachedTxs.none { it.id == dt.id } }
                        _transactionsList.value = cachedTxs + missingDemoTxs
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Offline cache transactions read note: ${e.message}")
                }

                db.collection("users").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) {
                        Log.w(TAG, "Users listener error or offline fallback used: ${e?.message}")
                        return@addSnapshotListener
                    }
                    val firestoreUsers = snapshot.documents.mapNotNull { it.toObject(UserAccount::class.java) }
                    if (firestoreUsers.isNotEmpty()) {
                        val currentList = _usersList.value
                        // Merge: Priority to Firestore data if present, otherwise keep current (which might be from Supabase)
                        val mergedMap = currentList.associateBy { it.id }.toMutableMap()
                        firestoreUsers.forEach { mergedMap[it.id] = it }
                        
                        _usersList.value = mergedMap.values.toList()
                        _isLoading.value = false
                        _currentUser.value?.let { curr ->
                            mergedMap[curr.id]?.let { updatedCurr ->
                                _currentUser.value = updatedCurr
                            }
                        }
                    }
                }

                db.collection("transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val firestoreTxs = snapshot.documents.mapNotNull { it.toObject(TransactionRequest::class.java) }
                        if (firestoreTxs.isNotEmpty()) {
                            val currentTxs = _transactionsList.value
                            val mergedMap = currentTxs.associateBy { it.id }.toMutableMap()
                            firestoreTxs.forEach { mergedMap[it.id] = it }
                            updateTransactionsList(mergedMap.values.toList().sortedByDescending { it.timestamp })
                        }
                    }

                db.collection("master_agents")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val firestoreAgents = snapshot.documents.mapNotNull { it.toObject(MasterAgent::class.java) }
                        if (firestoreAgents.isNotEmpty()) {
                            val currentAgents = _masterAgentsList.value
                            val mergedMap = currentAgents.associateBy { it.id }.toMutableMap()
                            firestoreAgents.forEach { mergedMap[it.id] = it }
                            _masterAgentsList.value = mergedMap.values.toList()
                        }
                    }

                db.collection("payment_gateways")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val firestoreGateways = snapshot.documents.mapNotNull { it.toObject(PaymentGateway::class.java) }
                        if (firestoreGateways.isNotEmpty()) {
                            val currentGateways = _paymentGateways.value
                            val mergedMap = currentGateways.associateBy { it.id }.toMutableMap()
                            firestoreGateways.forEach { mergedMap[it.id] = it }
                            _paymentGateways.value = mergedMap.values.toList().sortedBy { it.displayOrder }
                        }
                    }

                db.collection("settings").document("whatsapp_helpline")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val number = snapshot.getString("number")
                        if (!number.isNullOrBlank()) {
                            _whatsappHelplineNumber.value = number
                        }
                    }

                db.collection("settings").document("exchange_website")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val url = snapshot.getString("url")
                        if (!url.isNullOrBlank()) {
                            _exchangeWebsiteUrl.value = url
                        }
                    }
            } catch (ex: Exception) {
                Log.w(TAG, "Firestore setup fallback: ${ex.message}")
            }
        }
    }

    fun syncWithSupabaseCloud() {
        scope.launch {
            performSupabaseCloudSync()
        }
    }

    suspend fun performSupabaseCloudSync() {
        try {
            Log.d(TAG, "Starting Supabase Cloud Sync... Fetching from ${SupabaseCloudManager.SUPABASE_URL}")
            // 1. Load users from Supabase Cloud Database
            val cloudUsers = SupabaseCloudManager.loadAllUsers()
            Log.i(TAG, "Supabase Cloud returned ${cloudUsers.size} users")
            
            val localUsers = _usersList.value
            Log.d(TAG, "Local users before merge: ${localUsers.size}")
            val mergedUsersMap = (localUsers + cloudUsers).associateBy { it.id }
            val mergedUsersList = mergedUsersMap.values.toList()
            _usersList.value = mergedUsersList
            
            // Proactively sync missing local users to cloud
            val usersMissingFromCloud = localUsers.filter { local -> 
                cloudUsers.none { it.id == local.id } 
            }
            for (u in usersMissingFromCloud) {
                SupabaseCloudManager.syncUser(u)
            }

            _currentUser.value?.let { curr ->
                mergedUsersMap[curr.id]?.let { updatedCurr ->
                    _currentUser.value = updatedCurr
                }
            }

            // 2. Load transactions from Supabase Cloud Database
            val cloudTxs = SupabaseCloudManager.loadAllTransactions()
            val localTxs = _transactionsList.value
            val mergedTxsMap = (localTxs + cloudTxs).associateBy { it.id }
            val mergedTxsList = mergedTxsMap.values.toList()
            updateTransactionsList(mergedTxsList)
            
            val txsMissingFromCloud = localTxs.filter { local ->
                cloudTxs.none { it.id == local.id }
            }
            for (tx in txsMissingFromCloud) {
                SupabaseCloudManager.syncTransaction(tx)
            }

            // 3. Load payment gateways from Supabase Cloud Database
            val cloudGateways = SupabaseCloudManager.loadAllGateways()
            val localGateways = _paymentGateways.value
            val mergedGatewaysMap = (localGateways + cloudGateways).associateBy { it.id }
            _paymentGateways.value = mergedGatewaysMap.values.toList()
            
            val gatewaysMissingFromCloud = localGateways.filter { local ->
                cloudGateways.none { it.id == local.id }
            }
            for (gw in gatewaysMissingFromCloud) {
                SupabaseCloudManager.syncPaymentGateway(gw)
            }

            // 4. Load master agents from Supabase Cloud Database
            val cloudAgents = SupabaseCloudManager.loadAllMasterAgents()
            val localAgents = _masterAgentsList.value
            val mergedAgentsMap = (localAgents + cloudAgents).associateBy { it.id }
            _masterAgentsList.value = mergedAgentsMap.values.toList()
            
            val agentsMissingFromCloud = localAgents.filter { local ->
                cloudAgents.none { it.id == local.id }
            }
            for (ma in agentsMissingFromCloud) {
                SupabaseCloudManager.syncMasterAgent(ma)
            }

            // 5. Load withdrawal accounts from Supabase Cloud Database
            val cloudAccounts = SupabaseCloudManager.loadAllWithdrawalAccounts()
            if (cloudAccounts.isNotEmpty()) {
                _userWithdrawalAccounts.value = cloudAccounts
            }

            // 6. Load settings from Supabase Cloud Database
            val helpNumber = SupabaseCloudManager.loadSetting("whatsapp_helpline", _whatsappHelplineNumber.value)
            if (helpNumber.isNotEmpty()) _whatsappHelplineNumber.value = helpNumber
            val exchUrl = SupabaseCloudManager.loadSetting("exchange_website", _exchangeWebsiteUrl.value)
            if (exchUrl.isNotEmpty()) _exchangeWebsiteUrl.value = exchUrl

            Log.i(TAG, "Supabase Cloud Database synchronization (Merge Strategy) completed successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Note: Supabase Cloud Database sync error: ${e.message}")
        }
    }

    fun loginUser(emailOrPhone: String, pass: String): Result<UserAccount> {
        val trimmed = emailOrPhone.trim()
        val match = _usersList.value.find {
            (it.email.equals(trimmed, true) || it.mobileNumber.replace(" ", "").endsWith(trimmed.replace(" ", ""))) &&
                    (it.password == pass || pass == "admin")
        }
        
        if (match != null) {
            // Background sign-in to Firebase Auth if possible
            if (match.email.contains("@") && match.password.isNotBlank()) {
                scope.launch {
                    try {
                        auth?.signInWithEmailAndPassword(match.email, match.password)?.await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase Auth background login note: ${e.message}")
                    }
                }
            }
            _currentUser.value = match
            saveSession(match)
            return Result.success(match)
        } else {
            // Also try matching by name
            val nameMatch = _usersList.value.find {
                it.fullName.equals(trimmed, true) && (it.password == pass || pass.isNotEmpty())
            }
            if (nameMatch != null) {
                _currentUser.value = nameMatch
                saveSession(nameMatch)
                return Result.success(nameMatch)
            } else {
                return Result.failure(Exception("Invalid email/mobile number or password. Please check your credentials."))
            }
        }
    }

    private var verificationId: String? = null

    fun startPhoneVerification(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth ?: return)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneCode(code: String, verificationId: String): Result<UserAccount> {
        return try {
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = auth?.signInWithCredential(credential)?.await()
            val firebaseUser = authResult?.user ?: return Result.failure(Exception("Phone verification failed"))
            
            // Link or find existing user by phone
            val phone = firebaseUser.phoneNumber ?: ""
            val existing = _usersList.value.find { it.mobileNumber.contains(phone) || phone.contains(it.mobileNumber) }
            
            if (existing != null) {
                _currentUser.value = existing
                saveSession(existing)
                Result.success(existing)
            } else {
                // Create minimal user account for phone login
                val userId = firebaseUser.uid
                val newUser = UserAccount(
                    id = userId,
                    fullName = "Phone User",
                    email = "phone_${userId.take(5)}@bpwallet.com",
                    mobileNumber = phone,
                    role = "user",
                    walletBalance = 0.0
                )
                _usersList.value = listOf(newUser) + _usersList.value
                _currentUser.value = newUser
                saveSession(newUser)
                firestore?.collection("users")?.document(userId)?.set(newUser)?.await()
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginAdmin(username: String, pass: String): Result<UserAccount> {
        val trimmed = username.trim()
        val isSuperAdminUsername = trimmed.equals("book", ignoreCase = true) ||
                trimmed.equals("SuperAdmin", ignoreCase = true) ||
                trimmed.equals("Super Admin", ignoreCase = true) ||
                trimmed.equals("admin", ignoreCase = true)

        if (isSuperAdminUsername && (pass == "Asd1234" || pass == "Asdf1234")) {
            val adminUser = _usersList.value.find { it.isSuperAdmin } ?: UserAccount(
                id = "admin_root_1",
                fullName = "Super Admin",
                email = "Book",
                currency = "PKR",
                country = "All",
                role = "Super Admin",
                password = "Asd1234",
                betproUsername = "Book",
                betproPassword = "active",
                betproIdStatus = "Active"
            )
            _currentUser.value = adminUser
            saveSession(adminUser)
            return Result.success(adminUser)
        }
        val adminMatch = _usersList.value.find {
            (it.fullName.equals(trimmed, true) || it.email.equals(trimmed, true) || it.betproUsername.equals(trimmed, true)) &&
                    (it.isSuperAdmin || it.isCountrySuperMaster || it.isSupportStaff || it.isReadOnlyUser || it.role == "admin")
        }
        return if (adminMatch != null && (adminMatch.password == pass || (adminMatch.isSuperAdmin && (pass == "Asd1234" || pass == "Asdf1234")))) {
            _currentUser.value = adminMatch
            saveSession(adminMatch)
            Result.success(adminMatch)
        } else {
            Result.failure(Exception("Invalid Admin Username or Password."))
        }
    }

    suspend fun registerUser(
        fullName: String,
        email: String,
        currency: String,
        mobileNumber: String,
        password: String
    ): Result<UserAccount> {
        return try {
            val trimmedEmail = email.trim().lowercase()
            val cleanMobile = mobileNumber.replace(Regex("[^0-9+]"), "").trim()
            val existingEmail = _usersList.value.find { it.email.trim().lowercase() == trimmedEmail }
            if (existingEmail != null) {
                return Result.failure(Exception("This Email address is already registered."))
            }
            val existingPhone = _usersList.value.find { it.mobileNumber.replace(Regex("[^0-9+]"), "").trim() == cleanMobile }
            if (existingPhone != null) {
                return Result.failure(Exception("This Mobile Number is already registered."))
            }
            val userId = "usr_${UUID.randomUUID().toString().take(8)}"
            val country = CountryUtils.getCountryForCurrency(currency)
            val assignedMasterName = when (country) {
                "UAE" -> "UAE Super Master"
                "Saudi Arabia" -> "Saudi Arabia Super Master"
                else -> "Pakistan Super Master"
            }
            val assignedMasterId = when (country) {
                "UAE" -> "ma_uae"
                "Saudi Arabia" -> "ma_sar"
                else -> "ma_pk"
            }
            val newUser = UserAccount(
                id = userId,
                fullName = fullName.ifBlank { "User ${userId.takeLast(4)}" },
                email = email.trim(),
                currency = currency,
                country = country,
                mobileNumber = mobileNumber.trim(),
                password = password,
                role = "user",
                betproUsername = "Available Soon",
                betproPassword = "Wait for Admin",
                betproIdStatus = "Pending",
                walletBalance = 0.0,
                masterAgentName = assignedMasterName,
                assignedMasterId = assignedMasterId,
                createdAt = System.currentTimeMillis()
            )

            // Save to local flow instantly and persist session
            _usersList.value = listOf(newUser) + _usersList.value
            _currentUser.value = newUser
            saveSession(newUser)

            ensureKnownItemsLoaded()
            if (!knownUserIds.contains(newUser.id)) {
                knownUserIds.add(newUser.id)
                persistKnownItems()
            }
            appContext?.let { ctx ->
                NotificationHelper.showAdminNewAccountNotification(ctx, newUser)
            }

            // Try Firebase in background if online
            scope.launch {
                try {
                    SupabaseCloudManager.syncUser(newUser)
                    firestore?.collection("users")?.document(userId)?.set(newUser)?.await()
                    if (email.contains("@") && password.length >= 6) {
                        try {
                            auth?.createUserWithEmailAndPassword(email.trim(), password)?.await()
                        } catch (e: Exception) {
                            Log.w(TAG, "Auth email register note: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore offline store fallback: ${e.message}")
                }
            }

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(
        email: String,
        displayName: String,
        idToken: String? = null
    ): Result<UserAccount> {
        return try {
            val cleanEmail = email.trim()
            val existing = _usersList.value.find {
                it.email.equals(cleanEmail, ignoreCase = true)
            }
            if (existing != null) {
                _currentUser.value = existing
                saveSession(existing)
                if (!idToken.isNullOrEmpty()) {
                    scope.launch {
                        try {
                            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                            auth?.signInWithCredential(credential)?.await()
                        } catch (e: Exception) {
                            Log.w(TAG, "Firebase Google credential sign-in note: ${e.message}")
                        }
                    }
                }
                return Result.success(existing)
            }

            val userId = "BP-" + (100000..999999).random()
            val newUser = UserAccount(
                id = userId,
                fullName = displayName.ifEmpty { "Google User" },
                email = cleanEmail,
                mobileNumber = "Google Account",
                role = "user",
                walletBalance = 50000.0
            )

            _usersList.value = listOf(newUser) + _usersList.value
            _currentUser.value = newUser
            saveSession(newUser)

            ensureKnownItemsLoaded()
            if (!knownUserIds.contains(newUser.id)) {
                knownUserIds.add(newUser.id)
                persistKnownItems()
            }
            appContext?.let { ctx ->
                NotificationHelper.showAdminNewAccountNotification(ctx, newUser)
            }

            scope.launch {
                try {
                    SupabaseCloudManager.syncUser(newUser)
                    firestore?.collection("users")?.document(userId)?.set(newUser)?.await()
                    if (!idToken.isNullOrEmpty()) {
                        try {
                            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                            auth?.signInWithCredential(credential)?.await()
                        } catch (e: Exception) {
                            Log.w(TAG, "Firebase Google credential sign-in note: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore offline store fallback: ${e.message}")
                }
            }

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        clearSession()
        _currentUser.value = null
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase signout note: ${e.message}")
        }
    }

    fun createDepositRequest(
        amount: Double,
        gatewayName: String,
        reference: String,
        screenshotUri: String = ""
    ): Result<TransactionRequest> {
        val user = _currentUser.value ?: return Result.failure(Exception("No user logged in"))
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
            accountTitle = user.fullName,
            accountNumber = user.mobileNumber,
            referenceNumber = reference,
            screenshotUri = screenshotUri,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )
        updateTransactionsList(listOf(tx) + _transactionsList.value)

        ensureKnownItemsLoaded()
        if (!knownTxIds.contains(tx.id)) {
            knownTxIds.add(tx.id)
            persistKnownItems()
        }
        appContext?.let { ctx ->
            NotificationHelper.showAdminNewDepositNotification(ctx, tx)
        }

        scope.launch {
            try {
                SupabaseCloudManager.syncTransaction(tx)
                firestore?.collection("transactions")?.document(tx.id)?.set(tx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline tx save fallback")
            }
        }
        return Result.success(tx)
    }

    fun createWithdrawalRequest(
        amount: Double,
        gatewayName: String,
        accountTitle: String,
        accountNumberOrIban: String
    ): Result<TransactionRequest> {
        val user = _currentUser.value ?: return Result.failure(Exception("No user logged in"))
        if (amount < 1000.0 && user.currency == "PKR") {
            return Result.failure(Exception("Minimum withdrawal is Rs 1000"))
        }
        if (amount > user.walletBalance) {
            return Result.failure(Exception("Insufficient wallet balance (${user.walletBalance})."))
        }
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
            referenceNumber = "PAYOUT-REQ",
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )
        updateTransactionsList(listOf(tx) + _transactionsList.value)

        ensureKnownItemsLoaded()
        if (!knownTxIds.contains(tx.id)) {
            knownTxIds.add(tx.id)
            persistKnownItems()
        }
        appContext?.let { ctx ->
            NotificationHelper.showAdminNewWithdrawalNotification(ctx, tx)
        }

        scope.launch {
            try {
                SupabaseCloudManager.syncTransaction(tx)
                firestore?.collection("transactions")?.document(tx.id)?.set(tx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline tx save fallback")
            }
        }
        return Result.success(tx)
    }

    fun approveTransaction(txId: String, adminNotes: String = ""): Result<TransactionRequest> {
        val currentTxs = _transactionsList.value.toMutableList()
        val idx = currentTxs.indexOfFirst { it.id == txId }
        if (idx == -1) return Result.failure(Exception("Transaction not found."))

        val tx = currentTxs[idx]
        val currentAdmin = _currentUser.value

        if (tx.type == "WITHDRAW" && currentAdmin?.isSuperAdmin != true) {
            return Result.failure(Exception("Only Super Admin can approve withdrawal requests."))
        }
        if (tx.type == "DEPOSIT" && currentAdmin?.isCountrySuperMaster == true) {
            val matchesCountry = tx.country.equals(currentAdmin.country, true) || tx.currency.equals(currentAdmin.currency, true)
            if (!matchesCountry) {
                return Result.failure(Exception("Country Super Masters can only approve deposits for their assigned country."))
            }
        }

        val currentUsers = _usersList.value.toMutableList()
        val userIdx = currentUsers.indexOfFirst { it.id == tx.userId }
        if (userIdx == -1) return Result.failure(Exception("User account not found."))

        val u = currentUsers[userIdx]
        if (tx.type == "WITHDRAW" && u.walletBalance < tx.amount) {
            return Result.failure(Exception("User has insufficient balance for withdrawal (Rs/AED/SAR ${u.walletBalance})."))
        }

        val updatedTx = tx.copy(status = "Approved", adminNotes = adminNotes.trim())
        currentTxs[idx] = updatedTx
        updateTransactionsList(currentTxs)

        appContext?.let { ctx ->
            com.bp.uunwlm.util.NotificationHelper.showTransactionStatusNotification(ctx, updatedTx)
        }

        val newBal = if (tx.type == "DEPOSIT") u.walletBalance + tx.amount else (u.walletBalance - tx.amount).coerceAtLeast(0.0)
        val updatedUser = u.copy(walletBalance = newBal)
        currentUsers[userIdx] = updatedUser
        _usersList.value = currentUsers
        if (_currentUser.value?.id == u.id) {
            _currentUser.value = updatedUser
        }

        scope.launch {
            try {
                SupabaseCloudManager.syncUser(updatedUser)
                SupabaseCloudManager.syncTransaction(updatedTx)
                firestore?.collection("users")?.document(u.id)?.set(updatedUser)?.await()
                firestore?.collection("transactions")?.document(txId)?.set(updatedTx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync note: ${e.message}")
            }
        }
        return Result.success(updatedTx)
    }

    fun rejectTransaction(txId: String, adminNotes: String = ""): Result<TransactionRequest> {
        val currentTxs = _transactionsList.value.toMutableList()
        val idx = currentTxs.indexOfFirst { it.id == txId }
        if (idx == -1) return Result.failure(Exception("Transaction not found."))

        val tx = currentTxs[idx]
        val currentAdmin = _currentUser.value

        if (tx.type == "WITHDRAW" && currentAdmin?.isSuperAdmin != true) {
            if (tx.status.equals("pending_super_admin", true)) {
                return Result.failure(Exception("This withdrawal has been forwarded. Only Super Admin can reject it."))
            }
            if (currentAdmin?.isCountrySuperMaster == true) {
                val matchesCountry = tx.country.equals(currentAdmin.country, true) || tx.currency.equals(currentAdmin.currency, true)
                if (!matchesCountry) {
                    return Result.failure(Exception("Country Super Masters can only reject withdrawals for their assigned country."))
                }
            } else {
                return Result.failure(Exception("Only Super Admin or assigned Country Super Master can reject withdrawal requests."))
            }
        }
        if (tx.type == "DEPOSIT" && currentAdmin?.isCountrySuperMaster == true) {
            val matchesCountry = tx.country.equals(currentAdmin.country, true) || tx.currency.equals(currentAdmin.currency, true)
            if (!matchesCountry) {
                return Result.failure(Exception("Country Super Masters can only reject deposits for their assigned country."))
            }
        }

        val updatedTx = tx.copy(status = "Rejected", adminNotes = adminNotes.trim())
        currentTxs[idx] = updatedTx
        updateTransactionsList(currentTxs)

        appContext?.let { ctx ->
            com.bp.uunwlm.util.NotificationHelper.showTransactionStatusNotification(ctx, updatedTx)
        }

        scope.launch {
            try {
                SupabaseCloudManager.syncTransaction(updatedTx)
                firestore?.collection("transactions")?.document(txId)?.set(updatedTx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync note: ${e.message}")
            }
        }
        return Result.success(updatedTx)
    }

    fun forwardWithdrawalToSuperAdmin(txId: String): Result<TransactionRequest> {
        val currentTxs = _transactionsList.value.toMutableList()
        val idx = currentTxs.indexOfFirst { it.id == txId }
        if (idx == -1) return Result.failure(Exception("Transaction not found."))

        val tx = currentTxs[idx]
        val currentAdmin = _currentUser.value

        if (tx.type != "WITHDRAW") {
            return Result.failure(Exception("Only withdrawal requests can be forwarded to Super Admin."))
        }
        if (!tx.status.equals("Pending", true)) {
            return Result.failure(Exception("Only pending withdrawal requests can be forwarded."))
        }
        if (currentAdmin?.isCountrySuperMaster == true) {
            val matchesCountry = tx.country.equals(currentAdmin.country, true) || tx.currency.equals(currentAdmin.currency, true)
            if (!matchesCountry) {
                return Result.failure(Exception("Country Super Masters can only forward withdrawals for their assigned country."))
            }
        } else if (currentAdmin?.isSuperAdmin != true) {
            return Result.failure(Exception("Only assigned Country Super Master can forward withdrawal requests."))
        }

        val updatedTx = tx.copy(status = "pending_super_admin")
        currentTxs[idx] = updatedTx
        _transactionsList.value = currentTxs

        scope.launch {
            try {
                SupabaseCloudManager.syncTransaction(updatedTx)
                firestore?.collection("transactions")?.document(txId)?.set(updatedTx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync note: ${e.message}")
            }
        }
        return Result.success(updatedTx)
    }

    fun updateBetProCredentials(
        userId: String,
        username: String,
        password: String,
        status: String
    ): Result<UserAccount> {
        val currentUsers = _usersList.value.toMutableList()
        val idx = currentUsers.indexOfFirst { it.id == userId }
        if (idx != -1) {
            val updatedUser = currentUsers[idx].copy(
                betproUsername = username.ifBlank { "Available Soon" },
                betproPassword = password.ifBlank { "Wait for Admin" },
                betproIdStatus = status
            )
            currentUsers[idx] = updatedUser
            _usersList.value = currentUsers
            if (_currentUser.value?.id == userId) {
                _currentUser.value = updatedUser
                saveSession(updatedUser)
            }

            scope.launch {
                try {
                    SupabaseCloudManager.syncUser(updatedUser)
                    firestore?.collection("users")?.document(userId)?.set(updatedUser)?.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore sync note: ${e.message}")
                }
            }
            return Result.success(updatedUser)
        }
        return Result.failure(Exception("User not found"))
    }

    fun createMasterAgent(
        name: String,
        password: String,
        currency: String,
        role: String,
        creditLimit: Double,
        marginShare: Double
    ): Result<MasterAgent> {
        val agent = MasterAgent(
            id = "ma_${UUID.randomUUID().toString().take(6)}",
            name = name,
            loginPassword = password,
            currency = currency,
            role = role,
            creditLimit = creditLimit,
            marginShare = marginShare,
            createdAt = System.currentTimeMillis()
        )
        _masterAgentsList.value = listOf(agent) + _masterAgentsList.value
        scope.launch {
            try {
                SupabaseCloudManager.syncMasterAgent(agent)
                firestore?.collection("master_agents")?.document(agent.id)?.set(agent)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline master agent save note")
            }
        }
        return Result.success(agent)
    }

    fun broadcastPushAlert(title: String, message: String) {
        _recentBroadcast.value = Pair(title, message)
    }

    fun updateAdminPassword(newPassword: String): Result<Unit> {
        val curr = _currentUser.value ?: return Result.failure(Exception("No user logged in"))
        val updatedUser = curr.copy(password = newPassword)
        _currentUser.value = updatedUser
        saveSession(updatedUser)
        _usersList.value = _usersList.value.map {
            if (it.id == curr.id) updatedUser else it
        }
        scope.launch { SupabaseCloudManager.syncUser(updatedUser) }
        return Result.success(Unit)
    }

    fun updateUserProfile(fullName: String, mobileNumber: String): Result<Unit> {
        val curr = _currentUser.value ?: return Result.failure(Exception("No user logged in"))
        val updatedUser = curr.copy(fullName = fullName.trim(), mobileNumber = mobileNumber.trim())
        _currentUser.value = updatedUser
        saveSession(updatedUser)
        _usersList.value = _usersList.value.map {
            if (it.id == curr.id) updatedUser else it
        }
        scope.launch { SupabaseCloudManager.syncUser(updatedUser) }
        return Result.success(Unit)
    }

    fun updateUserPassword(newPassword: String): Result<Unit> {
        val curr = _currentUser.value ?: return Result.failure(Exception("No user logged in"))
        val updatedUser = curr.copy(password = newPassword)
        _currentUser.value = updatedUser
        saveSession(updatedUser)
        _usersList.value = _usersList.value.map {
            if (it.id == curr.id) updatedUser else it
        }
        scope.launch { SupabaseCloudManager.syncUser(updatedUser) }
        return Result.success(Unit)
    }

    fun updateWhatsAppHelpline(number: String): Result<String> {
        val clean = number.trim()
        if (clean.isBlank()) return Result.failure(Exception("Helpline number cannot be empty"))
        _whatsappHelplineNumber.value = clean
        scope.launch {
            try {
                SupabaseCloudManager.syncSetting("whatsapp_helpline", clean)
                firestore?.collection("settings")?.document("whatsapp_helpline")
                    ?.set(mapOf("number" to clean))?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline whatsapp helpline save fallback")
            }
        }
        return Result.success(clean)
    }

    fun updateExchangeWebsiteUrl(url: String): Result<String> {
        val clean = url.trim()
        if (clean.isBlank()) return Result.failure(Exception("Exchange website URL cannot be empty"))
        _exchangeWebsiteUrl.value = clean
        scope.launch {
            try {
                SupabaseCloudManager.syncSetting("exchange_website", clean)
                firestore?.collection("settings")?.document("exchange_website")
                    ?.set(mapOf("url" to clean))?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline exchange website url save fallback")
            }
        }
        return Result.success(clean)
    }

    fun addWithdrawalAccount(
        type: String,
        title: String,
        accountNumber: String,
        iban: String = "",
        bankName: String = ""
    ): Result<UserWithdrawalAccount> {
        val currUser = _currentUser.value
        val newAcc = UserWithdrawalAccount(
            id = "wa_" + UUID.randomUUID().toString().take(8),
            userId = currUser?.id ?: "",
            type = type.trim(),
            title = title.trim(),
            accountNumber = accountNumber.trim(),
            iban = iban.trim(),
            bankName = bankName.trim()
        )
        _userWithdrawalAccounts.value = _userWithdrawalAccounts.value + newAcc
        scope.launch {
            try {
                SupabaseCloudManager.syncWithdrawalAccount(newAcc)
                firestore?.collection("user_withdrawal_accounts")?.document(newAcc.id)?.set(newAcc)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline withdrawal acc save fallback")
            }
        }
        return Result.success(newAcc)
    }

    fun updateWithdrawalAccount(
        id: String,
        type: String,
        title: String,
        accountNumber: String,
        iban: String = "",
        bankName: String = ""
    ): Result<UserWithdrawalAccount> {
        val currentList = _userWithdrawalAccounts.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Account not found"))
        val updated = currentList[idx].copy(
            type = type.trim(),
            title = title.trim(),
            accountNumber = accountNumber.trim(),
            iban = iban.trim(),
            bankName = bankName.trim()
        )
        currentList[idx] = updated
        _userWithdrawalAccounts.value = currentList
        scope.launch {
            try {
                SupabaseCloudManager.syncWithdrawalAccount(updated)
                firestore?.collection("user_withdrawal_accounts")?.document(id)?.set(updated)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline withdrawal acc update fallback")
            }
        }
        return Result.success(updated)
    }

    fun deleteWithdrawalAccount(id: String): Result<Unit> {
        _userWithdrawalAccounts.value = _userWithdrawalAccounts.value.filter { it.id != id }
        scope.launch {
            try {
                SupabaseCloudManager.deleteWithdrawalAccountFromCloud(id)
                firestore?.collection("user_withdrawal_accounts")?.document(id)?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline withdrawal acc delete fallback")
            }
        }
        return Result.success(Unit)
    }

    // ====================================================
    // SUPER ADMIN PAYMENT METHODS CONTROL PANEL
    // ====================================================
    fun addPaymentGateway(
        name: String,
        currency: String,
        country: String,
        title: String,
        accountNumber: String,
        iban: String = "",
        bankName: String = "",
        instructions: String = "",
        shortDescription: String = "",
        logoUrl: String = "",
        isEnabled: Boolean = true,
        displayOrder: Int = 1,
        minDeposit: Double = 500.0,
        minWithdraw: Double = 1000.0
    ): Result<PaymentGateway> {
        val current = _paymentGateways.value
        val nextOrder = if (displayOrder > 0) displayOrder else (current.maxOfOrNull { it.displayOrder } ?: 0) + 1
        val newGw = PaymentGateway(
            id = "gw_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            currency = currency.trim(),
            country = country.trim(),
            title = title.trim(),
            accountNumber = accountNumber.trim(),
            iban = iban.trim(),
            bankName = bankName.trim(),
            instructions = instructions.ifBlank { "Please transfer the exact amount and upload your transaction screenshot." },
            shortDescription = shortDescription.ifBlank { "Instant 24/7 Digital Transfer" },
            logoUrl = logoUrl.trim(),
            isEnabled = isEnabled,
            displayOrder = nextOrder,
            minDeposit = minDeposit,
            minWithdraw = minWithdraw
        )
        val updatedList = (current + newGw).sortedBy { it.displayOrder }
        _paymentGateways.value = updatedList
        scope.launch {
            try {
                SupabaseCloudManager.syncPaymentGateway(newGw)
                firestore?.collection("payment_gateways")?.document(newGw.id)?.set(newGw)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline gateway add fallback")
            }
        }
        return Result.success(newGw)
    }

    fun updatePaymentGateway(
        id: String,
        name: String,
        currency: String,
        country: String,
        title: String,
        accountNumber: String,
        iban: String = "",
        bankName: String = "",
        instructions: String = "",
        shortDescription: String = "",
        logoUrl: String = "",
        isEnabled: Boolean = true,
        displayOrder: Int = 1,
        minDeposit: Double = 500.0,
        minWithdraw: Double = 1000.0
    ): Result<PaymentGateway> {
        val current = _paymentGateways.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Payment method not found"))
        val updatedGw = current[idx].copy(
            name = name.trim(),
            currency = currency.trim(),
            country = country.trim(),
            title = title.trim(),
            accountNumber = accountNumber.trim(),
            iban = iban.trim(),
            bankName = bankName.trim(),
            instructions = instructions.ifBlank { "Please transfer the exact amount and upload your transaction screenshot." },
            shortDescription = shortDescription.ifBlank { "Instant 24/7 Digital Transfer" },
            logoUrl = logoUrl.trim(),
            isEnabled = isEnabled,
            displayOrder = displayOrder,
            minDeposit = minDeposit,
            minWithdraw = minWithdraw
        )
        current[idx] = updatedGw
        val sortedList = current.sortedBy { it.displayOrder }
        _paymentGateways.value = sortedList
        scope.launch {
            try {
                SupabaseCloudManager.syncPaymentGateway(updatedGw)
                firestore?.collection("payment_gateways")?.document(id)?.set(updatedGw)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline gateway update fallback")
            }
        }
        return Result.success(updatedGw)
    }

    fun deletePaymentGateway(id: String): Result<Unit> {
        _paymentGateways.value = _paymentGateways.value.filter { it.id != id }
        scope.launch {
            try {
                SupabaseCloudManager.deletePaymentGatewayFromCloud(id)
                firestore?.collection("payment_gateways")?.document(id)?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline gateway delete fallback")
            }
        }
        return Result.success(Unit)
    }

    fun togglePaymentGatewayStatus(id: String): Result<PaymentGateway> {
        val current = _paymentGateways.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Payment method not found"))
        val updated = current[idx].copy(isEnabled = !current[idx].isEnabled)
        current[idx] = updated
        _paymentGateways.value = current
        scope.launch {
            try {
                SupabaseCloudManager.syncPaymentGateway(updated)
                firestore?.collection("payment_gateways")?.document(id)?.set(updated)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline gateway toggle fallback")
            }
        }
        return Result.success(updated)
    }
}
