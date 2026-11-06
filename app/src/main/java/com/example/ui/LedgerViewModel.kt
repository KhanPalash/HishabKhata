package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountEntity
import com.example.data.AppConfigEntity
import com.example.data.AppDatabase
import com.example.data.AttachmentEntity
import com.example.data.AuditLogEntity
import com.example.data.BackupRecordEntity
import com.example.data.CategoryEntity
import com.example.data.ContactEntity
import com.example.data.JournalEntryEntity
import com.example.data.JournalLineEntity
import com.example.data.LedgerRepository
import com.example.data.RecurringRunEntity
import com.example.data.RecurringTemplateEntity
import com.example.data.ReminderEntity
import com.example.data.TransactionEntity
import com.example.domain.AccountBalanceSummary
import com.example.domain.AccountingEngine
import com.example.domain.ContactLedgerSummary
import com.example.domain.PeriodFinancialSummary
import com.example.domain.ReconciliationReport
import com.example.util.ExportAndBackupManager
import com.example.util.Formatters
import com.example.util.ReportExportRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class MainTab {
    DASHBOARD,
    TRANSACTIONS,
    CONTACTS,
    ACCOUNTS,
    REPORTS,
    AUTOMATION,
    SETTINGS
}

enum class PeriodPreset(val labelEn: String, val labelBn: String) {
    TODAY("Today", "আজ"),
    THIS_WEEK("Last 7 Days", "গত ৭ দিন"),
    THIS_MONTH("Last 30 Days", "গত ৩০ দিন"),
    THIS_YEAR("Last 365 Days", "গত ১ বছর"),
    ALL_TIME("All Time", "সব সময়"),
    CUSTOM("Custom Range", "কাস্টম তারিখ")
}

data class ExportPreviewState(
    val title: String,
    val periodLabel: String,
    val summaryPairs: List<Pair<String, String>>,
    val headers: List<String>,
    val rows: List<ReportExportRow>,
    val generatedPdfFile: File? = null,
    val generatedCsvFile: File? = null
)

data class LedgerUiState(
    val isInitialized: Boolean = false,
    val config: AppConfigEntity = AppConfigEntity(),
    val isAppLocked: Boolean = false,
    val activeTab: MainTab = MainTab.DASHBOARD,
    val periodPreset: PeriodPreset = PeriodPreset.THIS_MONTH,
    val customStartEpochDay: Long = Formatters.currentEpochDay() - 30L,
    val customEndEpochDay: Long = Formatters.currentEpochDay(),
    val accounts: List<AccountEntity> = emptyList(),
    val accountBalances: List<AccountBalanceSummary> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val contactLedgers: List<ContactLedgerSummary> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val journalLines: List<JournalLineEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val recurringTemplates: List<RecurringTemplateEntity> = emptyList(),
    val recurringRuns: List<RecurringRunEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val backupHistory: List<BackupRecordEntity> = emptyList(),
    val currentPeriodSummary: PeriodFinancialSummary? = null,
    val previousPeriodSummary: PeriodFinancialSummary? = null,
    val reconciliation: ReconciliationReport = ReconciliationReport(true, 0, 0, 0L, 0L, emptyList()),
    val bannerMessage: String? = null,
    val exportPreview: ExportPreviewState? = null,
    val preselectedSmartEntryType: String? = null,
    val preselectedContactId: String? = null,
    val showSmartEntrySheet: Boolean = false,
    val editingTransaction: TransactionEntity? = null
)

private data class LocalActionState(
    val isAppLocked: Boolean = false,
    val hasCheckedInitialLock: Boolean = false,
    val activeTab: MainTab = MainTab.DASHBOARD,
    val periodPreset: PeriodPreset = PeriodPreset.THIS_MONTH,
    val customStartEpochDay: Long = Formatters.currentEpochDay() - 30L,
    val customEndEpochDay: Long = Formatters.currentEpochDay(),
    val bannerMessage: String? = null,
    val exportPreview: ExportPreviewState? = null,
    val preselectedSmartEntryType: String? = null,
    val preselectedContactId: String? = null,
    val showSmartEntrySheet: Boolean = false,
    val editingTransaction: TransactionEntity? = null
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = LedgerRepository(application, db)

    private val localState = MutableStateFlow(LocalActionState())

    private val coreDataFlow = combine(
        repository.configFlow,
        repository.accountsFlow,
        repository.categoriesFlow,
        repository.contactsFlow,
        repository.transactionsFlow
    ) { cfg, accs, cats, cons, txs ->
        Quint(cfg ?: AppConfigEntity(), accs, cats, cons, txs)
    }

    private val journalAndAuditFlow = combine(
        repository.journalEntriesFlow,
        repository.journalLinesFlow,
        repository.attachmentsFlow,
        repository.auditLogsFlow,
        repository.backupHistoryFlow
    ) { entries, lines, atts, logs, backups ->
        Quint(entries, lines, atts, logs, backups)
    }

    private val automationFlow = combine(
        repository.recurringTemplatesFlow,
        repository.recurringRunsFlow,
        repository.remindersFlow
    ) { templates, runs, rems ->
        Triple(templates, runs, rems)
    }

    val uiState: StateFlow<LedgerUiState> = combine(
        coreDataFlow,
        journalAndAuditFlow,
        automationFlow,
        localState
    ) { core, journalData, autoData, local ->
        val cfg = core.first
        val accs = core.second
        val cats = core.third
        val cons = core.fourth
        val txs = core.fifth

        val entries = journalData.first
        val lines = journalData.second
        val atts = journalData.third
        val logs = journalData.fourth
        val backups = journalData.fifth

        val templates = autoData.first
        val runs = autoData.second
        val rems = autoData.third

        val today = Formatters.currentEpochDay()
        val (startDay, endDay) = when (local.periodPreset) {
            PeriodPreset.TODAY -> today to today
            PeriodPreset.THIS_WEEK -> (today - 6L) to today
            PeriodPreset.THIS_MONTH -> (today - 29L) to today
            PeriodPreset.THIS_YEAR -> (today - 364L) to today
            PeriodPreset.ALL_TIME -> 0L to (today + 3650L)
            PeriodPreset.CUSTOM -> local.customStartEpochDay.coerceAtMost(local.customEndEpochDay) to
                local.customEndEpochDay.coerceAtLeast(local.customStartEpochDay)
        }

        val spanDays = (endDay - startDay + 1L).coerceAtLeast(1L)
        val prevEnd = startDay - 1L
        val prevStart = (prevEnd - spanDays + 1L).coerceAtLeast(0L)

        val accountBalances = AccountingEngine.computeAccountBalances(accs, lines)
        val contactLedgers = AccountingEngine.computeContactLedgers(cons, lines)
        val currentSummary = AccountingEngine.computePeriodSummary(startDay, endDay, accs, cats, lines)
        val prevSummary = AccountingEngine.computePeriodSummary(prevStart, prevEnd, accs, cats, lines)
        val recon = AccountingEngine.verifyLedgerReconciliation(entries, lines)

        LedgerUiState(
            isInitialized = true,
            config = cfg,
            isAppLocked = local.isAppLocked,
            activeTab = local.activeTab,
            periodPreset = local.periodPreset,
            customStartEpochDay = startDay,
            customEndEpochDay = endDay,
            accounts = accs,
            accountBalances = accountBalances,
            categories = cats,
            contacts = cons,
            contactLedgers = contactLedgers,
            transactions = txs,
            journalEntries = entries,
            journalLines = lines,
            attachments = atts,
            recurringTemplates = templates,
            recurringRuns = runs,
            reminders = rems,
            auditLogs = logs,
            backupHistory = backups,
            currentPeriodSummary = currentSummary,
            previousPeriodSummary = prevSummary,
            reconciliation = recon,
            bannerMessage = local.bannerMessage,
            exportPreview = local.exportPreview,
            preselectedSmartEntryType = local.preselectedSmartEntryType,
            preselectedContactId = local.preselectedContactId,
            showSmartEntrySheet = local.showSmartEntrySheet,
            editingTransaction = local.editingTransaction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededDefaults()
            val cfg = db.ledgerDao().getConfigOnce()
            if (cfg != null && cfg.isOnboardingCompleted && cfg.pinHash.isNotBlank()) {
                localState.update { it.copy(isAppLocked = true, hasCheckedInitialLock = true) }
            }
            repository.processDueAutoRecurringSchedules()
            repository.runAutomaticDailyBackupIfNeeded()
        }
    }

    fun selectTab(tab: MainTab) {
        localState.update { it.copy(activeTab = tab) }
    }

    fun setPeriodPreset(preset: PeriodPreset, customStart: Long? = null, customEnd: Long? = null) {
        localState.update {
            it.copy(
                periodPreset = preset,
                customStartEpochDay = customStart ?: it.customStartEpochDay,
                customEndEpochDay = customEnd ?: it.customEndEpochDay
            )
        }
    }

    fun clearBanner() {
        localState.update { it.copy(bannerMessage = null) }
    }

    fun showBanner(msg: String) {
        localState.update { it.copy(bannerMessage = msg) }
    }

    fun openSmartEntry(txType: String? = null, contactId: String? = null, editTx: TransactionEntity? = null) {
        localState.update {
            it.copy(
                preselectedSmartEntryType = txType,
                preselectedContactId = contactId,
                editingTransaction = editTx,
                showSmartEntrySheet = true
            )
        }
    }

    fun closeSmartEntry() {
        localState.update {
            it.copy(
                showSmartEntrySheet = false,
                preselectedSmartEntryType = null,
                preselectedContactId = null,
                editingTransaction = null
            )
        }
    }

    fun lockAppNow() {
        val cfg = uiState.value.config
        if (cfg.pinHash.isNotBlank()) {
            localState.update { it.copy(isAppLocked = true) }
        }
    }

    fun attemptPinUnlock(rawPin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val cfg = db.ledgerDao().getConfigOnce() ?: AppConfigEntity()
            val now = System.currentTimeMillis()
            if (cfg.lockoutUntilMillis > now) {
                val secsLeft = ((cfg.lockoutUntilMillis - now) / 1000L).coerceAtLeast(1L)
                onResult(false, "Too many failed attempts. Try again in ${secsLeft}s.")
                return@launch
            }
            val hashed = ExportAndBackupManager.hashPin(rawPin)
            if (hashed == cfg.pinHash || cfg.pinHash.isBlank()) {
                repository.updateConfig { it.copy(failedPinAttempts = 0, lockoutUntilMillis = 0L) }
                localState.update { it.copy(isAppLocked = false) }
                onResult(true, null)
            } else {
                val attempts = cfg.failedPinAttempts + 1
                val lockoutUntil = if (attempts >= 4) now + 30_000L else 0L
                repository.updateConfig {
                    it.copy(failedPinAttempts = attempts, lockoutUntilMillis = lockoutUntil)
                }
                val msg = if (attempts >= 4) {
                    "PIN locked for 30 seconds after 4 failed attempts."
                } else {
                    "Incorrect PIN (${4 - attempts} attempts remaining before cooldown)."
                }
                onResult(false, msg)
            }
        }
    }

    fun unlockWithBiometricSimulator() {
        viewModelScope.launch {
            repository.updateConfig { it.copy(failedPinAttempts = 0, lockoutUntilMillis = 0L) }
            localState.update { it.copy(isAppLocked = false) }
        }
    }

    fun resetPinWithOwnerPhoneVerification(ownerPhoneInput: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cfg = db.ledgerDao().getConfigOnce() ?: AppConfigEntity()
            val cleanEntered = Formatters.normalizeDigitsToAscii(ownerPhoneInput.trim()).filter { it.isDigit() }
            val cleanStored = Formatters.normalizeDigitsToAscii(cfg.ownerPhone.trim()).filter { it.isDigit() }
            if (cleanStored.isNotEmpty() && cleanEntered != cleanStored) {
                onResult(false, "Owner phone number does not match registered profile.")
                return@launch
            }
            if (Formatters.normalizeDigitsToAscii(newPin).length < 4) {
                onResult(false, "New PIN must be at least 4 digits.")
                return@launch
            }
            repository.updateConfig {
                it.copy(
                    pinHash = ExportAndBackupManager.hashPin(newPin),
                    failedPinAttempts = 0,
                    lockoutUntilMillis = 0L
                )
            }
            localState.update { it.copy(isAppLocked = false) }
            onResult(true, "PIN reset successfully.")
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            repository.updateConfig {
                val next = if (it.language == "EN") "BN" else "EN"
                it.copy(language = next)
            }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(themeMode = mode) }
        }
    }

    fun updateNumberAndDateFormat(numberFormat: String, dateFormat: String) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(numberFormat = numberFormat, dateFormat = dateFormat) }
        }
    }

    fun updateBusinessAndOwnerProfile(
        ownerName: String,
        ownerPhone: String,
        businessName: String,
        businessType: String,
        businessAddress: String,
        businessContact: String
    ) {
        viewModelScope.launch {
            repository.updateConfig {
                it.copy(
                    ownerName = ownerName.trim(),
                    ownerPhone = ownerPhone.trim(),
                    businessName = businessName.trim(),
                    businessType = businessType.trim(),
                    businessAddress = businessAddress.trim(),
                    businessContact = businessContact.trim()
                )
            }
            showBanner("Business & Owner profile saved.")
        }
    }

    fun updateSecurityAndBackupPreferences(
        biometricEnabled: Boolean,
        autoBackupEnabled: Boolean,
        notificationsEnabled: Boolean,
        duplicateWarningEnabled: Boolean,
        newPinOrEmpty: String,
        pinHint: String
    ) {
        viewModelScope.launch {
            repository.updateConfig { cfg ->
                cfg.copy(
                    biometricEnabled = biometricEnabled,
                    autoBackupEnabled = autoBackupEnabled,
                    notificationsEnabled = notificationsEnabled,
                    duplicateWarningEnabled = duplicateWarningEnabled,
                    pinHash = if (newPinOrEmpty.length >= 4) ExportAndBackupManager.hashPin(newPinOrEmpty) else cfg.pinHash,
                    pinHint = pinHint.trim()
                )
            }
            showBanner("Security & Backup settings updated.")
        }
    }

    fun completeOnboarding(
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
        autoBackupEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.completeOnboarding(
                language = language,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                businessName = businessName,
                businessType = businessType,
                businessAddress = businessAddress,
                businessContact = businessContact,
                numberFormat = numberFormat,
                dateFormat = dateFormat,
                firstAccountName = firstAccountName,
                firstAccountNameBn = firstAccountNameBn,
                firstAccountType = firstAccountType,
                firstAccountNumber = firstAccountNumber,
                openingBalancePaisa = openingBalancePaisa,
                openingDateEpochDay = openingDateEpochDay,
                rawPin = rawPin,
                pinHint = pinHint,
                biometricEnabled = biometricEnabled,
                autoBackupEnabled = autoBackupEnabled
            )
            localState.update { it.copy(isAppLocked = false, activeTab = MainTab.DASHBOARD) }
            showBanner(
                Formatters.tr(
                    language,
                    "Welcome! Opening balance posted to Opening Equity.",
                    "স্বাগতম! প্রারম্ভিক ব্যালেন্স ওপেনিং ইক্যুইটিতে জমা হয়েছে।"
                )
            )
        }
    }

    fun saveTransactionFromSmartForm(
        tx: TransactionEntity,
        attachments: List<AttachmentEntity>,
        isEdit: Boolean,
        editReason: String
    ) {
        viewModelScope.launch {
            if (isEdit) {
                repository.editTransactionWithAudit(tx, editReason)
                showBanner(
                    Formatters.tr(
                        uiState.value.config.language,
                        "Transaction updated via reversal & replacement journal.",
                        "লেনদেনটি রিভার্সাল ও রিপ্লেসমেন্ট জার্নালের মাধ্যমে আপডেট হয়েছে।"
                    )
                )
            } else {
                repository.recordNewTransaction(tx, attachments)
                showBanner(
                    Formatters.tr(
                        uiState.value.config.language,
                        "Balanced journal posted & balances updated.",
                        "ব্যালেন্সড জার্নাল এবং হিসাব সংরক্ষিত হয়েছে।"
                    )
                )
            }
            closeSmartEntry()
        }
    }

    fun deleteTransactionLogically(transactionId: String, reason: String) {
        viewModelScope.launch {
            repository.logicalDeleteTransaction(transactionId, reason)
            showBanner(
                Formatters.tr(
                    uiState.value.config.language,
                    "Transaction logically deleted with compensating reversal entry.",
                    "লেনদেনটি রিভার্সাল এন্ট্রিসহ লজিক্যাল ডিলিট করা হয়েছে।"
                )
            )
        }
    }

    fun saveAccount(account: AccountEntity, isNew: Boolean) {
        viewModelScope.launch {
            repository.saveAccount(account, isNew)
            showBanner(
                Formatters.tr(
                    uiState.value.config.language,
                    "Account saved.",
                    "অ্যাকাউন্ট সংরক্ষিত হয়েছে।"
                )
            )
        }
    }

    fun toggleArchiveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.toggleArchiveAccount(account)
            showBanner(
                if (!account.isArchived) "Account archived." else "Account restored to active."
            )
        }
    }

    fun saveContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.upsertContact(contact)
            showBanner("Contact saved.")
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.upsertCategory(category)
            showBanner("Category saved.")
        }
    }

    fun saveRecurringTemplate(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            repository.saveRecurringTemplate(template)
            showBanner("Recurring schedule & reminder saved.")
        }
    }

    fun runRecurringTemplateNow(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            val ran = repository.executeRecurringTemplateNow(template)
            if (ran) {
                showBanner("Recurring transaction posted & next run scheduled.")
            } else {
                showBanner("Already executed for this scheduled date (Idempotent check prevented duplicate).")
            }
        }
    }

    fun handleReminderAction(
        reminder: ReminderEntity,
        newStatus: String,
        snoozeDays: Int = 0,
        payFromAccountId: String? = null
    ) {
        viewModelScope.launch {
            repository.updateReminderStatus(reminder, newStatus, snoozeDays, payFromAccountId)
            showBanner("Reminder updated ($newStatus).")
        }
    }

    fun triggerLocalNotificationTest(reminder: ReminderEntity) {
        val state = uiState.value
        val formattedAmt = Formatters.formatBdt(reminder.amountPaisa, state.config.language, state.config.numberFormat)
        ExportAndBackupManager.sendLocalReminderNotification(
            context = getApplication(),
            title = reminder.title,
            message = "${reminder.reminderType.replace("_", " ")} • $formattedAmt",
            notificationId = reminder.id.hashCode()
        )
        showBanner("Local reminder notification sent.")
    }

    fun createManualOrAutoBackup(backupType: String) {
        viewModelScope.launch {
            val rec = withContext(Dispatchers.IO) {
                repository.createBackup(backupType)
            }
            showBanner("${rec.backupType} backup created (${rec.fileName}).")
        }
    }

    fun restoreBackupWithPinCheck(
        record: BackupRecordEntity,
        enteredPin: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cfg = uiState.value.config
            if (cfg.pinHash.isNotBlank() && ExportAndBackupManager.hashPin(enteredPin) != cfg.pinHash) {
                onComplete(false, "Invalid PIN. Restore cancelled.")
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                repository.restoreFromBackupRecord(record)
            }
            result.fold(
                onSuccess = { count ->
                    showBanner("Restored $count transactions & verified double-entry journal.")
                    onComplete(true, "Restore succeeded.")
                },
                onFailure = { err ->
                    onComplete(false, err.message ?: "Restore validation failed.")
                }
            )
        }
    }

    fun prepareReportPreviewAndExport(
        reportType: String,
        filterAccountId: String? = null,
        filterContactId: String? = null
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val cfg = state.config
            val lang = cfg.language
            val numFmt = cfg.numberFormat
            val dateFmt = cfg.dateFormat
            val summary = state.currentPeriodSummary ?: return@launch

            val periodLabel = "${Formatters.formatEpochDay(summary.startEpochDay, dateFmt, lang)} - ${Formatters.formatEpochDay(summary.endEpochDay, dateFmt, lang)}"
            val accMap = state.accounts.associateBy { it.id }
            val catMap = state.categories.associateBy { it.id }
            val conMap = state.contacts.associateBy { it.id }

            val summaryPairs = mutableListOf<Pair<String, String>>()
            val headers = mutableListOf<String>()
            val rows = mutableListOf<ReportExportRow>()
            var reportTitle = reportType

            when (reportType) {
                "CASH_PL" -> {
                    reportTitle = Formatters.tr(lang, "Cash-Basis Profit & Loss Statement", "নগদ লাভ-ক্ষতি বিবরণী (Cash P&L)")
                    summaryPairs.add("Cash Income" to Formatters.formatBdt(summary.cashIncomePaisa, lang, numFmt))
                    summaryPairs.add("Cash Expense" to Formatters.formatBdt(summary.cashExpensePaisa, lang, numFmt))
                    summaryPairs.add("Cash-Basis Net Profit" to Formatters.formatBdt(summary.cashProfitPaisa, lang, numFmt))
                    summaryPairs.add("Active Cash Balance" to Formatters.formatBdt(summary.totalActiveCashBalancePaisa, lang, numFmt))
                    headers.addAll(listOf("Type", "Category", "Share %", "Amount (BDT)"))
                    summary.incomeByCategory.forEach { row ->
                        rows.add(
                            ReportExportRow(
                                col1 = "CASH INCOME",
                                col2 = if (lang == "BN") row.category.nameBn else row.category.name,
                                col3 = String.format("%.1f%%", row.percentage),
                                col4 = Formatters.formatBdt(row.totalPaisa, lang, numFmt)
                            )
                        )
                    }
                    summary.expenseByCategory.forEach { row ->
                        rows.add(
                            ReportExportRow(
                                col1 = "CASH EXPENSE",
                                col2 = if (lang == "BN") row.category.nameBn else row.category.name,
                                col3 = String.format("%.1f%%", row.percentage),
                                col4 = Formatters.formatBdt(row.totalPaisa, lang, numFmt)
                            )
                        )
                    }
                }

                "CASHFLOW" -> {
                    reportTitle = Formatters.tr(lang, "Cashflow & Balance Sheet Statement", "নগদ প্রবাহ বিবরণী (Cashflow)")
                    summaryPairs.add("Net Period Cashflow" to Formatters.formatBdt(summary.netCashflowPaisa, lang, numFmt))
                    summaryPairs.add("Total Active Cash" to Formatters.formatBdt(summary.totalActiveCashBalancePaisa, lang, numFmt))
                    summaryPairs.add("Owner Capital + Opening" to Formatters.formatBdt(summary.ownerCapitalPaisa + summary.openingEquityPaisa, lang, numFmt))
                    summaryPairs.add("Loan Liability" to Formatters.formatBdt(summary.loanLiabilityPaisa, lang, numFmt))
                    headers.addAll(listOf("Account", "Type", "Inflow / Outflow", "Closing Balance"))
                    state.accountBalances.forEach { ab ->
                        rows.add(
                            ReportExportRow(
                                col1 = if (lang == "BN") ab.account.nameBn else ab.account.name,
                                col2 = ab.account.accountType,
                                col3 = "+${Formatters.formatBdt(ab.totalInflowPaisa, lang, numFmt)} / -${Formatters.formatBdt(ab.totalOutflowPaisa, lang, numFmt)}",
                                col4 = Formatters.formatBdt(ab.currentBalancePaisa, lang, numFmt)
                            )
                        )
                    }
                }

                "RECEIVABLES_PAYABLES" -> {
                    reportTitle = Formatters.tr(lang, "Customer Receivables & Supplier Payables", "বাকি ও দেনা-পাওনা রিপোর্ট")
                    summaryPairs.add("Total Customer Receivable" to Formatters.formatBdt(summary.totalReceivablePaisa, lang, numFmt))
                    summaryPairs.add("Total Supplier Payable" to Formatters.formatBdt(summary.totalPayablePaisa, lang, numFmt))
                    headers.addAll(listOf("Contact", "Role / Phone", "Total Billed / Settled", "Open Balance"))
                    state.contactLedgers.filter {
                        filterContactId == null || it.contact.id == filterContactId
                    }.forEach { cl ->
                        val openAmt = if (cl.contact.contactType == "CUSTOMER") cl.receivablePaisa else cl.payablePaisa
                        rows.add(
                            ReportExportRow(
                                col1 = cl.contact.name,
                                col2 = "${cl.contact.contactType} (${cl.contact.phone})",
                                col3 = "${Formatters.formatBdt(cl.totalBilledPaisa, lang, numFmt)} / ${Formatters.formatBdt(cl.totalSettledPaisa, lang, numFmt)}",
                                col4 = Formatters.formatBdt(openAmt, lang, numFmt)
                            )
                        )
                    }
                }

                "ACCOUNT_STATEMENT" -> {
                    val targetAcc = state.accounts.find { it.id == filterAccountId } ?: state.accounts.firstOrNull()
                    reportTitle = "Account Statement: ${targetAcc?.name ?: "All Accounts"}"
                    val accLines = state.journalLines.filter {
                        it.ledgerCode == AccountingEngine.LEDGER_ASSET_ACCOUNT &&
                            (targetAcc == null || it.accountId == targetAcc.id) &&
                            it.dateEpochDay in summary.startEpochDay..summary.endEpochDay
                    }
                    val inflow = accLines.sumOf { it.debitPaisa }
                    val outflow = accLines.sumOf { it.creditPaisa }
                    summaryPairs.add("Total Inflow (Debit)" to Formatters.formatBdt(inflow, lang, numFmt))
                    summaryPairs.add("Total Outflow (Credit)" to Formatters.formatBdt(outflow, lang, numFmt))
                    summaryPairs.add("Net Period Change" to Formatters.formatBdt(inflow - outflow, lang, numFmt))
                    headers.addAll(listOf("Date", "Ledger / Memo", "Debit (In)", "Credit (Out)"))
                    accLines.forEach { line ->
                        rows.add(
                            ReportExportRow(
                                col1 = Formatters.formatEpochDay(line.dateEpochDay, dateFmt, lang),
                                col2 = line.ledgerName,
                                col3 = if (line.debitPaisa > 0) Formatters.formatBdt(line.debitPaisa, lang, numFmt) else "-",
                                col4 = if (line.creditPaisa > 0) Formatters.formatBdt(line.creditPaisa, lang, numFmt) else "-"
                            )
                        )
                    }
                }

                else -> {
                    // "TRANSACTION_REGISTER"
                    reportTitle = Formatters.tr(lang, "Audit Transaction Register", "লেনদেন রেজিস্টার (Transaction Register)")
                    val periodTxs = state.transactions.filter {
                        it.dateEpochDay in summary.startEpochDay..summary.endEpochDay
                    }
                    summaryPairs.add("Transactions Count" to periodTxs.size.toString())
                    summaryPairs.add("Cash Income" to Formatters.formatBdt(summary.cashIncomePaisa, lang, numFmt))
                    summaryPairs.add("Cash Expense" to Formatters.formatBdt(summary.cashExpensePaisa, lang, numFmt))
                    summaryPairs.add("Journal Status" to if (state.reconciliation.isHealthy) "BALANCED" else "CHECK REQUIRED")
                    headers.addAll(listOf("Date", "Title & Type", "Account / Contact", "Amount (BDT)"))
                    periodTxs.forEach { tx ->
                        val accName = accMap[tx.accountId]?.name ?: "-"
                        val conName = conMap[tx.contactId]?.name ?: catMap[tx.categoryId]?.name ?: ""
                        rows.add(
                            ReportExportRow(
                                col1 = Formatters.formatEpochDay(tx.dateEpochDay, dateFmt, lang),
                                col2 = "${tx.title} [${tx.txType}] (${tx.status})",
                                col3 = "$accName ${if (conName.isNotBlank()) "• $conName" else ""}",
                                col4 = Formatters.formatBdt(tx.amountPaisa, lang, numFmt)
                            )
                        )
                    }
                }
            }

            if (rows.isEmpty()) {
                rows.add(
                    ReportExportRow(
                        col1 = "-",
                        col2 = Formatters.tr(lang, "No entries in selected date range", "নির্বাচিত তারিখে কোনো লেনদেন নেই"),
                        col3 = "-",
                        col4 = Formatters.formatBdt(0L, lang, numFmt)
                    )
                )
            }

            val (pdfFile, csvFile) = withContext(Dispatchers.IO) {
                val pdf = ExportAndBackupManager.generatePdfReport(
                    context = getApplication(),
                    reportTitle = reportTitle,
                    businessName = cfg.businessName.ifBlank { "HishabKhata Business" },
                    businessSubtitle = "${cfg.businessType} • ${cfg.businessAddress} • Owner: ${cfg.ownerName}",
                    periodLabel = periodLabel,
                    summaryPairs = summaryPairs,
                    headers = headers,
                    rows = rows
                )
                val csv = ExportAndBackupManager.generateCsvReport(
                    context = getApplication(),
                    reportTitle = reportTitle,
                    businessName = cfg.businessName.ifBlank { "HishabKhata Business" },
                    periodLabel = periodLabel,
                    summaryPairs = summaryPairs,
                    headers = headers,
                    rows = rows
                )
                pdf to csv
            }

            localState.update {
                it.copy(
                    exportPreview = ExportPreviewState(
                        title = reportTitle,
                        periodLabel = periodLabel,
                        summaryPairs = summaryPairs,
                        headers = headers,
                        rows = rows,
                        generatedPdfFile = pdfFile,
                        generatedCsvFile = csvFile
                    )
                )
            }
        }
    }

    fun closeExportPreview() {
        localState.update { it.copy(exportPreview = null) }
    }
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
