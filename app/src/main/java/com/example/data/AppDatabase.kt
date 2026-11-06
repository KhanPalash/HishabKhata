package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    // App Config
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun observeConfig(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfigOnce(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: AppConfigEntity)

    // Accounts
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, createdAt ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, createdAt ASC")
    suspend fun getAccountsOnce(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    // Categories
    @Query("SELECT * FROM categories ORDER BY usageCount DESC, lastUsedAt DESC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY usageCount DESC, lastUsedAt DESC, name ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY isArchived ASC, name ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isArchived ASC, name ASC")
    suspend fun getContactsOnce(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAt DESC")
    suspend fun getTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(txs: List<TransactionEntity>)

    // Journal Entries & Lines
    @Query("SELECT * FROM journal_entries ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY dateEpochDay DESC, createdAt DESC")
    suspend fun getJournalEntriesOnce(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_lines ORDER BY dateEpochDay DESC")
    fun observeJournalLines(): Flow<List<JournalLineEntity>>

    @Query("SELECT * FROM journal_lines ORDER BY dateEpochDay DESC")
    suspend fun getJournalLinesOnce(): List<JournalLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntries(entries: List<JournalEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    // Attachments
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC")
    fun observeAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments ORDER BY createdAt DESC")
    suspend fun getAttachmentsOnce(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: String)

    // Recurring Templates & Runs
    @Query("SELECT * FROM recurring_templates ORDER BY isActive DESC, nextRunEpochDay ASC")
    fun observeRecurringTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates ORDER BY isActive DESC, nextRunEpochDay ASC")
    suspend fun getRecurringTemplatesOnce(): List<RecurringTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringTemplate(template: RecurringTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringTemplates(templates: List<RecurringTemplateEntity>)

    @Query("SELECT * FROM recurring_runs ORDER BY executedAt DESC")
    fun observeRecurringRuns(): Flow<List<RecurringRunEntity>>

    @Query("SELECT * FROM recurring_runs ORDER BY executedAt DESC")
    suspend fun getRecurringRunsOnce(): List<RecurringRunEntity>

    @Query("SELECT * FROM recurring_runs WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getRecurringRunByKey(key: String): RecurringRunEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecurringRun(run: RecurringRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringRuns(runs: List<RecurringRunEntity>)

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY dueEpochDay ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY dueEpochDay ASC")
    suspend fun getRemindersOnce(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<ReminderEntity>)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAuditLogsOnce(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    // Backup History
    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    fun observeBackupHistory(): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    suspend fun getBackupHistoryOnce(): List<BackupRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackupRecord(record: BackupRecordEntity)

    @Query("DELETE FROM backup_history WHERE id = :id")
    suspend fun deleteBackupRecordById(id: String)

    // Full Wipe for Restore
    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournalEntries()

    @Query("DELETE FROM journal_lines")
    suspend fun clearJournalLines()

    @Query("DELETE FROM attachments")
    suspend fun clearAttachments()

    @Query("DELETE FROM recurring_templates")
    suspend fun clearRecurringTemplates()

    @Query("DELETE FROM recurring_runs")
    suspend fun clearRecurringRuns()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}

@Database(
    entities = [
        AppConfigEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        ContactEntity::class,
        TransactionEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        AttachmentEntity::class,
        RecurringTemplateEntity::class,
        RecurringRunEntity::class,
        ReminderEntity::class,
        AuditLogEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hishabkhata_ledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
