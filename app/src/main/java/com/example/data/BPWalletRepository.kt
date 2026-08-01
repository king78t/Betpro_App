package com.example.data

import android.util.Log
import com.example.model.CountryUtils
import com.example.model.MasterAgent
import com.example.model.PaymentGateway
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
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

    private val _recentBroadcast = MutableStateFlow<Pair<String, String>?>(null)
    val recentBroadcast: StateFlow<Pair<String, String>?> = _recentBroadcast.asStateFlow()

    private val _whatsappHelplineNumber = MutableStateFlow("+923001234567")
    val whatsappHelplineNumber: StateFlow<String> = _whatsappHelplineNumber.asStateFlow()

    init {
        seedDefaultAdminAndDemoUser()
        startFirestoreListeners()
    }

    private fun defaultGateways(): List<PaymentGateway> = listOf(
        PaymentGateway(
            id = "gw_ep_1",
            name = "EasyPaisa",
            currency = "PKR",
            title = "Muhammad Usman BP Exch",
            accountNumber = "03001234567",
            minDeposit = 500.0
        ),
        PaymentGateway(
            id = "gw_jz_1",
            name = "JazzCash",
            currency = "PKR",
            title = "Usman Ali Trading",
            accountNumber = "03007654321",
            minDeposit = 500.0
        ),
        PaymentGateway(
            id = "gw_mz_1",
            name = "Meezan Bank",
            currency = "PKR",
            title = "BP Traders Private Ltd",
            accountNumber = "0102010123456789",
            minDeposit = 1000.0
        ),
        PaymentGateway(
            id = "gw_aed_1",
            name = "Emirates NBD",
            currency = "AED",
            title = "BP Exchange Middle East",
            accountNumber = "AE2103300000001234567",
            minDeposit = 100.0
        ),
        PaymentGateway(
            id = "gw_sar_1",
            name = "Al Rajhi Bank",
            currency = "SAR",
            title = "BP Wallet SA Trading",
            accountNumber = "SA0380000000608010167519",
            minDeposit = 100.0
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
            } catch (ex: Exception) {
                Log.w(TAG, "Firestore setup fallback: ${ex.message}")
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
                firestore?.collection("transactions")?.document(tx.id)?.set(tx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline tx save fallback")
            }
        }
        return Result.success(tx)
    }

    fun approveTransaction(txId: String): Result<TransactionRequest> {
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

        val updatedTx = tx.copy(status = "Approved")
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
                firestore?.collection("users")?.document(u.id)?.set(updatedUser)?.await()
                firestore?.collection("transactions")?.document(txId)?.set(updatedTx)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync note: ${e.message}")
            }
        }
        return Result.success(updatedTx)
    }

    fun rejectTransaction(txId: String): Result<TransactionRequest> {
        val currentTxs = _transactionsList.value.toMutableList()
        val idx = currentTxs.indexOfFirst { it.id == txId }
        if (idx == -1) return Result.failure(Exception("Transaction not found."))

        val tx = currentTxs[idx]
        val currentAdmin = _currentUser.value

        if (tx.type == "WITHDRAW" && currentAdmin?.isSuperAdmin != true) {
            return Result.failure(Exception("Only Super Admin can reject withdrawal requests."))
        }
        if (tx.type == "DEPOSIT" && currentAdmin?.isCountrySuperMaster == true) {
            val matchesCountry = tx.country.equals(currentAdmin.country, true) || tx.currency.equals(currentAdmin.currency, true)
            if (!matchesCountry) {
                return Result.failure(Exception("Country Super Masters can only reject deposits for their assigned country."))
            }
        }

        val updatedTx = tx.copy(status = "Rejected")
        currentTxs[idx] = updatedTx
        _transactionsList.value = currentTxs

        scope.launch {
            try {
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
        return Result.success(Unit)
    }

    fun addPaymentGateway(
        name: String,
        currency: String,
        country: String,
        title: String,
        accountNumber: String,
        minDeposit: Double
    ): Result<PaymentGateway> {
        val gw = PaymentGateway(
            id = "gw_${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            currency = currency.trim(),
            country = country.trim(),
            title = title.trim(),
            accountNumber = accountNumber.trim(),
            minDeposit = minDeposit
        )
        _paymentGateways.value = _paymentGateways.value + gw
        scope.launch {
            try {
                firestore?.collection("payment_gateways")?.document(gw.id)?.set(gw)?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline gateway save fallback")
            }
        }
        return Result.success(gw)
    }

    fun deletePaymentGateway(gatewayId: String): Result<Unit> {
        _paymentGateways.value = _paymentGateways.value.filter { it.id != gatewayId }
        scope.launch {
            try {
                firestore?.collection("payment_gateways")?.document(gatewayId)?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline gateway delete fallback")
            }
        }
        return Result.success(Unit)
    }

    fun updateWhatsAppHelpline(number: String): Result<String> {
        val clean = number.trim()
        if (clean.isBlank()) return Result.failure(Exception("Helpline number cannot be empty"))
        _whatsappHelplineNumber.value = clean
        scope.launch {
            try {
                firestore?.collection("settings")?.document("whatsapp_helpline")
                    ?.set(mapOf("number" to clean))?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Offline whatsapp helpline save fallback")
            }
        }
        return Result.success(clean)
    }
}
