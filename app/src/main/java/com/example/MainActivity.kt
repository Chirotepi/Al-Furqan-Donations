package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FiscalCalendar
import com.example.ui.DonationViewModel
import com.example.ui.components.AddDonorDialog
import com.example.ui.components.DonorDetailSheet
import com.example.ui.components.ServerSyncDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DonorsScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppNavScreen {
    DASHBOARD,
    LEDGER,
    DONORS,
    REPORTS
}

class MainActivity : ComponentActivity() {

    private val viewModel: DonationViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppNavScreen.DASHBOARD) }
                var showAddDonorDialog by remember { mutableStateOf(false) }
                var showServerSyncDialog by remember { mutableStateOf(false) }

                val snackbarHostState = remember { SnackbarHostState() }

                val fiscalYear by viewModel.selectedFiscalYear.collectAsStateWithLifecycle()
                val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
                val donorRows by viewModel.donorLedgerRows.collectAsStateWithLifecycle()
                val donorsList by viewModel.donorsList.collectAsStateWithLifecycle()
                val selectedMonthFilter by viewModel.selectedMonthFilter.collectAsStateWithLifecycle()
                val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val selectedDonorForDetails by viewModel.selectedDonorForDetails.collectAsStateWithLifecycle()
                val nextDonorIdPreview by viewModel.nextDonorIdPreview.collectAsStateWithLifecycle()
                val notificationMessage by viewModel.notificationMessage.collectAsStateWithLifecycle()
                val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
                val serverStatusMessage by viewModel.serverStatusMessage.collectAsStateWithLifecycle()
                val isServerLoading by viewModel.isServerLoading.collectAsStateWithLifecycle()

                // Show notification in snackbar
                LaunchedEffect(notificationMessage) {
                    notificationMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearNotificationMessage()
                    }
                }

                // Handle back press: if on secondary screen, go back to Dashboard
                BackHandler(enabled = currentScreen != AppNavScreen.DASHBOARD) {
                    currentScreen = AppNavScreen.DASHBOARD
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "Al-Furqaan Donations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showServerSyncDialog = true },
                                    modifier = Modifier.testTag("topbar_server_sync_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "FastAPI Sync",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.refreshNextDonorIdPreview()
                                        showAddDonorDialog = true
                                    },
                                    modifier = Modifier.testTag("topbar_add_donor_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Register Donor",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_bottom_nav"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.DASHBOARD,
                                onClick = { currentScreen = AppNavScreen.DASHBOARD },
                                icon = {
                                    Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                                },
                                label = { Text("Dashboard") },
                                modifier = Modifier.testTag("nav_item_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.LEDGER,
                                onClick = { currentScreen = AppNavScreen.LEDGER },
                                icon = {
                                    Icon(Icons.Default.TableChart, contentDescription = "Ledger")
                                },
                                label = { Text("Ledger") },
                                modifier = Modifier.testTag("nav_item_ledger")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.DONORS,
                                onClick = { currentScreen = AppNavScreen.DONORS },
                                icon = {
                                    Icon(Icons.Default.Group, contentDescription = "Donors")
                                },
                                label = { Text("Donors") },
                                modifier = Modifier.testTag("nav_item_donors")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.REPORTS,
                                onClick = { currentScreen = AppNavScreen.REPORTS },
                                icon = {
                                    Icon(Icons.Default.Assessment, contentDescription = "Reports")
                                },
                                label = { Text("Reports") },
                                modifier = Modifier.testTag("nav_item_reports")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppNavScreen.DASHBOARD -> {
                                DashboardScreen(
                                    summary = summary,
                                    selectedFiscalYear = fiscalYear,
                                    donorRows = donorRows,
                                    onSelectFiscalYear = { viewModel.setFiscalYear(it) },
                                    onNavigateToLedger = { currentScreen = AppNavScreen.LEDGER },
                                    onNavigateToDonors = { currentScreen = AppNavScreen.DONORS },
                                    onOpenAddDonor = {
                                        viewModel.refreshNextDonorIdPreview()
                                        showAddDonorDialog = true
                                    }
                                )
                            }

                            AppNavScreen.LEDGER -> {
                                LedgerScreen(
                                    donorRows = donorRows,
                                    fiscalYear = fiscalYear,
                                    selectedMonthFilter = selectedMonthFilter,
                                    searchQuery = searchQuery,
                                    statusFilter = statusFilter,
                                    onSelectMonth = { viewModel.setMonthFilter(it) },
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onStatusFilterChange = { viewModel.setStatusFilter(it) },
                                    onTogglePaymentStatus = { donorId, month ->
                                        viewModel.togglePaymentStatus(donorId, month)
                                    },
                                    onSelectDonor = { donor ->
                                        viewModel.selectDonorForDetails(donor)
                                    }
                                )
                            }

                            AppNavScreen.DONORS -> {
                                DonorsScreen(
                                    donors = donorsList,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onSelectDonor = { donor ->
                                        viewModel.selectDonorForDetails(donor)
                                    },
                                    onOpenAddDonor = {
                                        viewModel.refreshNextDonorIdPreview()
                                        showAddDonorDialog = true
                                    }
                                )
                            }

                            AppNavScreen.REPORTS -> {
                                ReportsScreen(
                                    summary = summary,
                                    donorRows = donorRows,
                                    fiscalYear = fiscalYear
                                )
                            }
                        }

                        // Add Donor Dialog
                        if (showAddDonorDialog) {
                            AddDonorDialog(
                                nextIdPreview = nextDonorIdPreview,
                                onDismiss = { showAddDonorDialog = false },
                                onConfirm = { name, phone, pledge ->
                                    viewModel.createDonor(name, phone, pledge)
                                    showAddDonorDialog = false
                                }
                            )
                        }

                        // Donor Detail Sheet
                        selectedDonorForDetails?.let { donor ->
                            val donorRow = donorRows.find { it.donor.id == donor.id }
                            val monthlyStatuses = donorRow?.monthlyStatuses ?: emptyMap()

                            DonorDetailSheet(
                                donor = donor,
                                fiscalYear = fiscalYear,
                                monthlyStatuses = monthlyStatuses,
                                onToggleMonth = { monthIndex ->
                                    viewModel.togglePaymentStatus(donor.id, monthIndex)
                                },
                                onUpdateDonor = { updated ->
                                    viewModel.updateDonor(updated)
                                },
                                onDeleteDonor = { donorId ->
                                    viewModel.deleteDonor(donorId)
                                },
                                onDismiss = {
                                    viewModel.selectDonorForDetails(null)
                                }
                            )
                        }

                        // Server Sync Dialog
                        if (showServerSyncDialog) {
                            ServerSyncDialog(
                                currentUrl = serverUrl,
                                statusMessage = serverStatusMessage,
                                isLoading = isServerLoading,
                                onSaveUrl = { viewModel.setServerUrl(it) },
                                onTestConnection = { viewModel.testServerConnection() },
                                onSyncDonors = { viewModel.syncFromRemote() },
                                onDismiss = { showServerSyncDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
