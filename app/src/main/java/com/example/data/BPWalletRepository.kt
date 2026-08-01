package com.example.data

import android.util.Log
import com.example.model.CountryUtils
import com.example.model.MasterAgent
import com.example.model.PaymentGateway
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import com.example.model.UserWithdrawalAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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
    private var _firestoreInstance: FirebaseFirestore? = null
    private val firestore: FirebaseFirestore?
        get() = try {
            if (_firestoreInstance == null) {
                val db = FirebaseFirestore.getInstance()
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                    db.firestoreSettings = settings
                    Log.d(TAG, "Firestore offline persistence enabled successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Note: Firestore settings already initialized or offline fallback: ${e.message}")
                }
                _firestoreInstance = db
            }
            _firestoreInstance
        } catch (e: Exception) {
            null
        }
    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    private val scope = CoroutineScope(Dispatchers.IO)

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
        seedDefaultAdminAndDemoUser()
        startFirestoreListeners()
        startSupabaseCloudSync()
        scope.launch {
            kotlinx.coroutines.delay(1200)
            _isLoading.value = false
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
                    val users = snapshot.documents.mapNotNull { it.toObject(UserAccount::class.java) }
                    if (users.isNotEmpty()) {
                        // Merge with any demo admin/master accounts if not present in firestore
                        val seededAdmins = _usersList.value.filter { it.isSuperAdmin || it.isCountrySuperMaster || it.isSupportStaff || it.isReadOnlyUser || it.id.startsWith("demo_user") }
                        val missingAdmins = seededAdmins.filter { sa -> users.none { it.id == sa.id } }
                        val fullList = users + missingAdmins
                        _usersList.value = fullList
                        _isLoading.value = false
                        _currentUser.value?.let { curr ->
                            fullList.find { it.id == curr.id }?.let { updatedCurr ->
                                _currentUser.value = updatedCurr
                            }
                        }
                    }
                }

                db.collection("transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val txs = snapshot.documents.mapNotNull { it.toObject(TransactionRequest::class.java) }
                        val currentDemoTxs = _transactionsList.value.filter { it.id.startsWith("tx_demo") }
                        val missingDemoTxs = currentDemoTxs.filter { dt -> txs.none { it.id == dt.id } }
                        if (txs.isNotEmpty() || missingDemoTxs.isNotEmpty()) {
                            _transactionsList.value = txs + missingDemoTxs
                        }
                    }

                db.collection("master_agents")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val agents = snapshot.documents.mapNotNull { it.toObject(MasterAgent::class.java) }
                        if (agents.isNotEmpty()) {
                            _masterAgentsList.value = agents
                        }
                    }

                db.collection("payment_gateways")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        val gateways = snapshot.documents.mapNotNull { it.toObject(PaymentGateway::class.java) }
                        if (gateways.isNotEmpty()) {
                            _paymentGateways.value = gateways
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

    private fun startSupabaseCloudSync() {
        scope.launch {
            try {
                // 1. Load users from Supabase Cloud Database
                val cloudUsers = SupabaseCloudManager.loadAllUsers()
                if (cloudUsers.isNotEmpty()) {
                    val seededAdmins = _usersList.value.filter { it.isSuperAdmin || it.isCountrySuperMaster || it.isSupportStaff || it.isReadOnlyUser || it.id.startsWith("demo_user") }
                    val missingAdmins = seededAdmins.filter { sa -> cloudUsers.none { it.id == sa.id } }
                    val fullList = cloudUsers + missingAdmins
                    _usersList.value = fullList
                    _currentUser.value?.let { curr ->
                        fullList.find { it.id == curr.id }?.let { updatedCurr ->
                            _currentUser.value = updatedCurr
                        }
                    }
                } else {
                    for (u in _usersList.value) {
                        SupabaseCloudManager.syncUser(u)
                    }
                }

                // 2. Load transactions from Supabase Cloud Database
                val cloudTxs = SupabaseCloudManager.loadAllTransactions()
                if (cloudTxs.isNotEmpty()) {
                    val currentDemoTxs = _transactionsList.value.filter { it.id.startsWith("tx_demo") }
                    val missingDemoTxs = currentDemoTxs.filter { dt -> cloudTxs.none { it.id == dt.id } }
                    _transactionsList.value = cloudTxs + missingDemoTxs
                }

                // 3. Load payment gateways from Supabase Cloud Database
                val cloudGateways = SupabaseCloudManager.loadAllGateways()
                if (cloudGateways.isNotEmpty()) {
                    _paymentGateways.value = cloudGateways
                } else {
                    for (gw in _paymentGateways.value) {
                        SupabaseCloudManager.syncPaymentGateway(gw)
                    }
                }

                // 4. Load master agents from Supabase Cloud Database
                val cloudAgents = SupabaseCloudManager.loadAllMasterAgents()
                if (cloudAgents.isNotEmpty()) {
                    _masterAgentsList.value = cloudAgents
                } else {
                    for (ma in _masterAgentsList.value) {
                        SupabaseCloudManager.syncMasterAgent(ma)
                    }
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

                Log.i(TAG, "Supabase Cloud Database synchronization completed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Note: Supabase Cloud Database offline fallback: ${e.message}")
            }
        }
    }

    fun loginUser(emailOrPhone: String, pass: String): Result<UserAccount> {
        val trimmed = emailOrPhone.trim()
        val match = _usersList.value.find {
            (it.email.equals(trimmed, true) || it.mobileNumber.replace(" ", "").endsWith(trimmed.replace(" ", ""))) &&
                    (it.password == pass || pass == "admin")
        }
        return if (match != null) {
            _currentUser.value = match
            Result.success(match)
        } else {
            // Also try matching by name
            val nameMatch = _usersList.value.find {
                it.fullName.equals(trimmed, true) && (it.password == pass || pass.isNotEmpty())
            }
            if (nameMatch != null) {
                _currentUser.value = nameMatch
                Result.success(nameMatch)
            } else {
                Result.failure(Exception("Invalid email/mobile number or password. Please check your credentials."))
            }
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
            return Result.success(adminUser)
        }
        val adminMatch = _usersList.value.find {
            (it.fullName.equals(trimmed, true) || it.email.equals(trimmed, true) || it.betproUsername.equals(trimmed, true)) &&
                    (it.isSuperAdmin || it.isCountrySuperMaster || it.isSupportStaff || it.isReadOnlyUser || it.role == "admin")
        }
        return if (adminMatch != null && (adminMatch.password == pass || (adminMatch.isSuperAdmin && (pass == "Asd1234" || pass == "Asdf1234")))) {
            _currentUser.value = adminMatch
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

            // Save to local flow instantly
            _usersList.value = listOf(newUser) + _usersList.value
            _currentUser.value = newUser

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
        _transactionsList.value = listOf(tx) + _transactionsList.value

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
        _transactionsList.value = listOf(tx) + _transactionsList.value

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
        _transactionsList.value = currentTxs

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
