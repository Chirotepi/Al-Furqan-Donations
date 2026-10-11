package com.example.data.model

import com.squareup.moshi.JsonClass
import java.text.NumberFormat
import java.util.Locale

/**
 * Al-Furqaan Fiscal Calendar Helper.
 * Fiscal Year runs from Month 1 (July) to Month 12 (June).
 */
object FiscalCalendar {
    val MONTH_NAMES = listOf(
        "July", "August", "September", "October",
        "November", "December", "January", "February",
        "March", "April", "May", "June"
    )

    val MONTH_SHORT = listOf(
        "Jul", "Aug", "Sep", "Oct",
        "Nov", "Dec", "Jan", "Feb",
        "Mar", "Apr", "May", "Jun"
    )

    fun getMonthName(monthIndex: Int): String {
        return if (monthIndex in 1..12) MONTH_NAMES[monthIndex - 1] else "Month $monthIndex"
    }

    fun getMonthShort(monthIndex: Int): String {
        return if (monthIndex in 1..12) MONTH_SHORT[monthIndex - 1] else "M$monthIndex"
    }

    fun formatFiscalYear(fiscalYear: Int): String {
        return "FY $fiscalYear–${(fiscalYear + 1) % 100}"
    }

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }

    fun availableFiscalYears(): List<Int> {
        return listOf(2026, 2025, 2024, 2023, 2019)
    }
}

/**
 * KPI summary matching the FastAPI /api/v1/dashboard/summary response.
 */
@JsonClass(generateAdapter = true)
data class DashboardSummary(
    val fiscalYear: Int,
    val totalDonors: Int,
    val fixedMonthlyTarget: Double,
    val yearlyTarget: Double,
    val totalCollectedTillNow: Double,
    val outstandingVs12MonthTarget: Double,
    val collectionRate: Double,
    val totalPaidEntriesCount: Int,
    val totalNotPaidEntriesCount: Int
)

/**
 * Status values defined in the FastAPI specification: "Paid" or "Not Paid"
 */
object PaymentStatus {
    const val PAID = "Paid"
    const val NOT_PAID = "Not Paid"
}

/**
 * Domain representation of a registered donor.
 */
data class Donor(
    val id: String, // e.g., "MDAF-0001"
    val fullName: String,
    val phoneNumber: String? = null,
    val monthlyPledge: Double = 0.0,
    val totalCollectedLifetime: Double = 0.0
)

/**
 * Domain model for a payment ledger entry.
 */
data class LedgerEntry(
    val id: Long = 0,
    val donorId: String,
    val fiscalYear: Int,
    val monthIndex: Int,
    val status: String
)

/**
 * Consolidated donor ledger view with payment statuses for each of the 12 fiscal months.
 */
data class DonorLedgerRow(
    val donor: Donor,
    val monthlyStatuses: Map<Int, String>, // monthIndex (1..12) -> "Paid" / "Not Paid"
    val totalPaidInFiscalYear: Double,
    val paidMonthsCountInYear: Int
)
