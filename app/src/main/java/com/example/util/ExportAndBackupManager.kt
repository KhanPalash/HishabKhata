package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.AccountEntity
import com.example.data.AppConfigEntity
import com.example.data.AttachmentEntity
import com.example.data.AuditLogEntity
import com.example.data.BackupRecordEntity
import com.example.data.CategoryEntity
import com.example.data.ContactEntity
import com.example.data.JournalEntryEntity
import com.example.data.JournalLineEntity
import com.example.data.RecurringRunEntity
import com.example.data.RecurringTemplateEntity
import com.example.data.ReminderEntity
import com.example.data.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportExportRow(
    val col1: String,
    val col2: String,
    val col3: String,
    val col4: String
)

data class FullBackupSnapshot(
    val schemaVersion: Int,
    val checksumSha256: String,
    val config: AppConfigEntity,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val contacts: List<ContactEntity>,
    val transactions: List<TransactionEntity>,
    val journalEntries: List<JournalEntryEntity>,
    val journalLines: List<JournalLineEntity>,
    val attachments: List<AttachmentEntity>,
    val recurringTemplates: List<RecurringTemplateEntity>,
    val recurringRuns: List<RecurringRunEntity>,
    val reminders: List<ReminderEntity>,
    val auditLogs: List<AuditLogEntity>
)

object ExportAndBackupManager {
    private const val CHANNEL_ID = "hishabkhata_reminders"

    fun hashPin(pin: String): String {
        val clean = Formatters.normalizeDigitsToAscii(pin.trim())
        if (clean.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("hishabkhata_salt_v1:$clean".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateCsvReport(
        context: Context,
        reportTitle: String,
        businessName: String,
        periodLabel: String,
        summaryPairs: List<Pair<String, String>>,
        headers: List<String>,
        rows: List<ReportExportRow>
    ): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = reportTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "${safeName}_$stamp.csv")

        fun esc(v: String): String {
            val escaped = v.replace("\"", "\"\"")
            return "\"$escaped\""
        }

        val sb = StringBuilder()
        // UTF-8 BOM for proper Bangla rendering in spreadsheet apps
        sb.append("\uFEFF")
        sb.appendLine("${esc("Business")},${esc(businessName)}")
        sb.appendLine("${esc("Report")},${esc(reportTitle)}")
        sb.appendLine("${esc("Period")},${esc(periodLabel)}")
        sb.appendLine()
        for ((k, v) in summaryPairs) {
            sb.appendLine("${esc(k)},${esc(v)}")
        }
        sb.appendLine()
        sb.appendLine(headers.joinToString(",") { esc(it) })
        for (row in rows) {
            sb.appendLine("${esc(row.col1)},${esc(row.col2)},${esc(row.col3)},${esc(row.col4)}")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun generatePdfReport(
        context: Context,
        reportTitle: String,
        businessName: String,
        businessSubtitle: String,
        periodLabel: String,
        summaryPairs: List<Pair<String, String>>,
        headers: List<String>,
        rows: List<ReportExportRow>
    ): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = reportTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "${safeName}_$stamp.pdf")

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(6, 95, 70)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10.5f
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(209, 250, 229)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 45f

        // Header Banner
        canvas.drawText(businessName.ifBlank { "HishabKhata Business Ledger" }, 40f, y, titlePaint)
        y += 16f
        if (businessSubtitle.isNotBlank()) {
            canvas.drawText(businessSubtitle, 40f, y, subtitlePaint)
            y += 15f
        }
        canvas.drawText("$reportTitle  |  $periodLabel", 40f, y, boldPaint)
        y += 14f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Summary KPI Box
        for (chunk in summaryPairs.chunked(2)) {
            var x = 40f
            for ((label, value) in chunk) {
                canvas.drawText("$label:", x, y, subtitlePaint)
                canvas.drawText(value, x + 125f, y, boldPaint)
                x += 260f
            }
            y += 18f
        }
        y += 10f

        // Table Header helper
        fun drawTableHeaders(c: Canvas, topY: Float): Float {
            c.drawRect(40f, topY - 13f, 555f, topY + 7f, headerBgPaint)
            c.drawText(headers.getOrElse(0) { "Date / Item" }, 46f, topY, boldPaint)
            c.drawText(headers.getOrElse(1) { "Description" }, 155f, topY, boldPaint)
            c.drawText(headers.getOrElse(2) { "Category / Ref" }, 340f, topY, boldPaint)
            c.drawText(headers.getOrElse(3) { "Amount (BDT)" }, 455f, topY, boldPaint)
            return topY + 22f
        }

        y = drawTableHeaders(canvas, y)

        for (row in rows) {
            if (y > pageHeight - 55f) {
                pdf.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = 45f
                y = drawTableHeaders(canvas, y)
            }
            canvas.drawText(row.col1.take(18), 46f, y, bodyPaint)
            canvas.drawText(row.col2.take(30), 155f, y, bodyPaint)
            canvas.drawText(row.col3.take(18), 340f, y, bodyPaint)
            canvas.drawText(row.col4.take(18), 455f, y, boldPaint)
            y += 16f
            canvas.drawLine(40f, y - 11f, 555f, y - 11f, linePaint)
        }

        // Footer
        canvas.drawText(
            "Generated offline by HishabKhata Audit-Ready Ledger • Page $pageNumber",
            40f,
            pageHeight - 24f,
            subtitlePaint
        )
        pdf.finishPage(page)

        file.outputStream().use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Creates a verified local backup file with SHA-256 integrity checksum and schema version metadata.
     */
    fun writeBackupFile(
        context: Context,
        backupType: String, // "AUTOMATIC" or "MANUAL"
        config: AppConfigEntity,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        contacts: List<ContactEntity>,
        transactions: List<TransactionEntity>,
        journalEntries: List<JournalEntryEntity>,
        journalLines: List<JournalLineEntity>,
        attachments: List<AttachmentEntity>,
        recurringTemplates: List<RecurringTemplateEntity>,
        recurringRuns: List<RecurringRunEntity>,
        reminders: List<ReminderEntity>,
        auditLogs: List<AuditLogEntity>
    ): BackupRecordEntity {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val prefix = if (backupType == "AUTOMATIC") "auto_backup" else "manual_export"
        val fileName = "${prefix}_$stamp.hkbak"
        val file = File(dir, fileName)

        val payload = JSONObject().apply {
            put("config", JSONObject().apply {
                put("language", config.language)
                put("themeMode", config.themeMode)
                put("ownerName", config.ownerName)
                put("ownerPhone", config.ownerPhone)
                put("businessName", config.businessName)
                put("businessType", config.businessType)
                put("businessAddress", config.businessAddress)
                put("businessContact", config.businessContact)
                put("currencyCode", config.currencyCode)
                put("currencySymbol", config.currencySymbol)
                put("numberFormat", config.numberFormat)
                put("dateFormat", config.dateFormat)
                put("pinHash", config.pinHash)
                put("pinHint", config.pinHint)
                put("biometricEnabled", config.biometricEnabled)
                put("autoBackupEnabled", config.autoBackupEnabled)
                put("lastUsedAccountId", config.lastUsedAccountId)
            })
            put("accounts", JSONArray().apply {
                accounts.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("name", a.name)
                        put("nameBn", a.nameBn)
                        put("accountType", a.accountType)
                        put("accountNumber", a.accountNumber)
                        put("openingBalancePaisa", a.openingBalancePaisa)
                        put("openingDateEpochDay", a.openingDateEpochDay)
                        put("isArchived", a.isArchived)
                        put("createdAt", a.createdAt)
                    })
                }
            })
            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("nameBn", c.nameBn)
                        put("categoryType", c.categoryType)
                        put("iconName", c.iconName)
                        put("colorHex", c.colorHex)
                        put("isArchived", c.isArchived)
                        put("usageCount", c.usageCount)
                        put("lastUsedAt", c.lastUsedAt)
                    })
                }
            })
            put("contacts", JSONArray().apply {
                contacts.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("address", c.address)
                        put("notes", c.notes)
                        put("contactType", c.contactType)
                        put("isArchived", c.isArchived)
                        put("createdAt", c.createdAt)
                    })
                }
            })
            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("txType", t.txType)
                        put("amountPaisa", t.amountPaisa)
                        put("settledAmountPaisa", t.settledAmountPaisa)
                        put("dueAmountPaisa", t.dueAmountPaisa)
                        put("loanInterestPaisa", t.loanInterestPaisa)
                        put("accountId", t.accountId ?: "")
                        put("toAccountId", t.toAccountId ?: "")
                        put("categoryId", t.categoryId ?: "")
                        put("contactId", t.contactId ?: "")
                        put("parentTransactionId", t.parentTransactionId ?: "")
                        put("dateEpochDay", t.dateEpochDay)
                        put("dueDateEpochDay", t.dueDateEpochDay ?: -1L)
                        put("title", t.title)
                        put("referenceCode", t.referenceCode)
                        put("paymentDetails", t.paymentDetails)
                        put("internalNote", t.internalNote)
                        put("status", t.status)
                        put("version", t.version)
                        put("createdAt", t.createdAt)
                        put("updatedAt", t.updatedAt)
                    })
                }
            })
            put("journalEntries", JSONArray().apply {
                journalEntries.forEach { j ->
                    put(JSONObject().apply {
                        put("id", j.id)
                        put("transactionId", j.transactionId)
                        put("entryType", j.entryType)
                        put("dateEpochDay", j.dateEpochDay)
                        put("memo", j.memo)
                        put("totalDebitPaisa", j.totalDebitPaisa)
                        put("totalCreditPaisa", j.totalCreditPaisa)
                        put("isBalanced", j.isBalanced)
                        put("createdAt", j.createdAt)
                    })
                }
            })
            put("journalLines", JSONArray().apply {
                journalLines.forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id)
                        put("journalEntryId", l.journalEntryId)
                        put("transactionId", l.transactionId)
                        put("ledgerCode", l.ledgerCode)
                        put("ledgerName", l.ledgerName)
                        put("accountId", l.accountId ?: "")
                        put("contactId", l.contactId ?: "")
                        put("categoryId", l.categoryId ?: "")
                        put("debitPaisa", l.debitPaisa)
                        put("creditPaisa", l.creditPaisa)
                        put("dateEpochDay", l.dateEpochDay)
                    })
                }
            })
            put("attachments", JSONArray().apply {
                attachments.forEach { at ->
                    put(JSONObject().apply {
                        put("id", at.id)
                        put("transactionId", at.transactionId)
                        put("fileName", at.fileName)
                        put("mimeType", at.mimeType)
                        put("localPath", at.localPath)
                        put("fileSizeBytes", at.fileSizeBytes)
                        put("isMissingOrCorrupt", at.isMissingOrCorrupt)
                        put("createdAt", at.createdAt)
                    })
                }
            })
            put("recurringTemplates", JSONArray().apply {
                recurringTemplates.forEach { rt ->
                    put(JSONObject().apply {
                        put("id", rt.id)
                        put("title", rt.title)
                        put("txType", rt.txType)
                        put("amountPaisa", rt.amountPaisa)
                        put("accountId", rt.accountId ?: "")
                        put("categoryId", rt.categoryId ?: "")
                        put("contactId", rt.contactId ?: "")
                        put("frequency", rt.frequency)
                        put("intervalDays", rt.intervalDays)
                        put("startDateEpochDay", rt.startDateEpochDay)
                        put("endDateEpochDay", rt.endDateEpochDay ?: -1L)
                        put("nextRunEpochDay", rt.nextRunEpochDay)
                        put("autoPostPolicy", rt.autoPostPolicy)
                        put("isBillReminder", rt.isBillReminder)
                        put("isActive", rt.isActive)
                        put("createdAt", rt.createdAt)
                    })
                }
            })
            put("recurringRuns", JSONArray().apply {
                recurringRuns.forEach { rr ->
                    put(JSONObject().apply {
                        put("id", rr.id)
                        put("templateId", rr.templateId)
                        put("scheduledEpochDay", rr.scheduledEpochDay)
                        put("idempotencyKey", rr.idempotencyKey)
                        put("generatedTransactionId", rr.generatedTransactionId ?: "")
                        put("status", rr.status)
                        put("executedAt", rr.executedAt)
                    })
                }
            })
            put("reminders", JSONArray().apply {
                reminders.forEach { rm ->
                    put(JSONObject().apply {
                        put("id", rm.id)
                        put("title", rm.title)
                        put("reminderType", rm.reminderType)
                        put("relatedTransactionId", rm.relatedTransactionId ?: "")
                        put("relatedContactId", rm.relatedContactId ?: "")
                        put("relatedTemplateId", rm.relatedTemplateId ?: "")
                        put("amountPaisa", rm.amountPaisa)
                        put("dueEpochDay", rm.dueEpochDay)
                        put("status", rm.status)
                        put("snoozedUntilEpochDay", rm.snoozedUntilEpochDay ?: -1L)
                        put("createdAt", rm.createdAt)
                    })
                }
            })
            put("auditLogs", JSONArray().apply {
                auditLogs.forEach { al ->
                    put(JSONObject().apply {
                        put("id", al.id)
                        put("transactionId", al.transactionId)
                        put("action", al.action)
                        put("beforeSnapshotJson", al.beforeSnapshotJson)
                        put("afterSnapshotJson", al.afterSnapshotJson)
                        put("reason", al.reason)
                        put("actorName", al.actorName)
                        put("timestamp", al.timestamp)
                    })
                }
            })
        }

        val payloadStr = payload.toString()
        val checksum = sha256(payloadStr)

        val envelope = JSONObject().apply {
            put("schemaVersion", 1)
            put("backupType", backupType)
            put("createdAt", System.currentTimeMillis())
            put("checksumSha256", checksum)
            put("payload", payloadStr)
        }

        val contentBytes = envelope.toString(2).toByteArray(Charsets.UTF_8)
        file.writeBytes(contentBytes)

        return BackupRecordEntity(
            fileName = fileName,
            backupType = backupType,
            localFilePath = file.absolutePath,
            checksumSha256 = checksum,
            schemaVersion = 1,
            transactionCount = transactions.size,
            journalEntryCount = journalEntries.size,
            attachmentCount = attachments.size,
            fileSizeBytes = contentBytes.size.toLong()
        )
    }

    /**
     * Validates backup file integrity (schemaVersion == 1 & SHA-256 match) and parses all entities.
     * Throws IllegalArgumentException if corrupt or schema mismatch so caller leaves DB untouched.
     */
    fun parseAndVerifyBackupFile(file: File, existingConfig: AppConfigEntity): FullBackupSnapshot {
        if (!file.exists()) throw IllegalArgumentException("Backup file does not exist on disk.")
        val raw = file.readText(Charsets.UTF_8)
        val envelope = JSONObject(raw)
        val schemaVersion = envelope.optInt("schemaVersion", -1)
        if (schemaVersion != 1) {
            throw IllegalArgumentException("Unsupported schema version: $schemaVersion (expected 1).")
        }
        val expectedChecksum = envelope.optString("checksumSha256", "")
        val payloadStr = envelope.optString("payload", "")
        if (expectedChecksum.isBlank() || payloadStr.isBlank()) {
            throw IllegalArgumentException("Backup file is missing checksum or payload.")
        }
        val actualChecksum = sha256(payloadStr)
        if (actualChecksum != expectedChecksum) {
            throw IllegalArgumentException("Backup checksum mismatch! File may be corrupted.")
        }

        val payload = JSONObject(payloadStr)
        val cfgObj = payload.optJSONObject("config") ?: JSONObject()
        val restoredConfig = existingConfig.copy(
            isOnboardingCompleted = true,
            language = cfgObj.optString("language", existingConfig.language),
            themeMode = cfgObj.optString("themeMode", existingConfig.themeMode),
            ownerName = cfgObj.optString("ownerName", existingConfig.ownerName),
            ownerPhone = cfgObj.optString("ownerPhone", existingConfig.ownerPhone),
            businessName = cfgObj.optString("businessName", existingConfig.businessName),
            businessType = cfgObj.optString("businessType", existingConfig.businessType),
            businessAddress = cfgObj.optString("businessAddress", existingConfig.businessAddress),
            businessContact = cfgObj.optString("businessContact", existingConfig.businessContact),
            currencyCode = cfgObj.optString("currencyCode", "BDT"),
            currencySymbol = cfgObj.optString("currencySymbol", "৳"),
            numberFormat = cfgObj.optString("numberFormat", existingConfig.numberFormat),
            dateFormat = cfgObj.optString("dateFormat", existingConfig.dateFormat),
            lastUsedAccountId = cfgObj.optString("lastUsedAccountId", existingConfig.lastUsedAccountId)
        )

        fun optNullStr(o: JSONObject, key: String): String? {
            val s = o.optString(key, "")
            return s.ifBlank { null }
        }

        val accounts = mutableListOf<AccountEntity>()
        val accArr = payload.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArr.length()) {
            val o = accArr.getJSONObject(i)
            accounts.add(
                AccountEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    nameBn = o.optString("nameBn", o.getString("name")),
                    accountType = o.getString("accountType"),
                    accountNumber = o.optString("accountNumber", ""),
                    openingBalancePaisa = o.optLong("openingBalancePaisa", 0L),
                    openingDateEpochDay = o.optLong("openingDateEpochDay", 0L),
                    isArchived = o.optBoolean("isArchived", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val categories = mutableListOf<CategoryEntity>()
        val catArr = payload.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            categories.add(
                CategoryEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    nameBn = o.optString("nameBn", o.getString("name")),
                    categoryType = o.getString("categoryType"),
                    iconName = o.optString("iconName", "Category"),
                    colorHex = o.optString("colorHex", "#10B981"),
                    isArchived = o.optBoolean("isArchived", false),
                    usageCount = o.optInt("usageCount", 0),
                    lastUsedAt = o.optLong("lastUsedAt", 0L)
                )
            )
        }

        val contacts = mutableListOf<ContactEntity>()
        val conArr = payload.optJSONArray("contacts") ?: JSONArray()
        for (i in 0 until conArr.length()) {
            val o = conArr.getJSONObject(i)
            contacts.add(
                ContactEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    phone = o.optString("phone", ""),
                    address = o.optString("address", ""),
                    notes = o.optString("notes", ""),
                    contactType = o.getString("contactType"),
                    isArchived = o.optBoolean("isArchived", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val transactions = mutableListOf<TransactionEntity>()
        val txArr = payload.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val dueDay = o.optLong("dueDateEpochDay", -1L)
            transactions.add(
                TransactionEntity(
                    id = o.getString("id"),
                    txType = o.getString("txType"),
                    amountPaisa = o.getLong("amountPaisa"),
                    settledAmountPaisa = o.getLong("settledAmountPaisa"),
                    dueAmountPaisa = o.getLong("dueAmountPaisa"),
                    loanInterestPaisa = o.optLong("loanInterestPaisa", 0L),
                    accountId = optNullStr(o, "accountId"),
                    toAccountId = optNullStr(o, "toAccountId"),
                    categoryId = optNullStr(o, "categoryId"),
                    contactId = optNullStr(o, "contactId"),
                    parentTransactionId = optNullStr(o, "parentTransactionId"),
                    dateEpochDay = o.getLong("dateEpochDay"),
                    dueDateEpochDay = if (dueDay > 0L) dueDay else null,
                    title = o.getString("title"),
                    referenceCode = o.optString("referenceCode", ""),
                    paymentDetails = o.optString("paymentDetails", ""),
                    internalNote = o.optString("internalNote", ""),
                    status = o.optString("status", "POSTED"),
                    version = o.optInt("version", 1),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val journalEntries = mutableListOf<JournalEntryEntity>()
        val jeArr = payload.optJSONArray("journalEntries") ?: JSONArray()
        for (i in 0 until jeArr.length()) {
            val o = jeArr.getJSONObject(i)
            journalEntries.add(
                JournalEntryEntity(
                    id = o.getString("id"),
                    transactionId = o.getString("transactionId"),
                    entryType = o.getString("entryType"),
                    dateEpochDay = o.getLong("dateEpochDay"),
                    memo = o.optString("memo", ""),
                    totalDebitPaisa = o.getLong("totalDebitPaisa"),
                    totalCreditPaisa = o.getLong("totalCreditPaisa"),
                    isBalanced = o.optBoolean("isBalanced", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val journalLines = mutableListOf<JournalLineEntity>()
        val jlArr = payload.optJSONArray("journalLines") ?: JSONArray()
        for (i in 0 until jlArr.length()) {
            val o = jlArr.getJSONObject(i)
            journalLines.add(
                JournalLineEntity(
                    id = o.getString("id"),
                    journalEntryId = o.getString("journalEntryId"),
                    transactionId = o.getString("transactionId"),
                    ledgerCode = o.getString("ledgerCode"),
                    ledgerName = o.getString("ledgerName"),
                    accountId = optNullStr(o, "accountId"),
                    contactId = optNullStr(o, "contactId"),
                    categoryId = optNullStr(o, "categoryId"),
                    debitPaisa = o.getLong("debitPaisa"),
                    creditPaisa = o.getLong("creditPaisa"),
                    dateEpochDay = o.getLong("dateEpochDay")
                )
            )
        }

        val attachments = mutableListOf<AttachmentEntity>()
        val atArr = payload.optJSONArray("attachments") ?: JSONArray()
        for (i in 0 until atArr.length()) {
            val o = atArr.getJSONObject(i)
            val localPath = o.optString("localPath", "")
            val exists = localPath.isNotBlank() && File(localPath).exists()
            attachments.add(
                AttachmentEntity(
                    id = o.getString("id"),
                    transactionId = o.getString("transactionId"),
                    fileName = o.getString("fileName"),
                    mimeType = o.getString("mimeType"),
                    localPath = localPath,
                    fileSizeBytes = o.optLong("fileSizeBytes", 0L),
                    isMissingOrCorrupt = !exists,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val recurringTemplates = mutableListOf<RecurringTemplateEntity>()
        val rtArr = payload.optJSONArray("recurringTemplates") ?: JSONArray()
        for (i in 0 until rtArr.length()) {
            val o = rtArr.getJSONObject(i)
            val endDay = o.optLong("endDateEpochDay", -1L)
            recurringTemplates.add(
                RecurringTemplateEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    txType = o.getString("txType"),
                    amountPaisa = o.getLong("amountPaisa"),
                    accountId = optNullStr(o, "accountId"),
                    categoryId = optNullStr(o, "categoryId"),
                    contactId = optNullStr(o, "contactId"),
                    frequency = o.getString("frequency"),
                    intervalDays = o.optInt("intervalDays", 30),
                    startDateEpochDay = o.getLong("startDateEpochDay"),
                    endDateEpochDay = if (endDay > 0L) endDay else null,
                    nextRunEpochDay = o.getLong("nextRunEpochDay"),
                    autoPostPolicy = o.optString("autoPostPolicy", "AUTO_POST"),
                    isBillReminder = o.optBoolean("isBillReminder", true),
                    isActive = o.optBoolean("isActive", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val recurringRuns = mutableListOf<RecurringRunEntity>()
        val rrArr = payload.optJSONArray("recurringRuns") ?: JSONArray()
        for (i in 0 until rrArr.length()) {
            val o = rrArr.getJSONObject(i)
            recurringRuns.add(
                RecurringRunEntity(
                    id = o.getString("id"),
                    templateId = o.getString("templateId"),
                    scheduledEpochDay = o.getLong("scheduledEpochDay"),
                    idempotencyKey = o.getString("idempotencyKey"),
                    generatedTransactionId = optNullStr(o, "generatedTransactionId"),
                    status = o.getString("status"),
                    executedAt = o.optLong("executedAt", System.currentTimeMillis())
                )
            )
        }

        val reminders = mutableListOf<ReminderEntity>()
        val rmArr = payload.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until rmArr.length()) {
            val o = rmArr.getJSONObject(i)
            val snz = o.optLong("snoozedUntilEpochDay", -1L)
            reminders.add(
                ReminderEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    reminderType = o.getString("reminderType"),
                    relatedTransactionId = optNullStr(o, "relatedTransactionId"),
                    relatedContactId = optNullStr(o, "relatedContactId"),
                    relatedTemplateId = optNullStr(o, "relatedTemplateId"),
                    amountPaisa = o.getLong("amountPaisa"),
                    dueEpochDay = o.getLong("dueEpochDay"),
                    status = o.getString("status"),
                    snoozedUntilEpochDay = if (snz > 0L) snz else null,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val auditLogs = mutableListOf<AuditLogEntity>()
        val alArr = payload.optJSONArray("auditLogs") ?: JSONArray()
        for (i in 0 until alArr.length()) {
            val o = alArr.getJSONObject(i)
            auditLogs.add(
                AuditLogEntity(
                    id = o.getString("id"),
                    transactionId = o.getString("transactionId"),
                    action = o.getString("action"),
                    beforeSnapshotJson = o.optString("beforeSnapshotJson", ""),
                    afterSnapshotJson = o.optString("afterSnapshotJson", ""),
                    reason = o.optString("reason", ""),
                    actorName = o.optString("actorName", "Owner"),
                    timestamp = o.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        return FullBackupSnapshot(
            schemaVersion = schemaVersion,
            checksumSha256 = actualChecksum,
            config = restoredConfig,
            accounts = accounts,
            categories = categories,
            contacts = contacts,
            transactions = transactions,
            journalEntries = journalEntries,
            journalLines = journalLines,
            attachments = attachments,
            recurringTemplates = recurringTemplates,
            recurringRuns = recurringRuns,
            reminders = reminders,
            auditLogs = auditLogs
        )
    }

    fun sendLocalReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Due & Recurring Bill Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Offline local notifications for receivables, payables, and recurring obligations"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
