package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.BackupRecordEntity
import com.example.data.CategoryEntity
import com.example.data.RecurringTemplateEntity
import com.example.data.ReminderEntity
import com.example.ui.LedgerUiState
import com.example.ui.LedgerViewModel
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.ReceivableAmber
import com.example.util.ExportAndBackupManager
import com.example.util.Formatters
import java.io.File

@Composable
fun AutomationAndRemindersScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat

    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var pendingNotificationReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingNotificationReminder?.let { viewModel.triggerLocalNotificationTest(it) }
        pendingNotificationReminder = null
    }

    val activeAccountId = uiState.accounts.firstOrNull { !it.isArchived }?.id

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("automation_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formatters.tr(lang, "Recurring Schedules & Due Reminders", "অটোমেটিক বিল ও তাগাদা রিমাইন্ডার"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Rent, payroll, utilities, receivables & idempotent auto-runs",
                                    "দোকান ভাড়া, বেতন, বিদ্যুৎ বিল ও বাকির রিমাইন্ডার"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showAddRecurringDialog = true },
                            modifier = Modifier.testTag("add_recurring_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Schedule", "নতুন শিডিউল"))
                        }
                    }
                }
            }
        }

        // 2. Due & Recurring Bill Reminders Section
        item {
            Text(
                text = Formatters.tr(
                    lang,
                    "Due & Bill Reminders (${uiState.reminders.size})",
                    "বাকি ও বিল রিমাইন্ডার (${uiState.reminders.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.reminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = Formatters.tr(
                            lang,
                            "No active reminders. Credit sales/purchases and recurring bills automatically appear here.",
                            "কোনো রিমাইন্ডার নেই। বাকি লেনদেন এবং মাসিক বিল স্বয়ংক্রিয়ভাবে এখানে যুক্ত হবে।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(uiState.reminders, key = { it.id }) { rem ->
                val statusColor = when (rem.status) {
                    "PAID" -> CashInGreen
                    "OVERDUE" -> CashOutRed
                    "DISMISSED" -> MaterialTheme.colorScheme.outline
                    else -> ReceivableAmber
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rem.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${rem.reminderType.replace("_", " ")} • Due: ${Formatters.formatEpochDay(rem.dueEpochDay, dateFmt, lang)} • [${rem.status}]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor
                                )
                            }
                            Text(
                                text = Formatters.formatBdt(rem.amountPaisa, lang, numFmt),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (rem.status != "PAID" && rem.status != "DISMISSED") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.handleReminderAction(
                                            reminder = rem,
                                            newStatus = "PAID",
                                            payFromAccountId = activeAccountId
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Formatters.tr(lang, "Mark Paid", "পরিশোধিত"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.handleReminderAction(rem, "SNOOZED", snoozeDays = 3)
                                    }
                                ) {
                                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Formatters.tr(lang, "Snooze +3d", "৩ দিন পিছান"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            pendingNotificationReminder = rem
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.triggerLocalNotificationTest(rem)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Formatters.tr(lang, "Notify", "নোটিফিকেশন"))
                                }
                                TextButton(
                                    onClick = { viewModel.handleReminderAction(rem, "DISMISSED") }
                                ) {
                                    Text(Formatters.tr(lang, "Dismiss", "বাদ দিন"))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Recurring Transaction Templates Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Formatters.tr(
                    lang,
                    "Recurring Transaction Templates (${uiState.recurringTemplates.size})",
                    "পৌনঃপুনিক লেনদেন টেমপ্লেট (${uiState.recurringTemplates.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.recurringTemplates, key = { it.id }) { tmpl ->
            val runsCount = uiState.recurringRuns.count { it.templateId == tmpl.id }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tmpl.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tmpl.frequency} • ${tmpl.autoPostPolicy} • Next Run: ${Formatters.formatEpochDay(tmpl.nextRunEpochDay, dateFmt, lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Idempotent Runs Recorded: $runsCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = EquityBlue
                            )
                        }
                        Text(
                            text = Formatters.formatBdt(tmpl.amountPaisa, lang, numFmt),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.runRecurringTemplateNow(tmpl) }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Formatters.tr(lang, "Run Scheduled Occurrence Now", "এখনই রান করুন"))
                    }
                }
            }
        }
    }

    if (showAddRecurringDialog) {
        var title by remember { mutableStateOf("Shop Monthly Rent") }
        var amountStr by remember { mutableStateOf("15000") }
        var txType by remember { mutableStateOf("CASH_EXPENSE") }
        var frequency by remember { mutableStateOf("MONTHLY") }
        var intervalDays by remember { mutableStateOf("30") }
        var autoPolicy by remember { mutableStateOf("AUTO_POST") }

        AlertDialog(
            onDismissRequest = { showAddRecurringDialog = false },
            title = { Text(Formatters.tr(lang, "New Recurring Schedule & Bill Reminder", "নতুন পৌনঃপুনিক বিল ও রিমাইন্ডার")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Obligation Title (Rent, Payroll, Utility) *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (BDT) *") },
                        prefix = { Text("৳ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = txType == "CASH_EXPENSE",
                            onClick = { txType = "CASH_EXPENSE" },
                            label = { Text("Expense") }
                        )
                        FilterChip(
                            selected = txType == "CASH_INCOME",
                            onClick = { txType = "CASH_INCOME" },
                            label = { Text("Income") }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY", "CUSTOM_DAYS").forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f) }
                            )
                        }
                    }
                    if (frequency == "CUSTOM_DAYS") {
                        OutlinedTextField(
                            value = intervalDays,
                            onValueChange = { intervalDays = it },
                            label = { Text("Custom Interval (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = autoPolicy == "AUTO_POST",
                            onClick = { autoPolicy = "AUTO_POST" },
                            label = { Text("Auto-Post on Due") }
                        )
                        FilterChip(
                            selected = autoPolicy == "PENDING_CONFIRMATION",
                            onClick = { autoPolicy = "PENDING_CONFIRMATION" },
                            label = { Text("Pending Confirmation") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = Formatters.parseTakaToPaisa(amountStr) ?: 0L
                        if (title.isNotBlank() && amt > 0L) {
                            val today = Formatters.currentEpochDay()
                            val tmpl = RecurringTemplateEntity(
                                title = title.trim(),
                                txType = txType,
                                amountPaisa = amt,
                                accountId = activeAccountId,
                                categoryId = uiState.categories.firstOrNull()?.id,
                                frequency = frequency,
                                intervalDays = intervalDays.toIntOrNull() ?: 30,
                                startDateEpochDay = today,
                                nextRunEpochDay = today,
                                autoPostPolicy = autoPolicy,
                                isBillReminder = true
                            )
                            viewModel.saveRecurringTemplate(tmpl)
                            showAddRecurringDialog = false
                        }
                    }
                ) {
                    Text(Formatters.tr(lang, "Save Schedule", "সংরক্ষণ করুন"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecurringDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsAndBackupScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    val context = LocalContext.current
    val cfg = uiState.config
    val lang = cfg.language
    val dateFmt = cfg.dateFormat

    var restoreTarget by remember { mutableStateOf<BackupRecordEntity?>(null) }
    var restorePinInput by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val autoBackups = uiState.backupHistory.filter { it.backupType == "AUTOMATIC" }
    val manualBackups = uiState.backupHistory.filter { it.backupType == "MANUAL" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Local Backup & 7-Generation Rotation Center (Feature 11 & FR-09)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Local Backup & Restore (SHA-256 Verified)",
                                    "লোকাল ব্যাকআপ ও রিস্টোর (SHA-256 যাচাইকৃত)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Automatic Backups retained: ${autoBackups.size}/7 • Manual Exports retained: ${manualBackups.size}",
                                    "অটো ব্যাকআপ: ${autoBackups.size}/৭ • ম্যানুয়াল ব্যাকআপ: ${manualBackups.size}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.createManualOrAutoBackup("MANUAL") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_manual_backup_btn")
                        ) {
                            Text(Formatters.tr(lang, "+ Manual Export", "+ ম্যানুয়াল ব্যাকআপ"))
                        }
                        OutlinedButton(
                            onClick = { viewModel.createManualOrAutoBackup("AUTOMATIC") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_auto_backup_btn")
                        ) {
                            Text(Formatters.tr(lang, "Run Auto (Max 7)", "অটো ব্যাকআপ রান"))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.backupHistory.take(10).forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${rec.backupType} • ${rec.fileName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${rec.transactionCount} txs | ${rec.journalEntryCount} journals | SHA-256: ${rec.checksumSha256.take(10)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    TextButton(
                                        onClick = {
                                            restoreTarget = rec
                                            restorePinInput = ""
                                            restoreError = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Formatters.tr(lang, "Restore", "রিস্টোর"))
                                    }
                                    TextButton(
                                        onClick = {
                                            ExportAndBackupManager.shareFile(
                                                context = context,
                                                file = File(rec.localFilePath),
                                                mimeType = "application/octet-stream",
                                                chooserTitle = "Share HishabKhata Backup"
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Backup", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Language, Theme & BDT Format Preferences (Feature 13)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Formatters.tr(lang, "Language, Theme & BDT Formatting", "ভাষা, থিম এবং টাকার ফরম্যাট"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = lang == "EN",
                            onClick = { if (lang != "EN") viewModel.toggleLanguage() },
                            label = { Text("English (EN)") }
                        )
                        FilterChip(
                            selected = lang == "BN",
                            onClick = { if (lang != "BN") viewModel.toggleLanguage() },
                            label = { Text("বাংলা (BN)") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System Theme", "LIGHT" to "Light Theme", "DARK" to "Dark Theme").forEach { (m, lbl) ->
                            FilterChip(
                                selected = cfg.themeMode == m,
                                onClick = { viewModel.updateThemeMode(m) },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = cfg.numberFormat == "BD_LAKH",
                            onClick = { viewModel.updateNumberAndDateFormat("BD_LAKH", cfg.dateFormat) },
                            label = { Text("৳ 1,25,000 (Lakh)") }
                        )
                        FilterChip(
                            selected = cfg.numberFormat == "INTL",
                            onClick = { viewModel.updateNumberAndDateFormat("INTL", cfg.dateFormat) },
                            label = { Text("৳ 125,000 (Intl)") }
                        )
                    }
                }
            }
        }

        // 3. Editable Categories Manager
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Formatters.tr(
                                lang,
                                "Income & Expense Categories (${uiState.categories.size})",
                                "আয় ও ব্যয়ের ক্যাটাগরি (${uiState.categories.size})"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "+ Category", "+ ক্যাটাগরি"))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.categories.joinToString(" • ") {
                            if (lang == "BN") it.nameBn else it.name
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Business Profile & Security PIN Lock Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Formatters.tr(lang, "Business Profile & Security Controls", "ব্যবসার প্রোফাইল ও নিরাপত্তা"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${cfg.businessName} (${cfg.businessType}) • Owner: ${cfg.ownerName} (${cfg.ownerPhone})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Formatters.tr(lang, "Edit Business Profile", "প্রোফাইল সংশোধন"))
                        }
                        Button(
                            onClick = { viewModel.lockAppNow() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lock_app_now_btn")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Lock Ledger Now", "অ্যাপ লক করুন"))
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog with Overwrite Warning & PIN Verification (Feature 11 & FR-09)
    if (restoreTarget != null) {
        val target = restoreTarget!!
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text(Formatters.tr(lang, "Confirm Backup Restore (Overwrites Data)", "ব্যাকআপ রিস্টোর নিশ্চিতকরণ")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = Formatters.tr(
                            lang,
                            "Warning: Restoring '${target.fileName}' will replace current local database tables after verifying SHA-256 checksum & schema version.",
                            "সতর্কতা: '${target.fileName}' রিস্টোর করলে বর্তমান ডাটা প্রতিস্থাপিত হবে।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CashOutRed
                    )
                    if (cfg.pinHash.isNotBlank()) {
                        OutlinedTextField(
                            value = restorePinInput,
                            onValueChange = { restorePinInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Enter Security PIN to Confirm Restore *") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true
                        )
                    }
                    if (restoreError != null) {
                        Text(restoreError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackupWithPinCheck(target, restorePinInput) { ok, msg ->
                            if (ok) {
                                restoreTarget = null
                            } else {
                                restoreError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashOutRed)
                ) {
                    Text(Formatters.tr(lang, "Verify Checksum & Restore", "রিস্টোর নিশ্চিত করুন"))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var nameEn by remember { mutableStateOf("") }
        var nameBn by remember { mutableStateOf("") }
        var catType by remember { mutableStateOf("EXPENSE") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = catType == "INCOME",
                            onClick = { catType = "INCOME" },
                            label = { Text("Income") }
                        )
                        FilterChip(
                            selected = catType == "EXPENSE",
                            onClick = { catType = "EXPENSE" },
                            label = { Text("Expense") }
                        )
                    }
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("Category Name (English) *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text("Category Name (বাংলা)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            viewModel.saveCategory(
                                CategoryEntity(
                                    name = nameEn.trim(),
                                    nameBn = nameBn.trim().ifBlank { nameEn.trim() },
                                    categoryType = catType
                                )
                            )
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditProfileDialog) {
        var ownerName by remember { mutableStateOf(cfg.ownerName) }
        var ownerPhone by remember { mutableStateOf(cfg.ownerPhone) }
        var bizName by remember { mutableStateOf(cfg.businessName) }
        var bizType by remember { mutableStateOf(cfg.businessType) }
        var bizAddr by remember { mutableStateOf(cfg.businessAddress) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Business & Owner Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true)
                    OutlinedTextField(value = ownerPhone, onValueChange = { ownerPhone = it }, label = { Text("Owner Phone") }, singleLine = true)
                    OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name") }, singleLine = true)
                    OutlinedTextField(value = bizType, onValueChange = { bizType = it }, label = { Text("Business Type") }, singleLine = true)
                    OutlinedTextField(value = bizAddr, onValueChange = { bizAddr = it }, label = { Text("Business Address") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBusinessAndOwnerProfile(ownerName, ownerPhone, bizName, bizType, bizAddr, cfg.businessContact)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
