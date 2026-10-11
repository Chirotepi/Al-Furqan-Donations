package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DonorEntity
import com.example.data.model.DonorLedgerRow
import com.example.data.model.FiscalCalendar
import com.example.data.model.PaymentStatus
import com.example.ui.theme.StatusNotPaid
import com.example.ui.theme.StatusNotPaidContainer
import com.example.ui.theme.StatusOnNotPaidContainer
import com.example.ui.theme.StatusOnPaidContainer
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidContainer

@Composable
fun LedgerScreen(
    donorRows: List<DonorLedgerRow>,
    fiscalYear: Int,
    selectedMonthFilter: Int?, // null = all months, 1..12 = July to June
    searchQuery: String,
    statusFilter: String?, // null = all, "Paid", "Not Paid"
    onSelectMonth: (Int?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onTogglePaymentStatus: (donorId: String, monthIndex: Int) -> Unit,
    onSelectDonor: (DonorEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter donor rows based on search query (case-insensitive search by full_name or id)
    val filteredRows = remember(donorRows, searchQuery, statusFilter, selectedMonthFilter) {
        donorRows.filter { row ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                row.donor.fullName.lowercase().contains(q) ||
                    row.donor.id.lowercase().contains(q) ||
                    (row.donor.phoneNumber?.lowercase()?.contains(q) == true)
            }

            val matchesStatus = if (statusFilter == null) {
                true
            } else if (selectedMonthFilter != null) {
                val status = row.monthlyStatuses[selectedMonthFilter] ?: PaymentStatus.NOT_PAID
                status == statusFilter
            } else {
                // If all months, check if any month matches or if all paid/not paid
                if (statusFilter == PaymentStatus.PAID) {
                    row.paidMonthsCountInYear > 0
                } else {
                    row.paidMonthsCountInYear < 12
                }
            }

            matchesSearch && matchesStatus
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input (matching GET /api/v1/donors/search?q=...)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search donor by name or ID (e.g., MDAF-0001)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_btn")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ledger_search_input")
        )

        // Month Selection horizontal carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedMonthFilter == null,
                    onClick = { onSelectMonth(null) },
                    label = { Text("All 12 Months") },
                    modifier = Modifier.testTag("month_filter_all")
                )
            }

            items(12) { index ->
                val monthIndex = index + 1
                val monthShort = FiscalCalendar.getMonthShort(monthIndex)
                FilterChip(
                    selected = selectedMonthFilter == monthIndex,
                    onClick = { onSelectMonth(monthIndex) },
                    label = { Text("M$monthIndex: $monthShort") },
                    modifier = Modifier.testTag("month_filter_$monthIndex")
                )
            }
        }

        // Status Filter Chips (All, Paid, Not Paid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("filter_status_all")
            )

            FilterChip(
                selected = statusFilter == PaymentStatus.PAID,
                onClick = { onStatusFilterChange(if (statusFilter == PaymentStatus.PAID) null else PaymentStatus.PAID) },
                label = { Text("Paid Only") },
                leadingIcon = {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusPaidContainer,
                    selectedLabelColor = StatusOnPaidContainer
                ),
                modifier = Modifier.testTag("filter_status_paid")
            )

            FilterChip(
                selected = statusFilter == PaymentStatus.NOT_PAID,
                onClick = { onStatusFilterChange(if (statusFilter == PaymentStatus.NOT_PAID) null else PaymentStatus.NOT_PAID) },
                label = { Text("Unpaid Only") },
                leadingIcon = {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusNotPaidContainer,
                    selectedLabelColor = StatusOnNotPaidContainer
                ),
                modifier = Modifier.testTag("filter_status_unpaid")
            )
        }

        // Header info: Results count & Fiscal Year
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredRows.size} donors found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val monthLabel = if (selectedMonthFilter != null) {
                "${FiscalCalendar.getMonthName(selectedMonthFilter)} (M$selectedMonthFilter)"
            } else {
                "Full Year View"
            }
            Text(
                text = "$monthLabel • FY $fiscalYear",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Main Ledger List
        if (filteredRows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No donors matching '$searchQuery'" else "No donors recorded yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredRows, key = { it.donor.id }) { row ->
                    LedgerDonorCard(
                        row = row,
                        fiscalYear = fiscalYear,
                        selectedMonthFilter = selectedMonthFilter,
                        onToggleMonth = { month -> onTogglePaymentStatus(row.donor.id, month) },
                        onClickDonor = {
                            onSelectDonor(
                                DonorEntity(
                                    id = row.donor.id,
                                    fullName = row.donor.fullName,
                                    phoneNumber = row.donor.phoneNumber,
                                    monthlyPledge = row.donor.monthlyPledge
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerDonorCard(
    row: DonorLedgerRow,
    fiscalYear: Int,
    selectedMonthFilter: Int?,
    onToggleMonth: (monthIndex: Int) -> Unit,
    onClickDonor: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_card_${row.donor.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Donor Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickDonor() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = row.donor.fullName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = row.donor.id,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!row.donor.phoneNumber.isNullOrBlank()) {
                                Text(
                                    text = "• ${row.donor.phoneNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = row.donor.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Pledge & Paid summary badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${FiscalCalendar.formatCurrency(row.donor.monthlyPledge)}/mo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "FY: ${FiscalCalendar.formatCurrency(row.totalPaidInFiscalYear)} (${row.paidMonthsCountInYear}/12)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Payment Action Area
            if (selectedMonthFilter != null) {
                // Single month selected: show a big prominent quick toggle button
                val status = row.monthlyStatuses[selectedMonthFilter] ?: PaymentStatus.NOT_PAID
                val isPaid = status == PaymentStatus.PAID
                val monthName = FiscalCalendar.getMonthName(selectedMonthFilter)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPaid) StatusPaidContainer else StatusNotPaidContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isPaid) StatusPaid else StatusNotPaid.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleMonth(selectedMonthFilter) }
                        .testTag("toggle_status_btn_${row.donor.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isPaid) StatusPaid else StatusNotPaid
                            )
                            Column {
                                Text(
                                    text = "$monthName Status: ${if (isPaid) "PAID" else "NOT PAID"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaid) StatusOnPaidContainer else StatusOnNotPaidContainer
                                )
                                Text(
                                    text = if (isPaid) "${FiscalCalendar.formatCurrency(row.donor.monthlyPledge)} recorded" else "Tap to mark as Paid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPaid) StatusOnPaidContainer.copy(alpha = 0.8f) else StatusOnNotPaidContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = { onToggleMonth(selectedMonthFilter) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaid) StatusPaid else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isPaid) "Paid ✓" else "Mark Paid")
                        }
                    }
                }
            } else {
                // All months selected: show a compact horizontal ribbon of 12 clickable months
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (month in 1..12) {
                        val status = row.monthlyStatuses[month] ?: PaymentStatus.NOT_PAID
                        val isPaid = status == PaymentStatus.PAID
                        val monthShort = FiscalCalendar.getMonthShort(month)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPaid) StatusPaidContainer else StatusNotPaidContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isPaid) StatusPaid else StatusNotPaid.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onToggleMonth(month) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = monthShort,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaid) StatusOnPaidContainer else StatusOnNotPaidContainer
                                )
                                Text(
                                    text = if (isPaid) "✓" else "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPaid) StatusPaid else StatusNotPaid
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
