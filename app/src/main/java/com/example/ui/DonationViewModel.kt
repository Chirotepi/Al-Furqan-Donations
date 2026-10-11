package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DonorEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.DonorLedgerRow
import com.example.data.model.FiscalCalendar
import com.example.data.model.PaymentStatus
import com.example.data.repository.DonationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DonationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = DonationRepository(database)

    // Current fiscal year selection (default 2026, corresponding to July 2026 - June 2027)
    private val _selectedFiscalYear = MutableStateFlow(2026)
    val selectedFiscalYear: StateFlow<Int> = _selectedFiscalYear.asStateFlow()

    // Month filter for Ledger (null = all 12 months, 1..12 = July..June)
    private val _selectedMonthFilter = MutableStateFlow<Int?>(null)
    val selectedMonthFilter: StateFlow<Int?> = _selectedMonthFilter.asStateFlow()

    // Status filter: null (All), PaymentStatus.PAID, or PaymentStatus.NOT_PAID
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    // Search query for donor name, ID, or phone
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active donor for details sheet/dialog
    private val _selectedDonorForDetails = MutableStateFlow<DonorEntity?>(null)
    val selectedDonorForDetails: StateFlow<DonorEntity?> = _selectedDonorForDetails.asStateFlow()

    // Next predicted donor ID for add donor preview
    private val _nextDonorIdPreview = MutableStateFlow("MDAF-0009")
    val nextDonorIdPreview: StateFlow<String> = _nextDonorIdPreview.asStateFlow()

    // Server configuration state
    private val _serverUrl = MutableStateFlow("http://10.0.2.2:8000")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _serverStatusMessage = MutableStateFlow<String?>(null)
    val serverStatusMessage: StateFlow<String?> = _serverStatusMessage.asStateFlow()

    private val _isServerLoading = MutableStateFlow(false)
    val isServerLoading: StateFlow<Boolean> = _isServerLoading.asStateFlow()

    // Snackbar notification event
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    // Reactive list of all donors
    val donorsList: StateFlow<List<DonorEntity>> = repository.allDonors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Real-time Dashboard KPI summary calculated reactively
    val dashboardSummary: StateFlow<DashboardSummary> = _selectedFiscalYear
        .flatMapLatest { fy ->
            combine(repository.allDonors, repository.getLedgerForFiscalYear(fy)) { donors, ledger ->
                repository.calculateDashboardSummary(donors, ledger, fy)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardSummary(
                fiscalYear = 2026,
                totalDonors = 0,
                fixedMonthlyTarget = 0.0,
                yearlyTarget = 0.0,
                totalCollectedTillNow = 0.0,
                outstandingVs12MonthTarget = 0.0,
                collectionRate = 0.0,
                totalPaidEntriesCount = 0,
                totalNotPaidEntriesCount = 0
            )
        )

    // Reactive donor ledger rows for current fiscal year
    val donorLedgerRows: StateFlow<List<DonorLedgerRow>> = _selectedFiscalYear
        .flatMapLatest { fy ->
            repository.getDonorLedgerRows(fy)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshNextDonorIdPreview()
        // Ensure initial seed if database is first created
        viewModelScope.launch {
            database.seedInitialData()
            refreshNextDonorIdPreview()
        }
    }

    fun setFiscalYear(year: Int) {
        _selectedFiscalYear.value = year
    }

    fun setMonthFilter(month: Int?) {
        _selectedMonthFilter.value = month
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectDonorForDetails(donor: DonorEntity?) {
        _selectedDonorForDetails.value = donor
    }

    fun clearNotificationMessage() {
        _notificationMessage.value = null
    }

    fun refreshNextDonorIdPreview() {
        viewModelScope.launch {
            _nextDonorIdPreview.value = repository.getNextDonorIdPreview()
        }
    }

    fun togglePaymentStatus(donorId: String, monthIndex: Int) {
        viewModelScope.launch {
            val fy = _selectedFiscalYear.value
            val newStatus = repository.togglePaymentStatus(donorId, fy, monthIndex)
            val monthName = FiscalCalendar.getMonthShort(monthIndex)
            _notificationMessage.value = "$donorId $monthName status updated to $newStatus"
        }
    }

    fun setPaymentStatus(donorId: String, monthIndex: Int, status: String) {
        viewModelScope.launch {
            val fy = _selectedFiscalYear.value
            val totalCollected = repository.updatePaymentStatus(donorId, fy, monthIndex, status)
            val monthName = FiscalCalendar.getMonthName(monthIndex)
            _notificationMessage.value = "$monthName set to $status. Total collected: ${FiscalCalendar.formatCurrency(totalCollected)}"
        }
    }

    fun createDonor(fullName: String, phoneNumber: String?, monthlyPledge: Double, onCreated: ((DonorEntity) -> Unit)? = null) {
        viewModelScope.launch {
            if (fullName.isBlank()) {
                _notificationMessage.value = "Donor name cannot be empty."
                return@launch
            }
            val newDonor = repository.createDonor(fullName, phoneNumber, monthlyPledge)
            refreshNextDonorIdPreview()
            _notificationMessage.value = "Registered donor ${newDonor.fullName} (${newDonor.id})"
            onCreated?.invoke(newDonor)
        }
    }

    fun updateDonor(donor: DonorEntity) {
        viewModelScope.launch {
            repository.updateDonor(donor)
            _selectedDonorForDetails.value = donor
            _notificationMessage.value = "Updated profile for ${donor.fullName}"
        }
    }

    fun deleteDonor(donorId: String) {
        viewModelScope.launch {
            repository.deleteDonor(donorId)
            _selectedDonorForDetails.value = null
            refreshNextDonorIdPreview()
            _notificationMessage.value = "Removed donor profile $donorId"
        }
    }

    fun setServerUrl(url: String) {
        _serverUrl.value = url.trim()
    }

    fun testServerConnection() {
        viewModelScope.launch {
            _isServerLoading.value = true
            _serverStatusMessage.value = "Connecting to FastAPI backend..."
            val result = repository.testServerConnection(_serverUrl.value, _selectedFiscalYear.value)
            _isServerLoading.value = false
            result.onSuccess { summary ->
                _serverStatusMessage.value = "Connected! Remote FY ${summary.fiscalYear}: ${summary.totalDonors} donors, ${FiscalCalendar.formatCurrency(summary.totalCollectedTillNow)} collected."
            }.onFailure { error ->
                _serverStatusMessage.value = "Connection failed: ${error.localizedMessage ?: "Unable to reach server"}"
            }
        }
    }

    fun syncFromRemote() {
        viewModelScope.launch {
            _isServerLoading.value = true
            _serverStatusMessage.value = "Syncing donors from ${_serverUrl.value}..."
            val result = repository.syncDonorsFromRemote(_serverUrl.value)
            _isServerLoading.value = false
            result.onSuccess { count ->
                _serverStatusMessage.value = "Successfully synced $count donors from remote backend!"
                _notificationMessage.value = "Synced $count donors from FastAPI"
                refreshNextDonorIdPreview()
            }.onFailure { error ->
                _serverStatusMessage.value = "Sync error: ${error.localizedMessage}"
            }
        }
    }
}
