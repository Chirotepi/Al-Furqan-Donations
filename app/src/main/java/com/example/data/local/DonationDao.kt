package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DonorDao {
    @Query("SELECT * FROM donors ORDER BY id ASC")
    fun getAllDonors(): Flow<List<DonorEntity>>

    @Query("SELECT * FROM donors ORDER BY id ASC")
    suspend fun getAllDonorsList(): List<DonorEntity>

    @Query("SELECT * FROM donors WHERE id = :id LIMIT 1")
    suspend fun getDonorById(id: String): DonorEntity?

    @Query("SELECT * FROM donors WHERE lower(fullName) LIKE '%' || lower(:query) || '%' OR lower(id) LIKE '%' || lower(:query) || '%' ORDER BY id ASC")
    fun searchDonors(query: String): Flow<List<DonorEntity>>

    @Query("SELECT * FROM donors WHERE lower(fullName) LIKE '%' || lower(:query) || '%' OR lower(id) LIKE '%' || lower(:query) || '%' ORDER BY id ASC")
    suspend fun searchDonorsList(query: String): List<DonorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonor(donor: DonorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonors(donors: List<DonorEntity>)

    @Update
    suspend fun updateDonor(donor: DonorEntity)

    @Delete
    suspend fun deleteDonor(donor: DonorEntity)

    @Query("SELECT COUNT(*) FROM donors")
    suspend fun getDonorCount(): Int
}

@Dao
interface PaymentLedgerDao {
    @Query("SELECT * FROM payment_ledger WHERE fiscalYear = :fiscalYear ORDER BY monthIndex ASC")
    fun getLedgerForFiscalYear(fiscalYear: Int): Flow<List<PaymentLedgerEntity>>

    @Query("SELECT * FROM payment_ledger WHERE fiscalYear = :fiscalYear")
    suspend fun getLedgerForFiscalYearList(fiscalYear: Int): List<PaymentLedgerEntity>

    @Query("SELECT * FROM payment_ledger WHERE donorId = :donorId ORDER BY fiscalYear DESC, monthIndex ASC")
    fun getLedgerForDonor(donorId: String): Flow<List<PaymentLedgerEntity>>

    @Query("SELECT * FROM payment_ledger WHERE donorId = :donorId")
    suspend fun getLedgerListForDonor(donorId: String): List<PaymentLedgerEntity>

    @Query("SELECT * FROM payment_ledger WHERE donorId = :donorId AND fiscalYear = :fiscalYear AND monthIndex = :monthIndex LIMIT 1")
    suspend fun getEntry(donorId: String, fiscalYear: Int, monthIndex: Int): PaymentLedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(entry: PaymentLedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgers(entries: List<PaymentLedgerEntity>)

    @Query("SELECT COUNT(*) FROM payment_ledger WHERE donorId = :donorId AND status = 'Paid'")
    suspend fun countPaidMonthsForDonor(donorId: String): Int

    @Query("SELECT COUNT(*) FROM payment_ledger WHERE fiscalYear = :fiscalYear AND status = 'Paid'")
    suspend fun countPaidForFiscalYear(fiscalYear: Int): Int

    @Query("SELECT COUNT(*) FROM payment_ledger WHERE fiscalYear = :fiscalYear AND status = 'Not Paid'")
    suspend fun countNotPaidForFiscalYear(fiscalYear: Int): Int

    @Query("DELETE FROM payment_ledger WHERE donorId = :donorId")
    suspend fun deleteLedgerForDonor(donorId: String)
}

@Dao
interface DonorCounterDao {
    @Query("SELECT * FROM donor_id_counter WHERE id = 1 LIMIT 1")
    suspend fun getCounter(): DonorCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCounter(counter: DonorCounterEntity)
}
