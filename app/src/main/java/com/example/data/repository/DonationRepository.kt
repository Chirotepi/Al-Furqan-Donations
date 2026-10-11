package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.DonorCounterEntity
import com.example.data.local.DonorEntity
import com.example.data.local.PaymentLedgerEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.Donor
import com.example.data.model.DonorLedgerRow
import com.example.data.model.PaymentStatus
import com.example.data.remote.ApiClient
import com.example.data.remote.RemoteDonorCreate
import com.example.data.remote.RemoteStatusUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class DonationRepository(
    private val database: AppDatabase
) {
    private val donorDao = database.donorDao()
    private val ledgerDao = database.paymentLedgerDao()
    private val counterDao = database.donorCounterDao()

    val allDonors: Flow<List<DonorEntity>> = donorDao.getAllDonors()

    fun searchDonors(query: String): Flow<List<DonorEntity>> {
        return if (query.isBlank()) {
            allDonors
        } else {
            donorDao.searchDonors(query.trim())
        }
    }

    fun getLedgerForFiscalYear(fiscalYear: Int): Flow<List<PaymentLedgerEntity>> {
        return ledgerDao.getLedgerForFiscalYear(fiscalYear)
    }

    fun getLedgerForDonor(donorId: String): Flow<List<PaymentLedgerEntity>> {
        return ledgerDao.getLedgerForDonor(donorId)
    }

    /**
     * Combines donors with fiscal year ledger records to provide a reactive, full 12-month row view.
     */
    fun getDonorLedgerRows(fiscalYear: Int): Flow<List<DonorLedgerRow>> {
        return combine(allDonors, getLedgerForFiscalYear(fiscalYear)) { donors, entries ->
            val entriesByDonor = entries.groupBy { it.donorId }

            donors.map { donorEntity ->
                val donorEntries = entriesByDonor[donorEntity.id].orEmpty()
                val monthlyStatuses = mutableMapOf<Int, String>()
                var paidMonths = 0

                for (month in 1..12) {
                    val entry = donorEntries.find { it.monthIndex == month }
                    if (entry != null) {
                        monthlyStatuses[month] = entry.status
                        if (entry.status == PaymentStatus.PAID) {
                            paidMonths++
                        }
                    } else {
                        // Unrecorded defaults to Not Paid
                        monthlyStatuses[month] = PaymentStatus.NOT_PAID
                    }
                }

                val totalPaidInYear = paidMonths * donorEntity.monthlyPledge
                val domainDonor = Donor(
                    id = donorEntity.id,
                    fullName = donorEntity.fullName,
                    phoneNumber = donorEntity.phoneNumber,
                    monthlyPledge = donorEntity.monthlyPledge,
                    totalCollectedLifetime = 0.0
                )

                DonorLedgerRow(
                    donor = domainDonor,
                    monthlyStatuses = monthlyStatuses,
                    totalPaidInFiscalYear = totalPaidInYear,
                    paidMonthsCountInYear = paidMonths
                )
            }
        }
    }

    /**
     * Atomically generates sequential ID "MDAF-XXXX" using the counter table
     * exactly as in the FastAPI create_donor implementation.
     */
    suspend fun createDonor(
        fullName: String,
        phoneNumber: String?,
        monthlyPledge: Double
    ): DonorEntity = withContext(Dispatchers.IO) {
        val currentCounter = counterDao.getCounter() ?: DonorCounterEntity(id = 1, lastValue = 0)
        val nextValue = currentCounter.lastValue + 1
        counterDao.saveCounter(currentCounter.copy(lastValue = nextValue))

        val formattedId = "MDAF-${nextValue.toString().padStart(4, '0')}"
        val newDonor = DonorEntity(
            id = formattedId,
            fullName = fullName.trim(),
            phoneNumber = phoneNumber?.trim()?.ifEmpty { null },
            monthlyPledge = monthlyPledge
        )
        donorDao.insertDonor(newDonor)
        newDonor
    }

    suspend fun getNextDonorIdPreview(): String = withContext(Dispatchers.IO) {
        val currentCounter = counterDao.getCounter() ?: DonorCounterEntity(id = 1, lastValue = 0)
        "MDAF-${(currentCounter.lastValue + 1).toString().padStart(4, '0')}"
    }

    suspend fun updateDonor(donor: DonorEntity) = withContext(Dispatchers.IO) {
        donorDao.updateDonor(donor)
    }

    suspend fun deleteDonor(donorId: String) = withContext(Dispatchers.IO) {
        val donor = donorDao.getDonorById(donorId)
        if (donor != null) {
            ledgerDao.deleteLedgerForDonor(donorId)
            donorDao.deleteDonor(donor)
        }
    }

    /**
     * Updates payment status for a specific month and fiscal year.
     * Matches the FastAPI PATCH /api/v1/ledger/status logic.
     */
    suspend fun updatePaymentStatus(
        donorId: String,
        fiscalYear: Int,
        monthIndex: Int,
        status: String
    ): Double = withContext(Dispatchers.IO) {
        val existing = ledgerDao.getEntry(donorId, fiscalYear, monthIndex)
        if (existing != null) {
            ledgerDao.insertLedger(existing.copy(status = status))
        } else {
            ledgerDao.insertLedger(
                PaymentLedgerEntity(
                    donorId = donorId,
                    fiscalYear = fiscalYear,
                    monthIndex = monthIndex,
                    status = status
                )
            )
        }

        // Return total collected from donor across all fiscal years
        val donor = donorDao.getDonorById(donorId)
        val paidMonths = ledgerDao.countPaidMonthsForDonor(donorId)
        (donor?.monthlyPledge ?: 0.0) * paidMonths
    }

    suspend fun togglePaymentStatus(
        donorId: String,
        fiscalYear: Int,
        monthIndex: Int
    ): String = withContext(Dispatchers.IO) {
        val existing = ledgerDao.getEntry(donorId, fiscalYear, monthIndex)
        val newStatus = if (existing?.status == PaymentStatus.PAID) {
            PaymentStatus.NOT_PAID
        } else {
            PaymentStatus.PAID
        }
        updatePaymentStatus(donorId, fiscalYear, monthIndex, newStatus)
        newStatus
    }

    suspend fun getLifetimeCollectedForDonor(donorId: String): Double = withContext(Dispatchers.IO) {
        val donor = donorDao.getDonorById(donorId) ?: return@withContext 0.0
        val paidCount = ledgerDao.countPaidMonthsForDonor(donorId)
        paidCount * donor.monthlyPledge
    }

    /**
     * Aggregates donor and payment data into real-time KPI summary.
     * Uses in-memory donors map to avoid N+1 queries, exactly mirroring FastAPI GET /api/v1/dashboard/summary.
     */
    fun calculateDashboardSummary(
        donors: List<DonorEntity>,
        ledgerEntries: List<PaymentLedgerEntity>,
        fiscalYear: Int
    ): DashboardSummary {
        val totalDonors = donors.size
        val donorsMap = donors.associateBy { it.id }

        val fixedMonthlyTarget = donors.sumOf { it.monthlyPledge }
        val yearlyTarget = fixedMonthlyTarget * 12.0

        val paidEntriesForYear = ledgerEntries.filter {
            it.fiscalYear == fiscalYear && it.status == PaymentStatus.PAID
        }

        val totalCollected = paidEntriesForYear.sumOf { entry ->
            donorsMap[entry.donorId]?.monthlyPledge ?: 0.0
        }

        val totalNotPaidCount = ledgerEntries.count {
            it.fiscalYear == fiscalYear && it.status == PaymentStatus.NOT_PAID
        }

        val outstanding = maxOf(0.0, yearlyTarget - totalCollected)
        val collectionRate = if (yearlyTarget > 0.0) totalCollected / yearlyTarget else 0.0

        return DashboardSummary(
            fiscalYear = fiscalYear,
            totalDonors = totalDonors,
            fixedMonthlyTarget = fixedMonthlyTarget,
            yearlyTarget = yearlyTarget,
            totalCollectedTillNow = totalCollected,
            outstandingVs12MonthTarget = outstanding,
            collectionRate = collectionRate,
            totalPaidEntriesCount = paidEntriesForYear.size,
            totalNotPaidEntriesCount = totalNotPaidCount
        )
    }

    /**
     * Sync with remote FastAPI server
     */
    suspend fun testServerConnection(baseUrl: String, fiscalYear: Int): Result<DashboardSummary> =
        withContext(Dispatchers.IO) {
            try {
                val service = ApiClient.createService(baseUrl)
                val summary = service.getDashboardSummary(fiscalYear)
                Result.success(
                    DashboardSummary(
                        fiscalYear = summary.fiscalYear,
                        totalDonors = summary.totalDonors,
                        fixedMonthlyTarget = summary.fixedMonthlyTarget,
                        yearlyTarget = summary.yearlyTarget,
                        totalCollectedTillNow = summary.totalCollectedTillNow,
                        outstandingVs12MonthTarget = summary.outstandingVs12MonthTarget,
                        collectionRate = summary.collectionRate,
                        totalPaidEntriesCount = summary.totalPaidEntriesCount,
                        totalNotPaidEntriesCount = summary.totalNotPaidEntriesCount
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun syncDonorsFromRemote(baseUrl: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val service = ApiClient.createService(baseUrl)
                val remoteDonors = service.listDonors()
                val entities = remoteDonors.map {
                    DonorEntity(
                        id = it.id,
                        fullName = it.fullName,
                        phoneNumber = it.phoneNumber,
                        monthlyPledge = it.monthlyPledge
                    )
                }
                donorDao.insertDonors(entities)
                Result.success(entities.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun pushDonorToRemote(baseUrl: String, donor: DonorEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val service = ApiClient.createService(baseUrl)
                service.createDonor(
                    RemoteDonorCreate(
                        fullName = donor.fullName,
                        phoneNumber = donor.phoneNumber,
                        monthlyPledge = donor.monthlyPledge
                    )
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun pushStatusToRemote(
        baseUrl: String,
        donorId: String,
        fiscalYear: Int,
        monthIndex: Int,
        status: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = ApiClient.createService(baseUrl)
            service.updatePaymentStatus(
                RemoteStatusUpdate(
                    donorId = donorId,
                    fiscalYear = fiscalYear,
                    monthIndex = monthIndex,
                    status = status
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
