package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.DonorEntity
import com.example.data.model.FiscalCalendar

enum class DonorSortOrder {
    BY_ID,
    BY_NAME,
    BY_PLEDGE_DESC
}

@Composable
fun DonorsScreen(
    donors: List<DonorEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectDonor: (DonorEntity) -> Unit,
    onOpenAddDonor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortOrder by remember { mutableStateOf(DonorSortOrder.BY_ID) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Case-insensitive search on full_name or id (matching GET /api/v1/donors/search?q=...)
    val filteredDonors = remember(donors, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            donors
        } else {
            val q = searchQuery.trim().lowercase()
            donors.filter {
                it.fullName.lowercase().contains(q) ||
                    it.id.lowercase().contains(q) ||
                    (it.phoneNumber?.lowercase()?.contains(q) == true)
            }
        }

        when (sortOrder) {
            DonorSortOrder.BY_ID -> filtered.sortedBy { it.id }
            DonorSortOrder.BY_NAME -> filtered.sortedBy { it.fullName.lowercase() }
            DonorSortOrder.BY_PLEDGE_DESC -> filtered.sortedByDescending { it.monthlyPledge }
        }
    }

    val totalPledges = remember(donors) {
        donors.sumOf { it.monthlyPledge }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar (matches /api/v1/donors/search?q=)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by donor name or ID (MDAF-...)...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_donor_search_btn")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donor_search_input")
            )

            // Directory Stats & Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${filteredDonors.size} of ${donors.size} Donors Registered",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Monthly Pledges: ${FiscalCalendar.formatCurrency(totalPledges)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    TextButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("sort_donors_btn")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortOrder) {
                                DonorSortOrder.BY_ID -> "ID Order"
                                DonorSortOrder.BY_NAME -> "Name (A-Z)"
                                DonorSortOrder.BY_PLEDGE_DESC -> "Highest Pledge"
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("By Sequential ID (MDAF-...)") },
                            onClick = {
                                sortOrder = DonorSortOrder.BY_ID
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Donor Name (A-Z)") },
                            onClick = {
                                sortOrder = DonorSortOrder.BY_NAME
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Monthly Pledge (Highest first)") },
                            onClick = {
                                sortOrder = DonorSortOrder.BY_PLEDGE_DESC
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Donors List
            if (filteredDonors.isEmpty()) {
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No donors found for '$searchQuery'" else "No donors registered yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredDonors, key = { it.id }) { donor ->
                        DonorListCard(
                            donor = donor,
                            onClick = { onSelectDonor(donor) },
                            onCall = {
                                if (!donor.phoneNumber.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${donor.phoneNumber}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Register Donor
        FloatingActionButton(
            onClick = onOpenAddDonor,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_donor")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Register Donor")
        }
    }
}

@Composable
fun DonorListCard(
    donor: DonorEntity,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("donor_card_${donor.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = donor.fullName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = donor.id,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = donor.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!donor.phoneNumber.isNullOrBlank()) {
                        Text(
                            text = donor.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pledge amount pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "${FiscalCalendar.formatCurrency(donor.monthlyPledge)}/mo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                if (!donor.phoneNumber.isNullOrBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("call_donor_${donor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Donor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
