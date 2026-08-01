package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BPWalletRepository
import com.example.model.MasterAgent
import com.example.model.PaymentGateway
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class ScreenType {
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
    ADMIN_TRANSACTIONS
}

class BPWalletViewModel : ViewModel() {

    val currentUser = BPWalletRepository.currentUser
    val allUsers = BPWalletRepository.usersList
    val allTransactions = BPWalletRepository.transactionsList
    val masterAgents = BPWalletRepository.masterAgentsList
    val paymentGateways = BPWalletRepository.paymentGateways
    val recentBroadcast = BPWalletRepository.recentBroadcast

    private val _currentScreen = MutableStateFlow(ScreenType.LOGIN)
    val currentScreen: StateFlow<ScreenType> = _currentScreen.asStateFlow()

    // Login tab mode: true = User, false = Admin
    private val _isUserLoginTab = MutableStateFlow(true)
    val isUserLoginTab: StateFlow<Boolean> = _isUserLoginTab.asStateFlow()

    // Snackbar / Alert message
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

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

    // Dialog state for BetPro Exchange Info modal
    private val _showBetProExchangeModal = MutableStateFlow(false)
    val showBetProExchangeModal: StateFlow<Boolean> = _showBetProExchangeModal.asStateFlow()

    // Dialog state for Create Master Agent modal
    private val _showCreateMasterModal = MutableStateFlow(false)
    val showCreateMasterModal: StateFlow<Boolean> = _showCreateMasterModal.asStateFlow()

    fun setScreen(screen: ScreenType) {
        _currentScreen.value = screen
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
        val result = BPWalletRepository.loginUser(emailOrPhone, pass)
        result.onSuccess { user ->
            showSnack("Welcome Back, ${user.fullName}!")
            setScreen(ScreenType.USER_HOME)
        }.onFailure { err ->
            showSnack(err.message ?: "Login failed")
        }
    }

    fun loginAdmin(username: String, pass: String) {
        val result = BPWalletRepository.loginAdmin(username, pass)
        result.onSuccess { admin ->
            showSnack("Admin logged in successfully! Welcome ${admin.fullName}")
            setScreen(ScreenType.ADMIN_DASHBOARD)
        }.onFailure { err ->
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
            showSnack("Please fill in all required fields.")
            return
        }
        viewModelScope.launch {
            val result = BPWalletRepository.registerUser(fullName, email, currency, mobileNumber, pass)
            result.onSuccess { user ->
                showSnack("Success! Account created in $currency with Mobile $mobileNumber")
                setScreen(ScreenType.USER_HOME)
            }.onFailure { err ->
                showSnack("Registration error: ${err.message}")
            }
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

    fun submitDepositRequest(amount: Double, reference: String) {
        val gw = _selectedDepositGateway.value
        if (gw == null) {
            showSnack("Please select a payment gateway first")
            return
        }
        if (amount < gw.minDeposit) {
            showSnack("Minimum deposit for ${gw.name} is Rs ${gw.minDeposit.toInt()}")
            return
        }
        val result = BPWalletRepository.createDepositRequest(amount, gw.name, reference)
        result.onSuccess {
            showSnack("Deposit Request of Rs ${amount.toInt()} submitted! Awaiting Admin approval.")
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
    fun approveTransaction(txId: String) {
        val res = BPWalletRepository.approveTransaction(txId)
        res.onSuccess { tx ->
            showSnack("${tx.type} approved successfully!")
        }.onFailure { err ->
            showSnack(err.message ?: "Approval failed.")
        }
    }

    fun rejectTransaction(txId: String) {
        val res = BPWalletRepository.rejectTransaction(txId)
        res.onSuccess { tx ->
            showSnack("${tx.type} rejected.")
        }.onFailure { err ->
            showSnack(err.message ?: "Rejection failed.")
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
}
