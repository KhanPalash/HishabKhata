package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isOnboardingCompleted: Boolean = false,
    val language: String = "EN", // "EN" or "BN"
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerPhotoUri: String = "",
    val businessName: String = "",
    val businessType: String = "Retail & General Store",
    val businessAddress: String = "",
    val businessContact: String = "",
    val currencyCode: String = "BDT",
    val currencySymbol: String = "৳",
    val numberFormat: String = "BD_LAKH", // "BD_LAKH" or "INTL"
    val dateFormat: String = "DD_MM_YYYY", // "DD_MM_YYYY" or "YYYY_MM_DD"
    val pinHash: String = "",
    val pinHint: String = "",
    val biometricEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 5,
    val failedPinAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val autoBackupEnabled: Boolean = true,
    val lastAutoBackupEpochDay: Long = 0L,
    val notificationsEnabled: Boolean = true,
    val confirmBeforeDeleteOrSave: Boolean = true,
    val duplicateWarningEnabled: Boolean = true,
    val lastUsedAccountId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["accountType"]), Index(value = ["isArchived"])]
)
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameBn: String,
    val accountType: String, // "CASH", "BANK", "BKASH", "NAGAD", "ROCKET", "WALLET"
    val accountNumber: String = "",
    val openingBalancePaisa: Long = 0L,
    val openingDateEpochDay: Long = 0L,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["categoryType"]), Index(value = ["isArchived"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameBn: String,
    val categoryType: String, // "INCOME", "EXPENSE"
    val iconName: String = "Category",
    val colorHex: String = "#10B981",
    val isArchived: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0L
)

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["contactType"]), Index(value = ["isArchived"])]
)
data class ContactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val contactType: String, // "CUSTOMER", "SUPPLIER"
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["accountId"]),
        Index(value = ["contactId"]),
        Index(value = ["categoryId"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val txType: String,
    // "CASH_INCOME", "CREDIT_INCOME", "CASH_EXPENSE", "CREDIT_EXPENSE",
    // "RECEIVE_DUE", "PAY_DUE", "TRANSFER", "OWNER_INVESTMENT",
    // "OWNER_WITHDRAWAL", "LOAN_TAKEN", "LOAN_REPAID",
    // "REFUND_INCOME", "REFUND_EXPENSE", "OPENING_BALANCE", "ADJUSTMENT"
    val amountPaisa: Long,
    val settledAmountPaisa: Long,
    val dueAmountPaisa: Long,
    val loanInterestPaisa: Long = 0L,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val contactId: String? = null,
    val parentTransactionId: String? = null,
    val dateEpochDay: Long,
    val dueDateEpochDay: Long? = null,
    val title: String,
    val referenceCode: String = "",
    val paymentDetails: String = "",
    val internalNote: String = "",
    val status: String = "POSTED", // "POSTED", "REVERSED", "DELETED"
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["transactionId"]), Index(value = ["dateEpochDay"])]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val entryType: String, // "ORIGINAL", "REVERSAL", "REPLACEMENT", "ADJUSTMENT", "OPENING", "SETTLEMENT"
    val dateEpochDay: Long,
    val memo: String,
    val totalDebitPaisa: Long,
    val totalCreditPaisa: Long,
    val isBalanced: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_lines",
    indices = [
        Index(value = ["journalEntryId"]),
        Index(value = ["transactionId"]),
        Index(value = ["ledgerCode"]),
        Index(value = ["accountId"]),
        Index(value = ["contactId"]),
        Index(value = ["categoryId"]),
        Index(value = ["dateEpochDay"])
    ]
)
data class JournalLineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val journalEntryId: String,
    val transactionId: String,
    val ledgerCode: String,
    // Chart of Accounts codes:
    // "ASSET_ACCOUNT", "ASSET_RECEIVABLE", "LIABILITY_PAYABLE", "LIABILITY_LOAN",
    // "EQUITY_OPENING", "EQUITY_OWNER_CAPITAL", "EQUITY_OWNER_DRAWING",
    // "INCOME_CASH", "EXPENSE_CASH", "INCOME_UNREALIZED_CREDIT", "EXPENSE_UNREALIZED_CREDIT"
    val ledgerName: String,
    val accountId: String? = null,
    val contactId: String? = null,
    val categoryId: String? = null,
    val debitPaisa: Long,
    val creditPaisa: Long,
    val dateEpochDay: Long
)

@Entity(
    tableName = "attachments",
    indices = [Index(value = ["transactionId"])]
)
data class AttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val fileName: String,
    val mimeType: String, // "IMAGE", "PDF", "RECEIPT"
    val localPath: String,
    val fileSizeBytes: Long,
    val isMissingOrCorrupt: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val txType: String, // "CASH_EXPENSE", "CASH_INCOME", "CREDIT_EXPENSE", etc.
    val amountPaisa: Long,
    val accountId: String?,
    val categoryId: String?,
    val contactId: String? = null,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "CUSTOM_DAYS"
    val intervalDays: Int = 30,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val nextRunEpochDay: Long,
    val autoPostPolicy: String = "AUTO_POST", // "AUTO_POST" or "PENDING_CONFIRMATION"
    val isBillReminder: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recurring_runs",
    indices = [Index(value = ["idempotencyKey"], unique = true), Index(value = ["templateId"])]
)
data class RecurringRunEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val scheduledEpochDay: Long,
    val idempotencyKey: String, // "$templateId:$scheduledEpochDay"
    val generatedTransactionId: String? = null,
    val status: String, // "POSTED", "PENDING", "SKIPPED"
    val executedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [Index(value = ["dueEpochDay"]), Index(value = ["status"])]
)
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val reminderType: String, // "CUSTOMER_DUE", "SUPPLIER_PAYABLE", "RECURRING_BILL"
    val relatedTransactionId: String? = null,
    val relatedContactId: String? = null,
    val relatedTemplateId: String? = null,
    val amountPaisa: Long,
    val dueEpochDay: Long,
    val status: String = "UPCOMING", // "UPCOMING", "OVERDUE", "SNOOZED", "PAID", "DISMISSED"
    val snoozedUntilEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["transactionId"]), Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val action: String, // "CREATE", "EDIT_REVERSAL_REPLACE", "LOGICAL_DELETE_REVERSAL", "SETTLE_DUE", "BACKUP_RESTORE", "OPENING_EQUITY"
    val beforeSnapshotJson: String = "",
    val afterSnapshotJson: String = "",
    val reason: String = "",
    val actorName: String = "Owner",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "backup_history",
    indices = [Index(value = ["createdAt"])]
)
data class BackupRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val backupType: String, // "AUTOMATIC", "MANUAL"
    val localFilePath: String,
    val checksumSha256: String,
    val schemaVersion: Int = 1,
    val transactionCount: Int,
    val journalEntryCount: Int,
    val attachmentCount: Int,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
