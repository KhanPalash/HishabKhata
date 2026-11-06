package com.example.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.domain.AccountingEngine
import com.example.util.ExportAndBackupManager
import com.example.util.Formatters
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LedgerRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val dao = db.ledgerDao()

    val configFlow: Flow<AppConfigEntity?> = dao.observeConfig()
    val accountsFlow: Flow<List<AccountEntity>> = dao.observeAccounts()
    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeCategories()
    val contactsFlow: Flow<List<ContactEntity>> = dao.observeContacts()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val journalEntriesFlow: Flow<List<JournalEntryEntity>> = dao.observeJournalEntries()
    val journalLinesFlow: Flow<List<JournalLineEntity>> = dao.observeJournalLines()
    val attachmentsFlow: Flow<List<AttachmentEntity>> = dao.observeAttachments()
    val recurringTemplatesFlow: Flow<List<RecurringTemplateEntity>> = dao.observeRecurringTemplates()
    val recurringRunsFlow: Flow<List<RecurringRunEntity>> = dao.observeRecurringRuns()
    val remindersFlow: Flow<List<ReminderEntity>> = dao.observeReminders()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    val backupHistoryFlow: Flow<List<BackupRecordEntity>> = dao.observeBackupHistory()

    suspend fun ensureSeededDefaults() {
        val currentConfig = dao.getConfigOnce()
        if (currentConfig == null) {
            dao.upsertConfig(AppConfigEntity())
        }
        val existingCats = dao.getCategoriesOnce()
        if (existingCats.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Product Sales", nameBn = "পণ্য বিক্রয়", categoryType = "INCOME", iconName = "Storefront", colorHex = "#10B981", usageCount = 10),
                CategoryEntity(name = "Service Revenue", nameBn = "সেবা আয়", categoryType = "INCOME", iconName = "Handyman", colorHex = "#059669", usageCount = 6),
                CategoryEntity(name = "Wholesale Order", nameBn = "পাইকারি অর্ডার", categoryType = "INCOME", iconName = "LocalShipping", colorHex = "#0D9488", usageCount = 4),
                CategoryEntity(name = "Delivery & Commission", nameBn = "ডেলিভারি ও কমিশন", categoryType = "INCOME", iconName = "Payments", colorHex = "#14B8A6", usageCount = 2),
                CategoryEntity(name = "Other Income", nameBn = "অন্যান্য আয়", categoryType = "INCOME", iconName = "Savings", colorHex = "#34D399", usageCount = 1),
                CategoryEntity(name = "Stock / Merchandise", nameBn = "মাল ক্রয় / স্টক", categoryType = "EXPENSE", iconName = "Inventory", colorHex = "#EF4444", usageCount = 10),
                CategoryEntity(name = "Shop Rent", nameBn = "দোকান ভাড়া", categoryType = "EXPENSE", iconName = "HomeWork", colorHex = "#F97316", usageCount = 8),
                CategoryEntity(name = "Staff Wages & Salary", nameBn = "কর্মচারী বেতন", categoryType = "EXPENSE", iconName = "Groups", colorHex = "#F59E0B", usageCount = 7),
                CategoryEntity(name = "Electricity, Internet & Bills", nameBn = "বিদ্যুৎ, ইন্টারনেট ও বিল", categoryType = "EXPENSE", iconName = "Bolt", colorHex = "#8B5CF6", usageCount = 6),
                CategoryEntity(name = "Transport & Rickshaw/Van", nameBn = "পরিবহন ও ভাড়া", categoryType = "EXPENSE", iconName = "DirectionsBus", colorHex = "#EC4899", usageCount = 5),
                CategoryEntity(name = "Tea, Hospitality & Misc", nameBn = "আপ্যায়ন ও বিবিধ", categoryType = "EXPENSE", iconName = "LocalCafe", colorHex = "#6366F1", usageCount = 3),
                CategoryEntity(name = "Loan Interest & Bank Fee", nameBn = "ঋণের সুদ ও ব্যাংক চার্জ", categoryType = "EXPENSE", iconName = "AccountBalance", colorHex = "#64748B", usageCount = 1)
            )
            dao.upsertCategories(defaultCategories)
        }
    }

    suspend fun updateConfig(transform: (AppConfigEntity) -> AppConfigEntity) {
        val current = dao.getConfigOnce() ?: AppConfigEntity()
        dao.upsertConfig(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * FR-01 Onboarding: Creates owner & business profile, sets PIN/biometric, creates initial financial account(s),
     * and posts Opening Balance against Opening Equity/Capital (never income!).
     */
    suspend fun completeOnboarding(
        language: String,
        ownerName: String,
        ownerPhone: String,
        businessName: String,
        businessType: String,
        businessAddress: String,
        businessContact: String,
        numberFormat: String,
        dateFormat: String,
        firstAccountName: String,
        firstAccountNameBn: String,
        firstAccountType: String,
        firstAccountNumber: String,
        openingBalancePaisa: Long,
        openingDateEpochDay: Long,
        rawPin: String,
        pinHint: String,
        biometricEnabled: Boolean,
        autoBackupEnabled: Boolean,
        seedSampleContacts: Boolean = true
    ) {
        db.withTransaction {
            val accountId = UUID.randomUUID().toString()
            val firstAccount = AccountEntity(
                id = accountId,
                name = firstAccountName.trim(),
                nameBn = firstAccountNameBn.trim().ifBlank { firstAccountName.trim() },
                accountType = firstAccountType,
                accountNumber = firstAccountNumber.trim(),
                openingBalancePaisa = openingBalancePaisa,
                openingDateEpochDay = openingDateEpochDay
            )
            dao.upsertAccount(firstAccount)

            // Post Opening Balance transaction & journal (even if 0, recorded in audit trail; if > 0 posts to OPENING_EQUITY)
            val openingTx = TransactionEntity(
                txType = "OPENING_BALANCE",
                amountPaisa = openingBalancePaisa,
                settledAmountPaisa = openingBalancePaisa,
                dueAmountPaisa = 0L,
                accountId = accountId,
                dateEpochDay = openingDateEpochDay,
                title = "Opening Balance (${firstAccount.name})",
                referenceCode = "OPEN-EQ-001",
                internalNote = "Posted to Opening Equity / Capital per Cash-Basis Rule 10"
            )
            dao.upsertTransaction(openingTx)

            val journalBundle = AccountingEngine.createJournalForTransaction(
                tx = openingTx,
                entryType = "OPENING",
                memoPrefix = "Opening Balance"
            )
            dao.insertJournalEntry(journalBundle.entry)
            dao.insertJournalLines(journalBundle.lines)

            dao.insertAuditLog(
                AuditLogEntity(
                    transactionId = openingTx.id,
                    action = "OPENING_EQUITY",
                    beforeSnapshotJson = "{}",
                    afterSnapshotJson = snapshotToJson(openingTx),
                    reason = "Initial account setup during onboarding",
                    actorName = ownerName.trim().ifBlank { "Owner" }
                )
            )

            if (seedSampleContacts && dao.getContactsOnce().isEmpty()) {
                dao.upsertContacts(
                    listOf(
                        ContactEntity(
                            name = "Rahim Traders (Customer)",
                            phone = "01711-000111",
                            address = "Chawk Bazar, Dhaka",
                            notes = "Regular wholesale buyer",
                            contactType = "CUSTOMER"
                        ),
                        ContactEntity(
                            name = "Padma Distributors (Supplier)",
                            phone = "01819-222333",
                            address = "Khatunganj, Chattogram",
                            notes = "Monthly stock supplier",
                            contactType = "SUPPLIER"
                        )
                    )
                )
            }

            val currentCfg = dao.getConfigOnce() ?: AppConfigEntity()
            val updatedCfg = currentCfg.copy(
                isOnboardingCompleted = true,
                language = language,
                ownerName = ownerName.trim(),
                ownerPhone = ownerPhone.trim(),
                businessName = businessName.trim(),
                businessType = businessType.trim(),
                businessAddress = businessAddress.trim(),
                businessContact = businessContact.trim(),
                numberFormat = numberFormat,
                dateFormat = dateFormat,
                pinHash = ExportAndBackupManager.hashPin(rawPin),
                pinHint = pinHint.trim(),
                biometricEnabled = biometricEnabled,
                autoBackupEnabled = autoBackupEnabled,
                lastUsedAccountId = accountId,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertConfig(updatedCfg)
        }

        // Run initial automatic backup if enabled
        runAutomaticDailyBackupIfNeeded(force = true)
    }

    /**
     * Records a new transaction, generates a balanced double-entry journal, updates category usage & last-used account,
     * creates due reminders if credit sale/purchase has a due date, and writes an immutable audit log entry.
     */
    suspend fun recordNewTransaction(
        tx: TransactionEntity,
        attachments: List<AttachmentEntity> = emptyList()
    ): TransactionEntity {
        val cfg = dao.getConfigOnce() ?: AppConfigEntity()
        db.withTransaction {
            dao.upsertTransaction(tx)
            val bundle = AccountingEngine.createJournalForTransaction(tx, entryType = "ORIGINAL")
            dao.insertJournalEntry(bundle.entry)
            dao.insertJournalLines(bundle.lines)

            if (attachments.isNotEmpty()) {
                dao.upsertAttachments(attachments.map { it.copy(transactionId = tx.id) })
            }

            // Update category frequency for smart suggestions
            if (tx.categoryId != null) {
                val cats = dao.getCategoriesOnce()
                val targetCat = cats.find { it.id == tx.categoryId }
                if (targetCat != null) {
                    dao.upsertCategory(
                        targetCat.copy(
                            usageCount = targetCat.usageCount + 1,
                            lastUsedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Remember last-used account
            if (tx.accountId != null) {
                dao.upsertConfig(cfg.copy(lastUsedAccountId = tx.accountId, updatedAt = System.currentTimeMillis()))
            }

            // Create Due Reminder automatically if Credit Income or Credit Expense has open due > 0
            if (tx.dueAmountPaisa > 0L && (tx.txType == "CREDIT_INCOME" || tx.txType == "CREDIT_EXPENSE")) {
                val dueDay = tx.dueDateEpochDay ?: (tx.dateEpochDay + 7L)
                val remType = if (tx.txType == "CREDIT_INCOME") "CUSTOMER_DUE" else "SUPPLIER_PAYABLE"
                dao.upsertReminder(
                    ReminderEntity(
                        title = tx.title,
                        reminderType = remType,
                        relatedTransactionId = tx.id,
                        relatedContactId = tx.contactId,
                        amountPaisa = tx.dueAmountPaisa,
                        dueEpochDay = dueDay,
                        status = if (dueDay < Formatters.currentEpochDay()) "OVERDUE" else "UPCOMING"
                    )
                )
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    transactionId = tx.id,
                    action = "CREATE",
                    beforeSnapshotJson = "{}",
                    afterSnapshotJson = snapshotToJson(tx),
                    reason = "Recorded via Smart Entry",
                    actorName = cfg.ownerName.ifBlank { "Owner" }
                )
            )
        }
        return tx
    }

    /**
     * FR-07 Editing: Preserves original posted journal lines in history, posts an exact Reversal Journal Entry,
     * posts a Replacement Journal Entry with the updated values, and writes a before/after AuditLogEntity.
     */
    suspend fun editTransactionWithAudit(
        updatedTx: TransactionEntity,
        editReason: String
    ) {
        val cfg = dao.getConfigOnce() ?: AppConfigEntity()
        db.withTransaction {
            val originalTx = dao.getTransactionById(updatedTx.id) ?: return@withTransaction
            val beforeJson = snapshotToJson(originalTx)

            // Find all existing lines for this transaction to compute net active effect and reverse them
            val allLines = dao.getJournalLinesOnce().filter { it.transactionId == originalTx.id }
            val reversalBundle = AccountingEngine.createReversalJournal(
                transactionId = originalTx.id,
                existingLinesForTx = allLines,
                reversalDateEpochDay = updatedTx.dateEpochDay,
                reason = editReason.ifBlank { "Edited transaction v${originalTx.version + 1}" }
            )
            dao.insertJournalEntry(reversalBundle.entry)
            dao.insertJournalLines(reversalBundle.lines)

            // Save updated transaction with incremented version
            val newVersionTx = updatedTx.copy(
                version = originalTx.version + 1,
                status = "POSTED",
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertTransaction(newVersionTx)

            // Create replacement balanced journal
            val replacementBundle = AccountingEngine.createJournalForTransaction(
                tx = newVersionTx,
                entryType = "REPLACEMENT",
                memoPrefix = "Replacement v${newVersionTx.version}"
            )
            dao.insertJournalEntry(replacementBundle.entry)
            dao.insertJournalLines(replacementBundle.lines)

            dao.insertAuditLog(
                AuditLogEntity(
                    transactionId = originalTx.id,
                    action = "EDIT_REVERSAL_REPLACE",
                    beforeSnapshotJson = beforeJson,
                    afterSnapshotJson = snapshotToJson(newVersionTx),
                    reason = editReason.ifBlank { "Owner correction" },
                    actorName = cfg.ownerName.ifBlank { "Owner" }
                )
            )
        }
    }

    /**
     * FR-07 Logical Deletion: Never deletes raw database rows. Marks transaction as DELETED,
     * posts a compensating Reversal Journal Entry, and records an AuditLogEntity.
     */
    suspend fun logicalDeleteTransaction(
        transactionId: String,
        deleteReason: String
    ) {
        val cfg = dao.getConfigOnce() ?: AppConfigEntity()
        db.withTransaction {
            val originalTx = dao.getTransactionById(transactionId) ?: return@withTransaction
            if (originalTx.status == "DELETED") return@withTransaction

            val beforeJson = snapshotToJson(originalTx)
            val allLines = dao.getJournalLinesOnce().filter { it.transactionId == originalTx.id }

            val reversalBundle = AccountingEngine.createReversalJournal(
                transactionId = originalTx.id,
                existingLinesForTx = allLines,
                reversalDateEpochDay = Formatters.currentEpochDay(),
                reason = deleteReason.ifBlank { "Logical deletion" }
            )
            dao.insertJournalEntry(reversalBundle.entry)
            dao.insertJournalLines(reversalBundle.lines)

            val deletedTx = originalTx.copy(
                status = "DELETED",
                version = originalTx.version + 1,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertTransaction(deletedTx)

            // Dismiss any open reminders tied to this transaction
            val reminders = dao.getRemindersOnce().filter { it.relatedTransactionId == transactionId }
            reminders.forEach { rem ->
                dao.upsertReminder(rem.copy(status = "DISMISSED"))
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    transactionId = originalTx.id,
                    action = "LOGICAL_DELETE_REVERSAL",
                    beforeSnapshotJson = beforeJson,
                    afterSnapshotJson = snapshotToJson(deletedTx),
                    reason = deleteReason.ifBlank { "Logical delete with reversal" },
                    actorName = cfg.ownerName.ifBlank { "Owner" }
                )
            )
        }
    }

    /**
     * Adds or edits an Account. Accounts with history cannot be deleted, only archived (FR-04).
     */
    suspend fun saveAccount(
        account: AccountEntity,
        isNew: Boolean
    ) {
        val cfg = dao.getConfigOnce() ?: AppConfigEntity()
        db.withTransaction {
            dao.upsertAccount(account)
            if (isNew && account.openingBalancePaisa != 0L) {
                val openTx = TransactionEntity(
                    txType = "OPENING_BALANCE",
                    amountPaisa = account.openingBalancePaisa,
                    settledAmountPaisa = account.openingBalancePaisa,
                    dueAmountPaisa = 0L,
                    accountId = account.id,
                    dateEpochDay = account.openingDateEpochDay,
                    title = "Opening Balance (${account.name})",
                    referenceCode = "OPEN-${account.name.take(4).uppercase()}"
                )
                dao.upsertTransaction(openTx)
                val bundle = AccountingEngine.createJournalForTransaction(openTx, entryType = "OPENING")
                dao.insertJournalEntry(bundle.entry)
                dao.insertJournalLines(bundle.lines)
                dao.insertAuditLog(
                    AuditLogEntity(
                        transactionId = openTx.id,
                        action = "OPENING_EQUITY",
                        beforeSnapshotJson = "{}",
                        afterSnapshotJson = snapshotToJson(openTx),
                        reason = "New account opening balance",
                        actorName = cfg.ownerName.ifBlank { "Owner" }
                    )
                )
            }
        }
    }

    suspend fun toggleArchiveAccount(account: AccountEntity) {
        dao.upsertAccount(account.copy(isArchived = !account.isArchived))
    }

    suspend fun upsertContact(contact: ContactEntity) {
        dao.upsertContact(contact)
    }

    suspend fun upsertCategory(category: CategoryEntity) {
        dao.upsertCategory(category)
    }

    /**
     * Copies an attachment URI or creates a local receipt/note attachment file in files/attachments/
     */
    suspend fun createLocalAttachmentFromUri(
        uri: Uri?,
        customTitle: String,
        mimeType: String,
        noteContentIfGenerated: String = ""
    ): AttachmentEntity {
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val ext = when (mimeType) {
            "PDF" -> "pdf"
            "IMAGE" -> "jpg"
            else -> "txt"
        }
        val safeName = customTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "receipt" }
        val file = File(dir, "${safeName}_${System.currentTimeMillis()}.$ext")

        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        if (!file.exists() || file.length() == 0L) {
            file.writeText(
                "HishabKhata Offline Receipt Voucher\nTitle: $customTitle\nType: $mimeType\nDetails: $noteContentIfGenerated\nTimestamp: ${System.currentTimeMillis()}",
                Charsets.UTF_8
            )
        }

        return AttachmentEntity(
            transactionId = "",
            fileName = file.name,
            mimeType = mimeType,
            localPath = file.absolutePath,
            fileSizeBytes = file.length(),
            isMissingOrCorrupt = !file.exists()
        )
    }

    suspend fun removeAttachment(attachmentId: String) {
        val all = dao.getAttachmentsOnce()
        val target = all.find { it.id == attachmentId }
        if (target != null) {
            runCatching { File(target.localPath).delete() }
            dao.deleteAttachmentById(attachmentId)
        }
    }

    /**
     * FR-05 Recurring Transactions & Reminders:
     * Evaluates active recurring templates up to today, prevents duplicate execution via unique `idempotencyKey`,
     * and either auto-posts or creates a pending reminder.
     */
    suspend fun saveRecurringTemplate(template: RecurringTemplateEntity) {
        dao.upsertRecurringTemplate(template)
        if (template.isBillReminder) {
            dao.upsertReminder(
                ReminderEntity(
                    title = template.title,
                    reminderType = "RECURRING_BILL",
                    relatedTemplateId = template.id,
                    relatedContactId = template.contactId,
                    amountPaisa = template.amountPaisa,
                    dueEpochDay = template.nextRunEpochDay,
                    status = "UPCOMING"
                )
            )
        }
    }

    suspend fun executeRecurringTemplateNow(template: RecurringTemplateEntity): Boolean {
        val scheduledDay = template.nextRunEpochDay
        val idempotencyKey = "${template.id}:$scheduledDay"
        val existingRun = dao.getRecurringRunByKey(idempotencyKey)
        if (existingRun != null) {
            // Already executed for this scheduled occurrence! Advance nextRunEpochDay safely
            val nextDay = computeNextScheduleEpochDay(scheduledDay, template.frequency, template.intervalDays)
            dao.upsertRecurringTemplate(template.copy(nextRunEpochDay = nextDay))
            return false
        }

        val tx = TransactionEntity(
            txType = template.txType,
            amountPaisa = template.amountPaisa,
            settledAmountPaisa = template.amountPaisa,
            dueAmountPaisa = 0L,
            accountId = template.accountId,
            categoryId = template.categoryId,
            contactId = template.contactId,
            dateEpochDay = Formatters.currentEpochDay(),
            title = "${template.title} (Recurring)",
            referenceCode = "REC-${scheduledDay}",
            internalNote = "Generated from recurring schedule (${template.frequency})"
        )
        recordNewTransaction(tx)

        val run = RecurringRunEntity(
            templateId = template.id,
            scheduledEpochDay = scheduledDay,
            idempotencyKey = idempotencyKey,
            generatedTransactionId = tx.id,
            status = "POSTED"
        )
        dao.insertRecurringRun(run)

        val nextDay = computeNextScheduleEpochDay(scheduledDay, template.frequency, template.intervalDays)
        val shouldStayActive = template.endDateEpochDay == null || nextDay <= template.endDateEpochDay
        dao.upsertRecurringTemplate(
            template.copy(
                nextRunEpochDay = nextDay,
                isActive = shouldStayActive
            )
        )

        if (template.isBillReminder && shouldStayActive) {
            dao.upsertReminder(
                ReminderEntity(
                    title = template.title,
                    reminderType = "RECURRING_BILL",
                    relatedTemplateId = template.id,
                    relatedContactId = template.contactId,
                    amountPaisa = template.amountPaisa,
                    dueEpochDay = nextDay,
                    status = "UPCOMING"
                )
            )
        }
        return true
    }

    suspend fun processDueAutoRecurringSchedules() {
        val today = Formatters.currentEpochDay()
        val templates = dao.getRecurringTemplatesOnce().filter {
            it.isActive && it.autoPostPolicy == "AUTO_POST" && it.nextRunEpochDay <= today
        }
        for (tmpl in templates) {
            executeRecurringTemplateNow(tmpl)
        }
    }

    private fun computeNextScheduleEpochDay(currentEpochDay: Long, frequency: String, intervalDays: Int): Long {
        val delta = when (frequency) {
            "DAILY" -> 1L
            "WEEKLY" -> 7L
            "MONTHLY" -> 30L
            "YEARLY" -> 365L
            else -> intervalDays.coerceAtLeast(1).toLong()
        }
        return currentEpochDay + delta
    }

    suspend fun updateReminderStatus(
        reminder: ReminderEntity,
        newStatus: String,
        snoozeDays: Int = 0,
        payFromAccountId: String? = null
    ) {
        val today = Formatters.currentEpochDay()
        if (newStatus == "SNOOZED") {
            val until = today + snoozeDays.coerceAtLeast(1)
            dao.upsertReminder(reminder.copy(status = "SNOOZED", snoozedUntilEpochDay = until, dueEpochDay = until))
            return
        }
        if (newStatus == "PAID" && payFromAccountId != null) {
            when (reminder.reminderType) {
                "CUSTOMER_DUE" -> {
                    val tx = TransactionEntity(
                        txType = "RECEIVE_DUE",
                        amountPaisa = reminder.amountPaisa,
                        settledAmountPaisa = reminder.amountPaisa,
                        dueAmountPaisa = 0L,
                        accountId = payFromAccountId,
                        contactId = reminder.relatedContactId,
                        parentTransactionId = reminder.relatedTransactionId,
                        dateEpochDay = today,
                        title = "Due Received: ${reminder.title}",
                        referenceCode = "REM-RCV"
                    )
                    recordNewTransaction(tx)
                }
                "SUPPLIER_PAYABLE" -> {
                    val tx = TransactionEntity(
                        txType = "PAY_DUE",
                        amountPaisa = reminder.amountPaisa,
                        settledAmountPaisa = reminder.amountPaisa,
                        dueAmountPaisa = 0L,
                        accountId = payFromAccountId,
                        contactId = reminder.relatedContactId,
                        parentTransactionId = reminder.relatedTransactionId,
                        dateEpochDay = today,
                        title = "Payable Settled: ${reminder.title}",
                        referenceCode = "REM-PAY"
                    )
                    recordNewTransaction(tx)
                }
                "RECURRING_BILL" -> {
                    val tx = TransactionEntity(
                        txType = "CASH_EXPENSE",
                        amountPaisa = reminder.amountPaisa,
                        settledAmountPaisa = reminder.amountPaisa,
                        dueAmountPaisa = 0L,
                        accountId = payFromAccountId,
                        contactId = reminder.relatedContactId,
                        dateEpochDay = today,
                        title = "Bill Paid: ${reminder.title}",
                        referenceCode = "BILL-PAID"
                    )
                    recordNewTransaction(tx)
                }
            }
        }
        dao.upsertReminder(reminder.copy(status = newStatus))
    }

    /**
     * FR-09 Backup & 7-Generation Automatic Rotation:
     * Retains the latest 7 AUTOMATIC backups, rotating older automatic copies.
     * MANUAL exports are separately retained and never removed by automatic rotation.
     */
    suspend fun createBackup(backupType: String): BackupRecordEntity {
        val cfg = dao.getConfigOnce() ?: AppConfigEntity()
        val record = ExportAndBackupManager.writeBackupFile(
            context = context,
            backupType = backupType,
            config = cfg,
            accounts = dao.getAccountsOnce(),
            categories = dao.getCategoriesOnce(),
            contacts = dao.getContactsOnce(),
            transactions = dao.getTransactionsOnce(),
            journalEntries = dao.getJournalEntriesOnce(),
            journalLines = dao.getJournalLinesOnce(),
            attachments = dao.getAttachmentsOnce(),
            recurringTemplates = dao.getRecurringTemplatesOnce(),
            recurringRuns = dao.getRecurringRunsOnce(),
            reminders = dao.getRemindersOnce(),
            auditLogs = dao.getAuditLogsOnce()
        )
        dao.upsertBackupRecord(record)

        if (backupType == "AUTOMATIC") {
            dao.upsertConfig(cfg.copy(lastAutoBackupEpochDay = Formatters.currentEpochDay()))
            // Rotate automatic backups: retain latest 7, remove older automatic copies
            val allBackups = dao.getBackupHistoryOnce()
            val autoBackups = allBackups
                .filter { it.backupType == "AUTOMATIC" }
                .sortedByDescending { it.createdAt }
            if (autoBackups.size > 7) {
                val toRotateOut = autoBackups.drop(7)
                for (old in toRotateOut) {
                    runCatching { File(old.localFilePath).delete() }
                    dao.deleteBackupRecordById(old.id)
                }
            }
        }
        return record
    }

    suspend fun runAutomaticDailyBackupIfNeeded(force: Boolean = false) {
        val cfg = dao.getConfigOnce() ?: return
        if (!cfg.isOnboardingCompleted || !cfg.autoBackupEnabled) return
        val today = Formatters.currentEpochDay()
        if (force || cfg.lastAutoBackupEpochDay < today) {
            runCatching { createBackup("AUTOMATIC") }
        }
    }

    /**
     * FR-09 Restore: Validates checksum & schema version before touching database,
     * replaces tables inside an atomic Room transaction, runs post-restore ledger reconciliation,
     * and writes a BACKUP_RESTORE audit event.
     */
    suspend fun restoreFromBackupRecord(record: BackupRecordEntity): Result<Int> {
        return runCatching {
            val currentCfg = dao.getConfigOnce() ?: AppConfigEntity()
            val file = File(record.localFilePath)
            // Validate & parse BEFORE modifying database so bad checksum leaves data untouched
            val snapshot = ExportAndBackupManager.parseAndVerifyBackupFile(file, currentCfg)

            // Verify journal reconciliation of backup snapshot before committing
            val recon = AccountingEngine.verifyLedgerReconciliation(
                entries = snapshot.journalEntries,
                lines = snapshot.journalLines
            )
            if (!recon.isHealthy) {
                throw IllegalStateException("Backup ledger failed double-entry reconciliation check.")
            }

            db.withTransaction {
                dao.clearAccounts()
                dao.clearCategories()
                dao.clearContacts()
                dao.clearTransactions()
                dao.clearJournalEntries()
                dao.clearJournalLines()
                dao.clearAttachments()
                dao.clearRecurringTemplates()
                dao.clearRecurringRuns()
                dao.clearReminders()
                dao.clearAuditLogs()

                dao.upsertConfig(snapshot.config)
                dao.upsertAccounts(snapshot.accounts)
                dao.upsertCategories(snapshot.categories)
                dao.upsertContacts(snapshot.contacts)
                dao.upsertTransactions(snapshot.transactions)
                dao.insertJournalEntries(snapshot.journalEntries)
                dao.insertJournalLines(snapshot.journalLines)
                dao.upsertAttachments(snapshot.attachments)
                dao.upsertRecurringTemplates(snapshot.recurringTemplates)
                dao.upsertRecurringRuns(snapshot.recurringRuns)
                dao.upsertReminders(snapshot.reminders)
                dao.insertAuditLogs(snapshot.auditLogs)

                dao.insertAuditLog(
                    AuditLogEntity(
                        transactionId = "SYSTEM-RESTORE",
                        action = "BACKUP_RESTORE",
                        beforeSnapshotJson = "{\"previousLastBackupDay\":${currentCfg.lastAutoBackupEpochDay}}",
                        afterSnapshotJson = "{\"restoredFile\":\"${record.fileName}\",\"checksum\":\"${snapshot.checksumSha256.take(12)}\"}",
                        reason = "Restored validated ${record.backupType} backup (${snapshot.transactions.size} transactions)",
                        actorName = snapshot.config.ownerName.ifBlank { "Owner" }
                    )
                )
            }
            snapshot.transactions.size
        }
    }

    private fun snapshotToJson(tx: TransactionEntity): String {
        return JSONObject().apply {
            put("id", tx.id)
            put("txType", tx.txType)
            put("title", tx.title)
            put("amountPaisa", tx.amountPaisa)
            put("settledAmountPaisa", tx.settledAmountPaisa)
            put("dueAmountPaisa", tx.dueAmountPaisa)
            put("accountId", tx.accountId ?: "")
            put("toAccountId", tx.toAccountId ?: "")
            put("categoryId", tx.categoryId ?: "")
            put("contactId", tx.contactId ?: "")
            put("dateEpochDay", tx.dateEpochDay)
            put("status", tx.status)
            put("version", tx.version)
        }.toString()
    }
}
