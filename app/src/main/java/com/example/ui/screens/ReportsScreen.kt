package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DashboardSummary
import com.example.data.model.DonorLedgerRow
import com.example.data.model.FiscalCalendar
import com.example.data.model.PaymentStatus
import com.example.ui.theme.StatusNotPaid
import com.example.ui.theme.StatusNotPaidContainer
import com.example.ui.theme.StatusOnNotPaidContainer

@Composable
fun ReportsScreen(
    summary: DashboardSummary,
    donorRows: List<DonorLedgerRow>,
    fiscalYear: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMonthForFollowUp by remember { mutableStateOf(4) } // Default October (Month 4)

    // Unpaid donors in selected month
    val unpaidDonors = remember(donorRows, selectedMonthForFollowUp) {
        donorRows.filter {
            it.monthlyStatuses[selectedMonthForFollowUp] != PaymentStatus.PAID
        }
    }

    val monthName = FiscalCalendar.getMonthName(selectedMonthForFollowUp)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Report Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Al-Furqaan Executive Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${FiscalCalendar.formatFiscalYear(fiscalYear)} (July – June)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Share Summary Button
                        Button(
                            onClick = {
                                val reportText = buildString {
                                    appendLine("🕌 Al-Furqaan Donation Tracker Report")
                                    appendLine("📅 Fiscal Year: ${FiscalCalendar.formatFiscalYear(fiscalYear)}")
                                    appendLine("👥 Total Registered Donors: ${summary.totalDonors}")
                                    appendLine("🎯 Fixed Monthly Target: ${FiscalCalendar.formatCurrency(summary.fixedMonthlyTarget)}")
                                    appendLine("🎯 Yearly Target (12 Mos): ${FiscalCalendar.formatCurrency(summary.yearlyTarget)}")
                                    appendLine("💰 Total Collected: ${FiscalCalendar.formatCurrency(summary.totalCollectedTillNow)}")
                                    appendLine("⏳ Outstanding: ${FiscalCalendar.formatCurrency(summary.outstandingVs12MonthTarget)}")
                                    appendLine("📊 Collection Rate: ${"%.1f".format(summary.collectionRate * 100)}%")
                                    appendLine("✅ Paid Entries: ${summary.totalPaidEntriesCount}")
                                    appendLine("⚠️ Unpaid Entries: ${summary.totalNotPaidEntriesCount}")
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Al-Furqaan FY $fiscalYear Donation Report")
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Donation Report"))
                            },
                            modifier = Modifier.testTag("share_report_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }

                    HorizontalDivider()

                    // Key Summary stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Donors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.totalDonors}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Monthly Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FiscalCalendar.formatCurrency(summary.fixedMonthlyTarget), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Collected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FiscalCalendar.formatCurrency(summary.totalCollectedTillNow), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Collection %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${"%.1f".format(summary.collectionRate * 100)}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Unpaid Donors Follow-Up Roster
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = StatusNotPaid,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Monthly Follow-Up Roster",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select month to view pending donors requiring reminder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Month Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(12) { idx ->
                            val m = idx + 1
                            FilterChip(
                                selected = selectedMonthForFollowUp == m,
                                onClick = { selectedMonthForFollowUp = m },
                                label = { Text(FiscalCalendar.getMonthShort(m)) }
                            )
                        }
                    }

                    // Status line
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusNotPaidContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$monthName: ${unpaidDonors.size} Donors Pending",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusOnNotPaidContainer
                            )
                            val unpaidTotal = unpaidDonors.sumOf { it.donor.monthlyPledge }
                            Text(
                                text = FiscalCalendar.formatCurrency(unpaidTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusNotPaid
                            )
                        }
                    }
                }
            }
        }

        // List of unpaid donors for selected month
        if (unpaidDonors.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 All donors have paid for $monthName!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            items(unpaidDonors, key = { it.donor.id }) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusNotPaidContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = row.donor.fullName.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOnNotPaidContainer
                                )
                            }

                            Column {
                                Text(
                                    text = "${row.donor.fullName} (${row.donor.id})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Pledge: ${FiscalCalendar.formatCurrency(row.donor.monthlyPledge)} • Phone: ${row.donor.phoneNumber ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!row.donor.phoneNumber.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${row.donor.phoneNumber}")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("sms:${row.donor.phoneNumber}")
                                            putExtra(
                                                "sms_body",
                                                "Assalamu Alaikum ${row.donor.fullName}, this is a gentle reminder regarding your $monthName donation pledge of ${FiscalCalendar.formatCurrency(row.donor.monthlyPledge)} for Al-Furqaan. Jazakallahu Khairan."
                                            )
                                        }
                                        context.startActivity(smsIntent)
                                    }
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = "Reminder SMS", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
