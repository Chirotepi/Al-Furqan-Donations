package com.example

import com.example.data.local.DonorEntity
import com.example.data.local.PaymentLedgerEntity
import com.example.data.model.FiscalCalendar
import com.example.data.model.PaymentStatus
import com.example.data.repository.DonationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun fiscalCalendar_monthsMapping_isCorrect() {
        assertEquals("July", FiscalCalendar.getMonthName(1))
        assertEquals("Jul", FiscalCalendar.getMonthShort(1))
        assertEquals("December", FiscalCalendar.getMonthName(6))
        assertEquals("January", FiscalCalendar.getMonthName(7))
        assertEquals("June", FiscalCalendar.getMonthName(12))
        assertEquals("Jun", FiscalCalendar.getMonthShort(12))
    }

    @Test
    fun donorIdFormatting_matchesPrefixAndPadding() {
        val counter1 = 1
        val id1 = "MDAF-${counter1.toString().padStart(4, '0')}"
        assertEquals("MDAF-0001", id1)

        val counter42 = 42
        val id42 = "MDAF-${counter42.toString().padStart(4, '0')}"
        assertEquals("MDAF-0042", id42)
    }

    @Test
    fun dashboardCalculations_matchFastApiFormulas() {
        val donors = listOf(
            DonorEntity(id = "MDAF-0001", fullName = "Donor A", monthlyPledge = 100.0),
            DonorEntity(id = "MDAF-0002", fullName = "Donor B", monthlyPledge = 200.0)
        )

        val entries = listOf(
            PaymentLedgerEntity(donorId = "MDAF-0001", fiscalYear = 2026, monthIndex = 1, status = PaymentStatus.PAID),
            PaymentLedgerEntity(donorId = "MDAF-0001", fiscalYear = 2026, monthIndex = 2, status = PaymentStatus.PAID),
            PaymentLedgerEntity(donorId = "MDAF-0002", fiscalYear = 2026, monthIndex = 1, status = PaymentStatus.PAID),
            PaymentLedgerEntity(donorId = "MDAF-0002", fiscalYear = 2026, monthIndex = 2, status = PaymentStatus.NOT_PAID)
        )

        val donorsMap = donors.associateBy { it.id }
        val fixedMonthlyTarget = donors.sumOf { it.monthlyPledge }
        assertEquals(300.0, fixedMonthlyTarget, 0.001)

        val yearlyTarget = fixedMonthlyTarget * 12.0
        assertEquals(3600.0, yearlyTarget, 0.001)

        val paidEntries = entries.filter { it.fiscalYear == 2026 && it.status == PaymentStatus.PAID }
        assertEquals(3, paidEntries.size)

        val totalCollected = paidEntries.sumOf { donorsMap[it.donorId]?.monthlyPledge ?: 0.0 }
        // Donor A paid 2 months ($200) + Donor B paid 1 month ($200) = $400
        assertEquals(400.0, totalCollected, 0.001)

        val outstanding = maxOf(0.0, yearlyTarget - totalCollected)
        assertEquals(3200.0, outstanding, 0.001)

        val collectionRate = totalCollected / yearlyTarget
        assertEquals(400.0 / 3600.0, collectionRate, 0.001)
    }

    @Test
    fun donorSearch_caseInsensitiveMatching_isCorrect() {
        val donors = listOf(
            DonorEntity(id = "MDAF-0001", fullName = "Ibrahim Abdela", monthlyPledge = 100.0),
            DonorEntity(id = "MDAF-0002", fullName = "Sheikh Ahmed", monthlyPledge = 200.0)
        )

        val query = "ibrahim"
        val matched = donors.filter {
            it.fullName.lowercase().contains(query) || it.id.lowercase().contains(query)
        }
        assertEquals(1, matched.size)
        assertEquals("MDAF-0001", matched.first().id)

        val idQuery = "mdaf-0002"
        val matchedId = donors.filter {
            it.fullName.lowercase().contains(idQuery) || it.id.lowercase().contains(idQuery)
        }
        assertEquals(1, matchedId.size)
        assertEquals("Sheikh Ahmed", matchedId.first().fullName)
    }
}
