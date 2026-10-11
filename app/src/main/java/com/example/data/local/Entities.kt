package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "donors"
)
data class DonorEntity(
    @PrimaryKey
    val id: String, // e.g. "MDAF-0001"
    val fullName: String,
    val phoneNumber: String? = null,
    val monthlyPledge: Double = 0.0
)

@Entity(
    tableName = "payment_ledger",
    indices = [
        Index(value = ["donorId", "fiscalYear", "monthIndex"], unique = true),
        Index(value = ["fiscalYear", "status"])
    ]
)
data class PaymentLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val donorId: String,
    val fiscalYear: Int,
    val monthIndex: Int, // 1 (July) to 12 (June)
    val status: String   // "Paid" or "Not Paid"
)

@Entity(
    tableName = "donor_id_counter"
)
data class DonorCounterEntity(
    @PrimaryKey
    val id: Int = 1,
    val lastValue: Int = 0
)
