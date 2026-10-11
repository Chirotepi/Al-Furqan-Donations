package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DonorEntity::class,
        PaymentLedgerEntity::class,
        DonorCounterEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun donorDao(): DonorDao
    abstract fun paymentLedgerDao(): PaymentLedgerDao
    abstract fun donorCounterDao(): DonorCounterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "donations.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial seed data on first create
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val donorDao = donorDao()
        val ledgerDao = paymentLedgerDao()
        val counterDao = donorCounterDao()

        if (donorDao.getDonorCount() > 0) return

        // Set initial counter
        counterDao.saveCounter(DonorCounterEntity(id = 1, lastValue = 8))

        // Seed 8 realistic community donors
        val initialDonors = listOf(
            DonorEntity(id = "MDAF-0001", fullName = "Ibrahim Abdela", phoneNumber = "+1 (555) 234-5678", monthlyPledge = 100.0),
            DonorEntity(id = "MDAF-0002", fullName = "Sheikh Ahmed Noor", phoneNumber = "+1 (555) 345-6789", monthlyPledge = 250.0),
            DonorEntity(id = "MDAF-0003", fullName = "Fatima Al-Zahra", phoneNumber = "+1 (555) 456-7890", monthlyPledge = 75.0),
            DonorEntity(id = "MDAF-0004", fullName = "Mohamed Tariq", phoneNumber = "+1 (555) 567-8901", monthlyPledge = 150.0),
            DonorEntity(id = "MDAF-0005", fullName = "Amina Hassan", phoneNumber = "+1 (555) 678-9012", monthlyPledge = 50.0),
            DonorEntity(id = "MDAF-0006", fullName = "Bilal Mansoor", phoneNumber = "+1 (555) 789-0123", monthlyPledge = 200.0),
            DonorEntity(id = "MDAF-0007", fullName = "Zainab Qasim", phoneNumber = "+1 (555) 890-1234", monthlyPledge = 120.0),
            DonorEntity(id = "MDAF-0008", fullName = "Yusuf Kabbani", phoneNumber = "+1 (555) 901-2345", monthlyPledge = 80.0)
        )
        donorDao.insertDonors(initialDonors)

        // Seed ledger payments for FY 2026 (current fiscal year: Jul=1, Aug=2, Sep=3, Oct=4)
        val sampleEntries = mutableListOf<PaymentLedgerEntity>()
        
        // Month 1 (July)
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0001", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0002", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0003", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0004", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0005", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0006", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0007", fiscalYear = 2026, monthIndex = 1, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0008", fiscalYear = 2026, monthIndex = 1, status = "Not Paid"))

        // Month 2 (August)
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0001", fiscalYear = 2026, monthIndex = 2, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0002", fiscalYear = 2026, monthIndex = 2, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0003", fiscalYear = 2026, monthIndex = 2, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0004", fiscalYear = 2026, monthIndex = 2, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0005", fiscalYear = 2026, monthIndex = 2, status = "Not Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0006", fiscalYear = 2026, monthIndex = 2, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0007", fiscalYear = 2026, monthIndex = 2, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0008", fiscalYear = 2026, monthIndex = 2, status = "Paid"))

        // Month 3 (September)
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0001", fiscalYear = 2026, monthIndex = 3, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0002", fiscalYear = 2026, monthIndex = 3, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0003", fiscalYear = 2026, monthIndex = 3, status = "Not Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0004", fiscalYear = 2026, monthIndex = 3, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0005", fiscalYear = 2026, monthIndex = 3, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0006", fiscalYear = 2026, monthIndex = 3, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0007", fiscalYear = 2026, monthIndex = 3, status = "Not Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0008", fiscalYear = 2026, monthIndex = 3, status = "Not Paid"))

        // Month 4 (October - current)
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0001", fiscalYear = 2026, monthIndex = 4, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0002", fiscalYear = 2026, monthIndex = 4, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0003", fiscalYear = 2026, monthIndex = 4, status = "Paid"))
        sampleEntries.add(PaymentLedgerEntity(donorId = "MDAF-0004", fiscalYear = 2026, monthIndex = 4, status = "Not Paid"))

        ledgerDao.insertLedgers(sampleEntries)
    }
}
