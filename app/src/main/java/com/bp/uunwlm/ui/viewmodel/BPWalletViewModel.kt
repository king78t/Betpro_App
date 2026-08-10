package com.bp.uunwlm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bp.uunwlm.data.BPWalletRepository
import com.bp.uunwlm.model.CelebrationEvent
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenType {
    SPLASH,
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    RESET_PASSWORD,
    VERIFY_EMAIL,
    USER_HOME,
    USER_DEPOSIT,
    USER_WITHDRAW,
    USER_HISTORY,
    USER_PROFILE,
    ADMIN_DASHBOARD,
    ADMIN_USERS_CRM,
    ADMIN_TRANSACTIONS,
    ADMIN_DEPOSITS,
    ADMIN_WITHDRAWALS,
    ADMIN_BANK_MANAGEMENT,
    ADMIN_NOTIFICATIONS,
    ADMIN_AUDIT_LOGS,
    ADMIN_LIVE_CONTROL,
    ADMIN_SETTINGS
}

class BPWalletViewModel : ViewModel() {

    val currentUser = BPWalletRepository.currentUser
    val allUsers = BPWalletRepository.usersList
    val allTransactions = BPWalletRepository.transactionsList
    val paymentGateways = BPWalletRepository.paymentGateways
    val recentBroadcast = BPWalletRepository.recentBroadcast
    val whatsappHelplineNumber = BPWalletRepository.whatsappHelplineNumber
    val exchangeWebsiteUrl = BPWalletRepository.exchangeWebsiteUrl
    val isLoading = BPWalletRepository.isLoading
    val auditLogs = BPWalletRepository.auditLogs
    val appSettings = BPWalletRepository.appSettings
    val adminNotifications = BPWalletRepository.adminNotificationsList

    private val _isBiometricEnabled = MutableStateFlow(BPWalletRepository.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        BPWalletRepository.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    private val _isBiometricAuthenticated = MutableStateFlow(false)
    val isBiometricAuthenticated: StateFlow<Boolean> = _isBiometricAuthenticated.asStateFlow()

    private val _lastActivityTime = MutableStateFlow(System.currentTimeMillis())
    val lastActivityTime: StateFlow<Long> = _lastActivityTime.asStateFlow()

    fun recordUserActivity() {
        _lastActivityTime.value = System.currentTimeMillis()
    }

    fun setBiometricAuthenticated(authenticated: Boolean) {
        _isBiometricAuthenticated.value = authenticated
        if (authenticated) recordUserActivity()
    }

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
            allUsers.collect { users ->
                val normalUsers = users.filter { !it.isSuperAdmin && it.role != "admin" }
                android.util.Log.i("BPWalletVM", "Users Update: Total=${users.size}, Normal (Displayed)=${normalUsers.size}")
                users.forEach { u ->
                    android.util.Log.d("BPWalletVM", "  - User: ${u.fullName} (ID: ${u.id}, Role: ${u.role}, Admin: ${u.isSuperAdmin})")
                }
            }
        }
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
               screen == ScreenType.ADMIN_TRANSACTIONS ||
               screen == ScreenType.ADMIN_DEPOSITS ||
               screen == ScreenType.ADMIN_WITHDRAWALS ||
               screen == ScreenType.ADMIN_BANK_MANAGEMENT ||
               screen == ScreenType.ADMIN_NOTIFICATIONS ||
               screen == ScreenType.ADMIN_AUDIT_LOGS ||
               screen == ScreenType.ADMIN_SETTINGS
    }

    // Login tab mode: true = User, false = Admin
    private val _isUserLoginTab = MutableStateFlow(true)
    val isUserLoginTab: StateFlow<Boolean> = _isUserLoginTab.asStateFlow()

    // Snackbar / Alert message
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    private val _uiEvent = kotlinx.coroutines.channels.Channel<com.bp.uunwlm.util.UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun sendUiEvent(event: com.bp.uunwlm.util.UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    // Shake animation trigger for failed login attempts / visual feedback
    private val _errorShakeTrigger = MutableStateFlow(0)
    val errorShakeTrigger: StateFlow<Int> = _errorShakeTrigger.asStateFlow()

    fun triggerErrorShake() {
        _errorShakeTrigger.value += 1
    }

    // Celebratory Lottie Animation Event State
    private val _celebrationEvent = MutableStateFlow<CelebrationEvent?>(null)
    val celebrationEvent: StateFlow<CelebrationEvent?> = _celebrationEvent.asStateFlow()

    fun triggerCelebration(event: CelebrationEvent) {
        _celebrationEvent.value = event
    }

    fun clearCelebration() {
        _celebrationEvent.value = null
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

    // Transaction for Deposit Proof Screenshot Verification modal
    private val _txForProofVerification = MutableStateFlow<TransactionRequest?>(null)
    val txForProofVerification: StateFlow<TransactionRequest?> = _txForProofVerification.asStateFlow()

    // Dialog state for BetPro Exchange Info modal
    private val _showBetProExchangeModal = MutableStateFlow(false)
    val showBetProExchangeModal: StateFlow<Boolean> = _showBetProExchangeModal.asStateFlow()

    private val _pendingVerificationEmail = MutableStateFlow("")
    val pendingVerificationEmail: StateFlow<String> = _pendingVerificationEmail.asStateFlow()

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
        sendUiEvent(com.bp.uunwlm.util.UiEvent.ShowSnackbar(message))
    }

    fun showToast(message: String, isLong: Boolean = false) {
        sendUiEvent(com.bp.uunwlm.util.UiEvent.ShowToast(message, isLong))
    }

    private fun <T> handleResult(
        result: Result<T>,
        onSuccess: (T) -> Unit,
        onError: ((Throwable) -> Unit)? = null
    ) {
        result.onSuccess {
            onSuccess(it)
        }.onFailure { err ->
            if (err.message == "PENDING_VERIFICATION") {
                setScreen(ScreenType.VERIFY_EMAIL)
                return@onFailure
            }
            val message = com.bp.uunwlm.util.ErrorHandler.getErrorMessage(err)
            showSnack(message)
            onError?.invoke(err)
        }
    }

    fun loginUser(username: String, pass: String) {
        if (username.isBlank() || pass.isBlank()) {
            triggerErrorShake()
            showSnack("Please enter username and password")
            return
        }
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.loginUser(username, pass)
            BPWalletRepository.setLoading(false)
            result.onSuccess { user ->
                recordUserActivity()
                showSnack("Welcome Back, ${user.fullName}!")
                if (user.isAdminRole) {
                    setScreen(ScreenType.ADMIN_DASHBOARD)
                } else {
                    setScreen(ScreenType.USER_HOME)
                }
            }.onFailure { err ->
                if (err.message == "PENDING_VERIFICATION") {
                    val userEmail = BPWalletRepository.usersList.value.find { it.username.equals(username, true) }?.email ?: ""
                    _pendingVerificationEmail.value = userEmail
                    setScreen(ScreenType.VERIFY_EMAIL)
                    return@onFailure
                }
                val message = com.bp.uunwlm.util.ErrorHandler.getErrorMessage(err)
                showSnack(message)
                triggerErrorShake()
            }
        }
    }

    fun loginAdmin(username: String, pass: String) {
        if (username.isBlank() || pass.isBlank()) {
            triggerErrorShake()
            showSnack("Please enter admin username and password")
            return
        }
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.loginAdmin(username, pass)
            BPWalletRepository.setLoading(false)
            handleResult<UserAccount>(result, onSuccess = { user ->
                recordUserActivity()
                showSnack("Admin logged in successfully! Welcome ${user.fullName}")
                if (user.isAdminRole) {
                    setScreen(ScreenType.ADMIN_DASHBOARD)
                } else {
                    setScreen(ScreenType.USER_HOME)
                }
            }, onError = {
                triggerErrorShake()
            })
        }
    }

    fun registerUser(
        fullName: String,
        email: String,
        currency: String,
        mobileNumber: String,
        pass: String,
        username: String
    ) {
        if (fullName.isBlank() || email.isBlank() || mobileNumber.isBlank() || pass.isBlank() || username.isBlank()) {
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
        if (pass.length < 4) {
            triggerErrorShake()
            showSnack("Password must be at least 4 characters long.")
            return
        }
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.registerUser(
                fullName = fullName,
                email = trimmedEmail,
                currency = currency,
                mobileNumber = mobileNumber,
                password = pass,
                username = username
            )
            BPWalletRepository.setLoading(false)
            handleResult<UserAccount>(result, onSuccess = {
                _pendingVerificationEmail.value = trimmedEmail
                showSnack("Verification code sent! Please check your email.")
                setScreen(ScreenType.VERIFY_EMAIL)
            }, onError = {
                triggerErrorShake()
            })
        }
    }

    fun verifyOtp(email: String, otp: String) {
        if (otp.length < 6) {
            showSnack("Please enter the 6-digit code.")
            return
        }
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.verifyOtp(email, otp)
            BPWalletRepository.setLoading(false)
            handleResult(result, onSuccess = {
                showSnack("Email verified successfully! You can now login.")
                setScreen(ScreenType.LOGIN)
            })
        }
    }

    fun resendVerification(email: String) {
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.resendVerification(email)
            BPWalletRepository.setLoading(false)
            handleResult(result, onSuccess = {
                showSnack("New verification code sent!")
            })
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            showSnack("Please enter a valid email")
            return
        }
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.forgotPassword(email)
            BPWalletRepository.setLoading(false)
            handleResult(result, onSuccess = {
                showSnack("Password reset email sent! Check your inbox.")
                setScreen(ScreenType.LOGIN)
            })
        }
    }

    fun resetPassword(newPass: String) {
        if (newPass.length < 4) {
            showSnack("Password must be at least 4 characters")
            return
        }
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            val result = BPWalletRepository.resetPassword(newPass)
            BPWalletRepository.setLoading(false)
            handleResult(result, onSuccess = {
                showSnack("Password updated! You can now login.")
                setScreen(ScreenType.LOGIN)
            })
        }
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
        if (screenshotUri.isBlank()) {
            showSnack("Please attach payment screenshot proof!")
            return
        }

        val refToSave = if (reference.isBlank()) "PROOF_UPLOADED" else reference
        
        viewModelScope.launch {
            BPWalletRepository.setLoading(true)
            // 1. Upload screenshot to Supabase Storage
            val publicUrl = BPWalletRepository.uploadProofScreenshot(screenshotUri)
            
            // 2. Create the deposit request with the public URL
            val result = BPWalletRepository.createDepositRequest(amount, gw.name, refToSave, publicUrl)
            BPWalletRepository.setLoading(false)
            
            handleResult(result, onSuccess = {
                showSnack("Deposit Request of $curr ${amount.toInt()} submitted! Awaiting Admin approval.")
                _selectedDepositGateway.value = null
                triggerCelebration(
                    CelebrationEvent(
                        title = "Deposit Submitted! 🎉",
                        subtitle = "Your deposit request of $curr ${amount.toInt()} via ${gw.name} has been received and sent for verification.",
                        amount = amount,
                        currency = curr,
                        transactionType = "DEPOSIT",
                        referenceOrDetails = "Ref: $refToSave"
                    )
                )
                setScreen(ScreenType.USER_HOME)
            })
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
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.createWithdrawalRequest(amount, gatewayName, accountTitle, accountNumberOrIban)
        BPWalletRepository.setLoading(false)
        val curr = currentUser.value?.currency ?: "PKR"
        handleResult(result, onSuccess = {
            showSnack("Withdrawal Request of $curr ${amount.toInt()} submitted successfully!")
            triggerCelebration(
                CelebrationEvent(
                    title = "Withdrawal Requested! 💰",
                    subtitle = "Your payout request of $curr ${amount.toInt()} to $accountTitle ($gatewayName) is processing.",
                    amount = amount,
                    currency = curr,
                    transactionType = "WITHDRAW",
                    referenceOrDetails = "$gatewayName • $accountNumberOrIban"
                )
            )
            setScreen(ScreenType.USER_HOME)
        })
    }

    // Admin Actions
    fun approveTransaction(txId: String, notes: String = "") {
        BPWalletRepository.setLoading(true)
        val res = BPWalletRepository.approveTransaction(txId, notes)
        BPWalletRepository.setLoading(false)
        handleResult(res, onSuccess = { tx ->
            showSnack("${tx.type} approved successfully!")
            triggerCelebration(
                CelebrationEvent(
                    title = "${tx.type} Approved! ✨",
                    subtitle = "Transaction of ${tx.currency} ${tx.amount.toInt()} has been successfully approved and completed.",
                    amount = tx.amount,
                    currency = tx.currency,
                    transactionType = tx.type,
                    referenceOrDetails = "Transaction ID: ${tx.id}"
                )
            )
        })
    }

    fun rejectTransaction(txId: String, notes: String = "") {
        BPWalletRepository.setLoading(true)
        val res = BPWalletRepository.rejectTransaction(txId, notes)
        BPWalletRepository.setLoading(false)
        handleResult(res, onSuccess = { tx ->
            showSnack("${tx.type} rejected.")
        })
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
        BPWalletRepository.setLoading(true)
        val res = BPWalletRepository.addPaymentGateway(
            name, currency, country, title, accountNumber, iban, bankName, instructions, shortDescription, logoUrl, isEnabled, displayOrder, minDeposit, minWithdraw
        )
        BPWalletRepository.setLoading(false)
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
        BPWalletRepository.setLoading(true)
        val res = BPWalletRepository.updatePaymentGateway(
            id, name, currency, country, title, accountNumber, iban, bankName, instructions, shortDescription, logoUrl, isEnabled, displayOrder, minDeposit, minWithdraw
        )
        BPWalletRepository.setLoading(false)
        res.onSuccess {
            showSnack("Payment Method updated successfully!")
        }.onFailure { err ->
            showSnack(err.message ?: "Failed to update payment method")
        }
    }

    fun togglePaymentGatewayStatus(id: String) {
        BPWalletRepository.setLoading(true)
        val res = BPWalletRepository.togglePaymentGatewayStatus(id)
        BPWalletRepository.setLoading(false)
        res.onSuccess { gw ->
            val statusStr = if (gw.isEnabled) "Enabled" else "Disabled"
            showSnack("${gw.name} is now $statusStr")
        }.onFailure { err ->
            showSnack(err.message ?: "Failed to toggle status")
        }
    }

    fun deletePaymentGateway(gatewayId: String) {
        BPWalletRepository.setLoading(true)
        val res = BPWalletRepository.deletePaymentGateway(gatewayId)
        BPWalletRepository.setLoading(false)
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
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.updateBetProCredentials(userId, username, pass, status)
        BPWalletRepository.setLoading(false)
        handleResult(result, onSuccess = {
            showSnack("User account updated & user notified!")
            _selectedUserForCreds.value = null
        })
    }

    fun setCrmCurrencyFilter(currency: String) {
        _crmCurrencyFilter.value = currency
    }

    fun setCrmStatusFilter(status: String) {
        _crmStatusFilter.value = status
    }

    fun resetCrmFilters() {
        _crmSearchQuery.value = ""
        _crmCurrencyFilter.value = "All Curr"
        _crmStatusFilter.value = "All Status"
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

    fun broadcastPushAlert(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) {
            showSnack("Please enter announcement title and message")
            return
        }
        BPWalletRepository.broadcastPushAlert(title, message)
        showSnack("Push alert broadcasted to all active wallets!")
    }

    fun sendAdminNotification(title: String, message: String, type: String) {
        if (title.isBlank() || message.isBlank()) {
            showSnack("Please enter title and message")
            return
        }
        val notification = com.bp.uunwlm.model.AdminNotification(
            title = title,
            message = message,
            type = type
        )
        BPWalletRepository.sendAdminNotification(notification)
        showSnack("Notification sent successfully!")
    }

    fun saveAppSettings(settings: com.bp.uunwlm.model.AppSettings) {
        BPWalletRepository.saveAppSettings(settings)
        showSnack("App settings updated successfully!")
    }

    fun deleteUser(userId: String) {
        val res = BPWalletRepository.deleteUser(userId)
        handleResult(res, onSuccess = {
            showSnack("User account deleted permanently.")
        })
    }

    fun updateUserStatus(userId: String, status: String) {
        val res = BPWalletRepository.updateUserStatus(userId, status)
        handleResult(res, onSuccess = {
            showSnack("User status updated to $status.")
        })
    }

    fun setBetProExchangeModalVisible(visible: Boolean) {
        _showBetProExchangeModal.value = visible
    }

    // Quick seed demo deposit for testing
    /*
    fun seedDemoDepositRequest() {
        BPWalletRepository.createDepositRequest(1500.0, "EasyPaisa", "EPX-99881122")
        showSnack("Demo Deposit request created!")
    }
    */

    fun updateAdminPassword(newPassword: String) {
        if (newPassword.isBlank() || newPassword.length < 4) {
            showSnack("Password must be at least 4 characters")
            return
        }
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.updateAdminPassword(newPassword)
        BPWalletRepository.setLoading(false)
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
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.updateWhatsAppHelpline(number)
        BPWalletRepository.setLoading(false)
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
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.updateUserProfile(fullName, mobileNumber)
        BPWalletRepository.setLoading(false)
        handleResult(result, onSuccess = {
            showSnack("Profile details updated successfully!")
        })
    }

    fun updateUserPassword(newPassword: String) {
        if (newPassword.isBlank() || newPassword.length < 4) {
            showSnack("Password must be at least 4 characters")
            return
        }
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.updateUserPassword(newPassword)
        BPWalletRepository.setLoading(false)
        handleResult(result, onSuccess = {
            showSnack("Your password was updated successfully!")
        })
    }

    fun verifySecurityStorage(): String {
        return BPWalletRepository.verifyTokenEncryption()
    }

    fun updateExchangeWebsiteUrl(url: String) {
        BPWalletRepository.setLoading(true)
        val result = BPWalletRepository.updateExchangeWebsiteUrl(url)
        BPWalletRepository.setLoading(false)
        result.onSuccess {
            showSnack("Exchange URL updated to $it")
        }.onFailure {
            showSnack("Failed to update Exchange URL: ${it.message}")
        }
    }

    fun openWhatsAppSupport(context: android.content.Context) {
        val user = currentUser.value ?: return
        val number = whatsappHelplineNumber.value
        if (number.isBlank()) {
            showSnack("Support number not configured")
            return
        }
        
        val message = "Hello Support, I need help with my BP Wallet account.\n\nAccount Details:\n- Name: ${user.fullName}\n- User ID: ${user.id}\n- Country: ${user.country}\n\nPlease assist me."
        val url = "https://api.whatsapp.com/send?phone=$number&text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            showSnack("Could not open WhatsApp. Please make sure it's installed.")
        }
    }
}
