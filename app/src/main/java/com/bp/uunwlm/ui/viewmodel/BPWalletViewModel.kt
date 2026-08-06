package com.bp.uunwlm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bp.uunwlm.data.BPWalletRepository
import com.bp.uunwlm.model.MasterAgent
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class ScreenType {
    SPLASH,
    LOGIN,
    REGISTER,
    USER_HOME,
    USER_DEPOSIT,
    USER_WITHDRAW,
    USER_HISTORY,
    USER_PROFILE,
    ADMIN_DASHBOARD,
    ADMIN_USERS_CRM,
    ADMIN_MASTER_AGENTS,
    ADMIN_TRANSACTIONS,
    ADMIN_LIVE_CONTROL,
    ADMIN_SETTINGS
}

class BPWalletViewModel : ViewModel() {

    val currentUser = BPWalletRepository.currentUser
    val allUsers = BPWalletRepository.usersList
    val allTransactions = BPWalletRepository.transactionsList
    val masterAgents = BPWalletRepository.masterAgentsList
    val paymentGateways = BPWalletRepository.paymentGateways
    val recentBroadcast = BPWalletRepository.recentBroadcast
    val whatsappHelplineNumber = BPWalletRepository.whatsappHelplineNumber
    val exchangeWebsiteUrl = BPWalletRepository.exchangeWebsiteUrl
    val isLoading = BPWalletRepository.isLoading

    private fun getInitialScreen(): ScreenType {
        val user = currentUser.value
        return if (user != null) {
            if (user.isAdminRole) ScreenType.ADMIN_DASHBOARD else ScreenType.USER_HOME
        } else {
            ScreenType.LOGIN
        }
    }

    private val _currentScreen = MutableStateFlow(getInitialScreen())
    val currentScreen: StateFlow<ScreenType> = _currentScreen.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    val current = _currentScreen.value
                    if (current == ScreenType.LOGIN || current == ScreenType.REGISTER || current == ScreenType.SPLASH) {
                        _currentScreen.value = if (user.isAdminRole) ScreenType.ADMIN_DASHBOARD else ScreenType.USER_HOME
                    } else if (user.isAdminRole && !isScreenAdmin(current)) {
                        _currentScreen.value = ScreenType.ADMIN_DASHBOARD
                    } else if (!user.isAdminRole && isScreenAdmin(current)) {
                        _currentScreen.value = ScreenType.USER_HOME
                    }
                } else {
                    if (_currentScreen.value != ScreenType.SPLASH && _currentScreen.value != ScreenType.REGISTER) {
                        _currentScreen.value = ScreenType.LOGIN
                    }
                }
            }
        }
    }

    private fun isScreenAdmin(screen: ScreenType): Boolean {
        return screen == ScreenType.ADMIN_DASHBOARD ||
               screen == ScreenType.ADMIN_LIVE_CONTROL ||
               screen == ScreenType.ADMIN_USERS_CRM ||
               screen == ScreenType.ADMIN_MASTER_AGENTS ||
               screen == ScreenType.ADMIN_TRANSACTIONS ||
               screen == ScreenType.ADMIN_SETTINGS
    }

    // Login tab mode: true = User, false = Admin
    private val _isUserLoginTab = MutableStateFlow(true)
    val isUserLoginTab: StateFlow<Boolean> = _isUserLoginTab.asStateFlow()

    // Snackbar / Alert message
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    // Shake animation trigger for failed login attempts / visual feedback
    private val _errorShakeTrigger = MutableStateFlow(0)
    val errorShakeTrigger: StateFlow<Int> = _errorShakeTrigger.asStateFlow()

    fun triggerErrorShake() {
        _errorShakeTrigger.value += 1
    }

    // Deposit flow state
    private val _selectedDepositGateway = MutableStateFlow<PaymentGateway?>(null)
    val selectedDepositGateway: StateFlow<PaymentGateway?> = _selectedDepositGateway.asStateFlow()

    // Selected User for admin modal
    private val _selectedUserForCreds = MutableStateFlow<UserAccount?>(null)
    val selectedUserForCreds: StateFlow<UserAccount?> = _selectedUserForCreds.asStateFlow()

    // Selected user for balance edit modal
    private val _selectedUserForBalance = MutableStateFlow<UserAccount?>(null)
    val selectedUserForBalance: StateFlow<UserAccount?> = _selectedUserForBalance.asStateFlow()

    // Admin Users CRM filters
    private val _crmCurrencyFilter = MutableStateFlow("All Curr")
    val crmCurrencyFilter: StateFlow<String> = _crmCurrencyFilter.asStateFlow()

    private val _crmStatusFilter = MutableStateFlow("All Status")
    val crmStatusFilter: StateFlow<String> = _crmStatusFilter.asStateFlow()

    private val _crmSearchQuery = MutableStateFlow("")
    val crmSearchQuery: StateFlow<String> = _crmSearchQuery.asStateFlow()

    // Selected Master Agent for detail modal/view
    private val _selectedMasterForDetails = MutableStateFlow<MasterAgent?>(null)
    val selectedMasterForDetails: StateFlow<MasterAgent?> = _selectedMasterForDetails.asStateFlow()

    // Transaction for Deposit Proof Screenshot Verification modal
    private val _txForProofVerification = MutableStateFlow<TransactionRequest?>(null)
    val txForProofVerification: StateFlow<TransactionRequest?> = _txForProofVerification.asStateFlow()

    // Dialog state for BetPro Exchange Info modal
    private val _showBetProExchangeModal = MutableStateFlow(false)
    val showBetProExchangeModal: StateFlow<Boolean> = _showBetProExchangeModal.asStateFlow()

    // Dialog state for Create Master Agent modal
    private val _showCreateMasterModal = MutableStateFlow(false)
    val showCreateMasterModal: StateFlow<Boolean> = _showCreateMasterModal.asStateFlow()

    fun setScreen(screen: ScreenType) {
        _currentScreen.value = screen
    }

    fun onSplashCompleted() {
        val user = currentUser.value
        if (user != null) {
            if (user.isAdminRole) {
                setScreen(ScreenType.ADMIN_DASHBOARD)
            } else {
                setScreen(ScreenType.USER_HOME)
            }
        } else {
            setScreen(ScreenType.LOGIN)
        }
    }

    fun setLoginTab(isUser: Boolean) {
        _isUserLoginTab.value = isUser
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun showSnack(message: String) {
        _snackMessage.value = message
    }

    fun loginUser(emailOrPhone: String, pass: String) {
        if (emailOrPhone.isBlank() || pass.isBlank()) {
            triggerErrorShake()
            showSnack("Please enter email/phone and password")
            return
        }
        val result = BPWalletRepository.loginUser(emailOrPhone, pass)
        result.onSuccess { user ->
            showSnack("Welcome Back, ${user.fullName}!")
            if (user.isAdminRole) {
                setScreen(ScreenType.ADMIN_DASHBOARD)
            } else {
                setScreen(ScreenType.USER_HOME)
            }
        }.onFailure { err ->
            triggerErrorShake()
            showSnack(err.message ?: "Login failed")
        }
    }

    fun loginAdmin(username: String, pass: String) {
        if (username.isBlank() || pass.isBlank()) {
            triggerErrorShake()
            showSnack("Please enter admin username and password")
            return
        }
        val result = BPWalletRepository.loginAdmin(username, pass)
        result.onSuccess { admin ->
            showSnack("Admin logged in successfully! Welcome ${admin.fullName}")
            if (admin.isAdminRole) {
                setScreen(ScreenType.ADMIN_DASHBOARD)
            } else {
                setScreen(ScreenType.USER_HOME)
            }
        }.onFailure { err ->
            triggerErrorShake()
            showSnack(err.message ?: "Admin login failed")
        }
    }

    fun registerUser(
        fullName: String,
        email: String,
        currency: String,
        mobileNumber: String,
        pass: String
    ) {
        if (fullName.isBlank() || email.isBlank() || mobileNumber.isBlank() || pass.isBlank()) {
            triggerErrorShake()
            showSnack("Please fill in all required fields.")
            return
        }
        val trimmedEmail = email.trim()
        if (!trimmedEmail.contains("@")) {
            triggerErrorShake()
            showSnack("Please enter a valid email address.")
            return
        }
        val cleanPhone = mobileNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.length < 5) {
            triggerErrorShake()
            showSnack("Please enter a valid phone number.")
            return
        }
        if (pass.length < 4) {
            triggerErrorShake()
            showSnack("Password must be at least 4 characters long.")
            return
        }
        viewModelScope.launch {
            val result = BPWalletRepository.registerUser(
                fullName = fullName,
                email = trimmedEmail,
                currency = currency,
                mobileNumber = mobileNumber,
                password = pass
            )
            result.onSuccess { user ->
                showSnack("Account created successfully! Welcome to BP Wallet, ${user.fullName}.")
                setScreen(ScreenType.USER_HOME)
            }.onFailure { err ->
                triggerErrorShake()
                showSnack("Registration blocked: ${err.message}")
            }
        }
    }

    fun signInWithGoogle(email: String, displayName: String, idToken: String? = null) {
        viewModelScope.launch {
            val result = BPWalletRepository.signInWithGoogle(email, displayName, idToken)
            result.onSuccess { user ->
                showSnack("Welcome, ${user.fullName}! Signed in with Google via Firebase Auth.")
                setScreen(ScreenType.USER_HOME)
            }.onFailure { err ->
                triggerErrorShake()
                showSnack("Google Sign-in error: ${err.message}")
            }
        }
    }

    private val _phoneVerificationId = MutableStateFlow<String?>(null)
    val phoneVerificationId: StateFlow<String?> = _phoneVerificationId.asStateFlow()

    private val _showOtpDialog = MutableStateFlow(false)
    val showOtpDialog: StateFlow<Boolean> = _showOtpDialog.asStateFlow()

    fun startPhoneLogin(phoneNumber: String, activity: android.app.Activity) {
        if (phoneNumber.isBlank() || phoneNumber.length < 8) {
            showSnack("Please enter a valid phone number")
            return
        }
        
        val callbacks = object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                // Auto-verification handled in some cases
                viewModelScope.launch {
                    val res = BPWalletRepository.verifyPhoneCode(credential.smsCode ?: "", _phoneVerificationId.value ?: "")
                    res.onSuccess { setScreen(ScreenType.USER_HOME) }
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                showSnack("Verification failed: ${e.message}")
            }

            override fun onCodeSent(verificationId: String, token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken) {
                _phoneVerificationId.value = verificationId
                _showOtpDialog.value = true
                showSnack("Verification code sent to $phoneNumber")
            }
        }
        
        BPWalletRepository.startPhoneVerification(phoneNumber, activity, callbacks)
    }

    fun verifyOtp(code: String) {
        val vid = _phoneVerificationId.value
        if (vid == null) {
            showSnack("Session expired. Please try again.")
            return
        }
        viewModelScope.launch {
            val res = BPWalletRepository.verifyPhoneCode(code, vid)
            res.onSuccess {
                _showOtpDialog.value = false
                showSnack("Phone verified! Welcome to BP Wallet.")
                setScreen(ScreenType.USER_HOME)
            }.onFailure {
                showSnack("Invalid verification code.")
            }
        }
    }

    fun closeOtpDialog() {
        _showOtpDialog.value = false
    }

    fun logout() {
        BPWalletRepository.logout()
        _currentScreen.value = ScreenType.LOGIN
        showSnack("Logged out of account")
    }

    // Deposit Flow
    fun selectDepositGateway(gateway: PaymentGateway?) {
        _selectedDepositGateway.value = gateway
    }

    fun submitDepositRequest(amount: Double, reference: String = "PROOF_UPLOADED", screenshotUri: String = "") {
        val gw = _selectedDepositGateway.value
        if (gw == null) {
            showSnack("Please select a payment method first")
            return
        }
        val curr = currentUser.value?.currency ?: gw.currency
        if (amount < gw.minDeposit) {
            showSnack("Minimum deposit for ${gw.name} is $curr ${gw.minDeposit.toInt()}")
            return
        }
        val refToSave = if (reference.isBlank()) "PROOF_UPLOADED" else reference
        val result = BPWalletRepository.createDepositRequest(amount, gw.name, refToSave, screenshotUri)
        result.onSuccess {
            showSnack("Deposit Request of $curr ${amount.toInt()} submitted! Awaiting Admin approval.")
            _selectedDepositGateway.value = null
            setScreen(ScreenType.USER_HOME)
        }.onFailure { err ->
            showSnack(err.message ?: "Could not create deposit request")
        }
    }

    fun submitWithdrawalRequest(
        amount: Double,
        gatewayName: String,
        accountTitle: String,
        accountNumberOrIban: String
    ) {
        if (accountTitle.isBlank() || accountNumberOrIban.isBlank()) {
            showSnack("Please enter Account Title and Number/IBAN")
            return
        }
        val result = BPWalletRepository.createWithdrawalRequest(amount, gatewayName, accountTitle, accountNumberOrIban)
        result.onSuccess {
            showSnack("Withdrawal Request of Rs ${amount.toInt()} submitted successfully!")
            setScreen(ScreenType.USER_HOME)
        }.onFailure { err ->
            showSnack(err.message ?: "Withdrawal error")
        }
    }

    // Admin Actions
    fun approveTransaction(txId: String, notes: String = "") {
        val res = BPWalletRepository.approveTransaction(txId, notes)
        res.onSuccess { tx ->
            showSnack("${tx.type} approved successfully!")
        }.onFailure { err ->
            showSnack(err.message ?: "Approval failed.")
        }
    }

    fun rejectTransaction(txId: String, notes: String = "") {
        val res = BPWalletRepository.rejectTransaction(txId, notes)
        res.onSuccess { tx ->
            showSnack("${tx.type} rejected.")
        }.onFailure { err ->
            showSnack(err.message ?: "Rejection failed.")
        }
    }

    fun forwardWithdrawalToSuperAdmin(txId: String) {
        val res = BPWalletRepository.forwardWithdrawalToSuperAdmin(txId)
        res.onSuccess {
            showSnack("Withdrawal forwarded to Super Admin for payment release.")
        }.onFailure { err ->
            showSnack(err.message ?: "Forwarding failed.")
        }
    }

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
    ) {
        val res = BPWalletRepository.addPaymentGateway(
            name, currency, country, title, accountNumber, iban, bankName, instructions, shortDescription, logoUrl, isEnabled, displayOrder, minDeposit, minWithdraw
        )
        res.onSuccess {
            showSnack("Payment Method added successfully!")
        }.onFailure { err ->
            showSnack(err.message ?: "Failed to add payment method")
        }
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
    ) {
        val res = BPWalletRepository.updatePaymentGateway(
            id, name, currency, country, title, accountNumber, iban, bankName, instructions, shortDescription, logoUrl, isEnabled, displayOrder, minDeposit, minWithdraw
        )
        res.onSuccess {
            showSnack("Payment Method updated successfully!")
        }.onFailure { err ->
            showSnack(err.message ?: "Failed to update payment method")
        }
    }

    fun togglePaymentGatewayStatus(id: String) {
        val res = BPWalletRepository.togglePaymentGatewayStatus(id)
        res.onSuccess { gw ->
            val statusStr = if (gw.isEnabled) "Enabled" else "Disabled"
            showSnack("${gw.name} is now $statusStr")
        }.onFailure { err ->
            showSnack(err.message ?: "Failed to toggle status")
        }
    }

    fun deletePaymentGateway(gatewayId: String) {
        val res = BPWalletRepository.deletePaymentGateway(gatewayId)
        res.onSuccess {
            showSnack("Payment Method removed.")
        }.onFailure { err ->
            showSnack(err.message ?: "Failed to remove payment method")
        }
    }

    fun openBetProCredsModal(user: UserAccount) {
        _selectedUserForCreds.value = user
    }

    fun closeBetProCredsModal() {
        _selectedUserForCreds.value = null
    }

    fun saveBetProCredentials(userId: String, username: String, pass: String, status: String) {
        val result = BPWalletRepository.updateBetProCredentials(userId, username, pass, status)
        result.onSuccess {
            showSnack("User account updated & user notified!")
            _selectedUserForCreds.value = null
        }.onFailure {
            showSnack("Failed to update credentials")
        }
    }

    fun setCrmCurrencyFilter(currency: String) {
        _crmCurrencyFilter.value = currency
    }

    fun setCrmStatusFilter(status: String) {
        _crmStatusFilter.value = status
    }

    fun openMasterDetails(agent: MasterAgent) {
        _selectedMasterForDetails.value = agent
    }

    fun closeMasterDetails() {
        _selectedMasterForDetails.value = null
    }

    fun filterCrmByMaster(agent: MasterAgent) {
        _crmSearchQuery.value = agent.name
        _crmCurrencyFilter.value = agent.currency
        closeMasterDetails()
        setScreen(ScreenType.ADMIN_USERS_CRM)
    }

    fun openProofVerification(tx: TransactionRequest) {
        _txForProofVerification.value = tx
    }

    fun closeProofVerification() {
        _txForProofVerification.value = null
    }

    fun setCrmSearchQuery(query: String) {
        _crmSearchQuery.value = query
    }

    fun openCreateMasterModal() {
        _showCreateMasterModal.value = true
    }

    fun closeCreateMasterModal() {
        _showCreateMasterModal.value = false
    }

    fun createMasterAgent(
        name: String,
        password: String,
        currency: String,
        role: String,
        creditLimit: Double,
        marginShare: Double
    ) {
        if (name.isBlank()) {
            showSnack("Please enter Master Agent/Agency Name")
            return
        }
        val result = BPWalletRepository.createMasterAgent(name, password, currency, role, creditLimit, marginShare)
        result.onSuccess { agent ->
            showSnack("Master Agent '${agent.name}' created in ${agent.currency}!")
            closeCreateMasterModal()
        }.onFailure {
            showSnack("Could not create master agent")
        }
    }

    fun broadcastPushAlert(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) {
            showSnack("Please enter announcement title and message")
            return
        }
        BPWalletRepository.broadcastPushAlert(title, message)
        showSnack("Push alert broadcasted to all active wallets!")
    }

    fun setBetProExchangeModalVisible(visible: Boolean) {
        _showBetProExchangeModal.value = visible
    }

    // Quick seed demo deposit for testing
    fun seedDemoDepositRequest() {
        BPWalletRepository.createDepositRequest(1500.0, "EasyPaisa", "EPX-99881122")
        showSnack("Demo Deposit request created!")
    }

    fun updateAdminPassword(newPassword: String) {
        if (newPassword.isBlank() || newPassword.length < 4) {
            showSnack("Password must be at least 4 characters")
            return
        }
        val result = BPWalletRepository.updateAdminPassword(newPassword)
        result.onSuccess {
            showSnack("Admin / SuperAdmin password updated successfully!")
        }.onFailure {
            showSnack("Failed to update password: ${it.message}")
        }
    }

    fun updateWhatsAppHelpline(number: String) {
        if (number.isBlank()) {
            showSnack("Please enter a valid WhatsApp Helpline Number")
            return
        }
        val result = BPWalletRepository.updateWhatsAppHelpline(number)
        result.onSuccess {
            showSnack("WhatsApp Helpline updated to $it")
        }.onFailure {
            showSnack("Failed to update WhatsApp Helpline: ${it.message}")
        }
    }

    fun updateUserProfile(fullName: String, mobileNumber: String) {
        if (fullName.isBlank()) {
            showSnack("Full Name cannot be empty")
            return
        }
        val result = BPWalletRepository.updateUserProfile(fullName, mobileNumber)
        result.onSuccess {
            showSnack("Profile details updated successfully!")
        }.onFailure {
            showSnack("Failed to update profile: ${it.message}")
        }
    }

    fun updateUserPassword(newPassword: String) {
        if (newPassword.isBlank() || newPassword.length < 4) {
            showSnack("Password must be at least 4 characters")
            return
        }
        val result = BPWalletRepository.updateUserPassword(newPassword)
        result.onSuccess {
            showSnack("Your password was updated successfully!")
        }.onFailure {
            showSnack("Failed to update password: ${it.message}")
        }
    }

    fun updateExchangeWebsiteUrl(url: String) {
        val result = BPWalletRepository.updateExchangeWebsiteUrl(url)
        result.onSuccess {
            showSnack("Exchange URL updated to $it")
        }.onFailure {
            showSnack("Failed to update Exchange URL: ${it.message}")
        }
    }
}
