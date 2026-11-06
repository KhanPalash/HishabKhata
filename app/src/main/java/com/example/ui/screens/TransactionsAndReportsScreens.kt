package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TransactionEntity
import com.example.ui.LedgerUiState
import com.example.ui.LedgerViewModel
import com.example.ui.PeriodPreset
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PayablePurple
import com.example.ui.theme.ReceivableAmber
import com.example.util.ExportAndBackupManager
import com.example.util.Formatters

@Composable
fun TransactionsAndAuditScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel,
    onInspectTransaction: (TransactionEntity) -> Unit
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat

    var subTab by remember { mutableIntStateOf(0) } // 0 = Transactions, 1 = Journal Lines, 2 = Audit Log
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("ALL") }
    var includeDeleted by remember { mutableStateOf(true) }

    val filteredTransactions = uiState.transactions.filter { tx ->
        val matchesDeleted = includeDeleted || tx.status != "DELETED"
        val matchesType = typeFilter == "ALL" || tx.txType == typeFilter
        val matchesSearch = searchQuery.isBlank() ||
            tx.title.contains(searchQuery, ignoreCase = true) ||
            tx.referenceCode.contains(searchQuery, ignoreCase = true) ||
            tx.txType.contains(searchQuery, ignoreCase = true)
        matchesDeleted && matchesType && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Formatters.tr(
                            lang,
                            "Ledger Register, Journal & Audit Trail",
                            "লেনদেন রেজিস্টার, জার্নাল ও অডিট লগ"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = subTab == 0,
                            onClick = { subTab = 0 },
                            label = { Text(Formatters.tr(lang, "Transactions (${uiState.transactions.size})", "লেনদেন (${uiState.transactions.size})")) }
                        )
                        FilterChip(
                            selected = subTab == 1,
                            onClick = { subTab = 1 },
                            label = { Text(Formatters.tr(lang, "Journal (${uiState.journalEntries.size})", "জার্নাল (${uiState.journalEntries.size})")) }
                        )
                        FilterChip(
                            selected = subTab == 2,
                            onClick = { subTab = 2 },
                            label = { Text(Formatters.tr(lang, "Audit Log (${uiState.auditLogs.size})", "অডিট লগ (${uiState.auditLogs.size})")) }
                        )
                    }

                    if (subTab == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(Formatters.tr(lang, "Search Title, Reference or Type", "মেমো, রেফারেন্স বা ধরন খুঁজুন")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "CASH_INCOME", "CREDIT_INCOME", "CASH_EXPENSE", "CREDIT_EXPENSE", "RECEIVE_DUE", "PAY_DUE", "TRANSFER", "OPENING_BALANCE").forEach { t ->
                                FilterChip(
                                    selected = typeFilter == t,
                                    onClick = { typeFilter = t },
                                    label = { Text(t.replace("_", " ")) }
                                )
                            }
                        }
                    }
                }
            }
        }

        when (subTab) {
            0 -> {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        tx = tx,
                        lang = lang,
                        numFmt = numFmt,
                        dateFmt = dateFmt,
                        onClick = { onInspectTransaction(tx) }
                    )
                }
            }

            1 -> {
                items(uiState.journalEntries, key = { it.id }) { je ->
                    val entryLines = uiState.journalLines.filter { it.journalEntryId == je.id }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${je.entryType} • ${je.memo}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Formatters.formatEpochDay(je.dateEpochDay, dateFmt, lang),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            entryLines.forEach { ln ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${if (ln.debitPaisa > 0) "DR" else "CR"} • ${ln.ledgerName} [${ln.ledgerCode}]",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = Formatters.formatBdt(maxOf(ln.debitPaisa, ln.creditPaisa), lang, numFmt),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                items(uiState.auditLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${log.action} • Actor: ${log.actorName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Formatters.formatTimestamp(log.timestamp, lang),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${log.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (log.beforeSnapshotJson.isNotBlank() && log.beforeSnapshotJson != "{}") {
                                Text(
                                    text = "Before: ${log.beforeSnapshotJson}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (log.afterSnapshotJson.isNotBlank()) {
                                Text(
                                    text = "After: ${log.afterSnapshotJson}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsAndExportsScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    val context = LocalContext.current
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat
    val summary = uiState.currentPeriodSummary
    val prevSummary = uiState.previousPeriodSummary

    var pendingShareFileAndMime by remember { mutableStateOf<Pair<java.io.File, String>?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Date Range & Unlimited Custom Filter Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Formatters.tr(lang, "Unlimited Historical Date Filter & Comparison", "কাস্টম তারিখ ও তুলনামূলক রিপোর্ট"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = uiState.periodPreset == preset,
                                onClick = { viewModel.setPeriodPreset(preset) },
                                label = { Text(if (lang == "BN") preset.labelBn else preset.labelEn) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Formatters.formatEpochDay(uiState.customStartEpochDay, dateFmt, lang)} → ${Formatters.formatEpochDay(uiState.customEndEpochDay, dateFmt, lang)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setPeriodPreset(
                                        PeriodPreset.CUSTOM,
                                        customStart = uiState.customStartEpochDay - 30L,
                                        customEnd = uiState.customEndEpochDay
                                    )
                                }
                            ) {
                                Text("-30d Start")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.setPeriodPreset(
                                        PeriodPreset.CUSTOM,
                                        customStart = uiState.customStartEpochDay,
                                        customEnd = Formatters.currentEpochDay()
                                    )
                                }
                            ) {
                                Text("To Today")
                            }
                        }
                    }
                }
            }
        }

        // 2. Cash-Basis P&L & Period Comparison Card
        if (summary != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Formatters.tr(lang, "Cash-Basis Profit & Loss Summary", "নগদ লাভ-ক্ষতি সারসংক্ষেপ (Cash P&L)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(Formatters.tr(lang, "Cash Income", "নগদ আয়"), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.formatBdt(summary.cashIncomePaisa, lang, numFmt),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = CashInGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(Formatters.tr(lang, "Cash Expense", "নগদ ব্যয়"), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.formatBdt(summary.cashExpensePaisa, lang, numFmt),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = CashOutRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Formatters.tr(lang, "Cash Net Profit", "নিট নগদ লাভ"), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.formatBdt(summary.cashProfitPaisa, lang, numFmt),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = if (summary.cashProfitPaisa >= 0L) EquityBlue else CashOutRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (prevSummary != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            val incomeDelta = summary.cashIncomePaisa - prevSummary.cashIncomePaisa
                            val expenseDelta = summary.cashExpensePaisa - prevSummary.cashExpensePaisa
                            Text(
                                text = Formatters.tr(lang, "Comparison vs Previous Period: ", "পূর্ববর্তী সময়ের তুলনা: ") +
                                    "Income Δ ${if (incomeDelta >= 0) "+" else ""}${Formatters.formatBdt(incomeDelta, lang, numFmt)}  |  " +
                                    "Expense Δ ${if (expenseDelta >= 0) "+" else ""}${Formatters.formatBdt(expenseDelta, lang, numFmt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Category Breakdown Bars
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Formatters.tr(lang, "Category Breakdown (Cash Income & Expense)", "ক্যাটাগরি অনুযায়ী নগদ আয় ও ব্যয়"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (summary.incomeByCategory.isEmpty() && summary.expenseByCategory.isEmpty()) {
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Zero-state: No settled cash income or expense in this date range.",
                                    "এই তারিখের মধ্যে কোনো নগদ আয় বা ব্যয় নেই।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            summary.incomeByCategory.forEach { row ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "IN • ${if (lang == "BN") row.category.nameBn else row.category.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${Formatters.formatBdt(row.totalPaisa, lang, numFmt)} (${String.format("%.0f%%", row.percentage)})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (row.percentage / 100f).coerceIn(0.04f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = CashInGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            summary.expenseByCategory.forEach { row ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "OUT • ${if (lang == "BN") row.category.nameBn else row.category.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${Formatters.formatBdt(row.totalPaisa, lang, numFmt)} (${String.format("%.0f%%", row.percentage)})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (row.percentage / 100f).coerceIn(0.04f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = CashOutRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. PDF & CSV Export Suite (Preview + Share)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Formatters.tr(lang, "Generate, Preview & Export Reports (PDF / CSV)", "রিপোর্ট প্রিভিউ এবং পিডিএফ / সিএসভি এক্সপোর্ট"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val reportCatalog = listOf(
                        Triple("CASH_PL", "Cash-Basis P&L Statement", "নগদ লাভ-ক্ষতি বিবরণী (Cash P&L)"),
                        Triple("CASHFLOW", "Cashflow & Account Balances Report", "নগদ প্রবাহ ও অ্যাকাউন্ট ব্যালেন্স"),
                        Triple("RECEIVABLES_PAYABLES", "Customer Receivables & Supplier Payables", "কাস্টমার বাকি ও মহাজন দেনা রিপোর্ট"),
                        Triple("ACCOUNT_STATEMENT", "Detailed Account Ledger Statement", "অ্যাকাউন্ট লেজার স্টেটমেন্ট"),
                        Triple("TRANSACTION_REGISTER", "Complete Audit Transaction Register", "সম্পূর্ণ লেনদেন রেজিস্টার")
                    )

                    reportCatalog.forEach { (code, titleEn, titleBn) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.tr(lang, titleEn, titleBn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.prepareReportPreviewAndExport(code) },
                                    modifier = Modifier.testTag("preview_report_$code")
                                ) {
                                    Text(Formatters.tr(lang, "Preview & Export", "প্রিভিউ ও এক্সপোর্ট"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // In-App Report Preview & Export Confirmation Modal (Feature 10 & Section 12 explicit export confirmation)
    val preview = uiState.exportPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeExportPreview() },
            title = {
                Column {
                    Text(preview.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(preview.periodLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            preview.summaryPairs.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(k, style = MaterialTheme.typography.bodySmall)
                                    Text(v, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Previewing ${preview.rows.size} rows • Saved locally to files/exports/",
                        style = MaterialTheme.typography.labelSmall
                    )
                    HorizontalDivider()
                    preview.rows.take(15).forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${r.col1} • ${r.col2}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text(r.col3, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(r.col4, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (preview.generatedCsvFile != null) {
                        OutlinedButton(
                            onClick = {
                                pendingShareFileAndMime = preview.generatedCsvFile to "text/csv"
                            }
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share CSV")
                        }
                    }
                    if (preview.generatedPdfFile != null) {
                        Button(
                            onClick = {
                                pendingShareFileAndMime = preview.generatedPdfFile to "application/pdf"
                            }
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeExportPreview() }) {
                    Text(Formatters.tr(lang, "Close", "বন্ধ করুন"))
                }
            }
        )
    }

    // Explicit Privacy Export/Share Confirmation Dialog (Feature 12: Explicit export/share confirmation)
    if (pendingShareFileAndMime != null) {
        val (file, mime) = pendingShareFileAndMime!!
        AlertDialog(
            onDismissRequest = { pendingShareFileAndMime = null },
            title = { Text(Formatters.tr(lang, "Confirm External File Share", "ফাইল শেয়ার নিশ্চিতকরণ")) },
            text = {
                Text(
                    Formatters.tr(
                        lang,
                        "This report (${file.name}) contains private business financial records. Are you sure you want to share it outside HishabKhata?",
                        "এই ফাইলে (${file.name}) আপনার ব্যবসার ব্যক্তিগত আর্থিক তথ্য রয়েছে। আপনি কি এটি বাইরে শেয়ার করতে নিশ্চিত?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingShareFileAndMime = null
                        ExportAndBackupManager.shareFile(
                            context = context,
                            file = file,
                            mimeType = mime,
                            chooserTitle = "Export Financial Report"
                        )
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Formatters.tr(lang, "Confirm & Share", "নিশ্চিত করুন"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingShareFileAndMime = null }) {
                    Text(Formatters.tr(lang, "Cancel", "বাতিল"))
                }
            }
        )
    }
}
