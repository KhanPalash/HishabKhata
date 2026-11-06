package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TransactionEntity
import com.example.ui.LedgerUiState
import com.example.ui.LedgerViewModel
import com.example.ui.MainTab
import com.example.ui.PeriodPreset
import com.example.ui.theme.BKashPink
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.EmeraldPrimaryContainerDark
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.PayablePurple
import com.example.ui.theme.ReceivableAmber
import com.example.ui.theme.RocketPurple
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel,
    onInspectTransaction: (TransactionEntity) -> Unit
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat
    val summary = uiState.currentPeriodSummary
    val prevSummary = uiState.previousPeriodSummary

    val activeAccountBalances = uiState.accountBalances.filter { !it.account.isArchived }
    val pendingReminders = uiState.reminders.filter { it.status == "UPCOMING" || it.status == "OVERDUE" || it.status == "SNOOZED" }
    val recentTransactions = uiState.transactions.filter { it.status == "POSTED" }.take(8)
    val latestBackup = uiState.backupHistory.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Total Balance & Cash-Basis P&L Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(EmeraldPrimaryLight, EmeraldPrimaryContainerDark)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Total Active Cash, Bank & Wallet Balance",
                                    "সকল সক্রিয় ক্যাশ, ব্যাংক ও ওয়ালেট ব্যালেন্স"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatBdt(
                                    summary?.totalActiveCashBalancePaisa ?: 0L,
                                    lang,
                                    numFmt
                                ),
                                style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMono),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("hero_total_balance")
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Journal Status",
                                    tint = GoldSecondaryDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.reconciliation.isHealthy) {
                                        Formatters.tr(lang, "Ledger Balanced", "জার্নাল সুষম")
                                    } else {
                                        "Check Journal"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Accounts Horizontal Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeAccountBalances.forEach { ab ->
                            val badgeColor = when (ab.account.accountType) {
                                "BKASH" -> BKashPink
                                "NAGAD" -> NagadOrange
                                "ROCKET" -> RocketPurple
                                "BANK" -> EquityBlue
                                else -> CashInGreen
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.13f),
                                modifier = Modifier.clickable { viewModel.selectTab(MainTab.ACCOUNTS) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = if (lang == "BN") ab.account.nameBn else ab.account.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Text(
                                            text = Formatters.formatBdt(ab.currentBalancePaisa, lang, numFmt),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Period Cash Income, Cash Expense, Cash-Basis Profit
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Formatters.tr(lang, "Cash Income", "নগদ আয়"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = Formatters.formatBdt(summary?.cashIncomePaisa ?: 0L, lang, numFmt),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Formatters.tr(lang, "Cash Expense", "নগদ ব্যয়"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = Formatters.formatBdt(summary?.cashExpensePaisa ?: 0L, lang, numFmt),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.tr(lang, "Cash Profit", "নগদ মুনাফা"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                val profit = summary?.cashProfitPaisa ?: 0L
                                Text(
                                    text = Formatters.formatBdt(profit, lang, numFmt),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = if (profit >= 0L) GoldSecondaryDark else Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Period Filter Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodPreset.entries.filter { it != PeriodPreset.CUSTOM }.forEach { preset ->
                    FilterChip(
                        selected = uiState.periodPreset == preset,
                        onClick = { viewModel.setPeriodPreset(preset) },
                        label = { Text(if (lang == "BN") preset.labelBn else preset.labelEn) }
                    )
                }
                if (prevSummary != null && summary != null) {
                    val diff = summary.cashProfitPaisa - prevSummary.cashProfitPaisa
                    AssistChip(
                        onClick = { viewModel.selectTab(MainTab.REPORTS) },
                        label = {
                            Text(
                                text = Formatters.tr(lang, "vs Prev: ", "পূর্ববর্তী তুলনা: ") +
                                    (if (diff >= 0) "+" else "") +
                                    Formatters.formatBdt(diff, lang, numFmt)
                            )
                        }
                    )
                }
            }
        }

        // 3. Quick Actions 6-Button Grid (Feature 2 requirement)
        item {
            Column {
                Text(
                    text = Formatters.tr(lang, "Quick Operations", "দ্রুত কার্যক্রম"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_add_tx"),
                        icon = Icons.Default.AddCircle,
                        label = Formatters.tr(lang, "Add Entry", "নতুন লেনদেন"),
                        tint = CashInGreen,
                        onClick = { viewModel.openSmartEntry("CASH_INCOME") }
                    )
                    QuickActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_receive_due"),
                        icon = Icons.Default.CallReceived,
                        label = Formatters.tr(lang, "Receive Due", "বাকি আদায়"),
                        tint = ReceivableAmber,
                        onClick = { viewModel.openSmartEntry("RECEIVE_DUE") }
                    )
                    QuickActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_pay_due"),
                        icon = Icons.Default.CallMade,
                        label = Formatters.tr(lang, "Pay Due", "দেনা পরিশোধ"),
                        tint = PayablePurple,
                        onClick = { viewModel.openSmartEntry("PAY_DUE") }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_transfer"),
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        label = Formatters.tr(lang, "Transfer", "ব্যালেন্স ট্রান্সফার"),
                        tint = EquityBlue,
                        onClick = { viewModel.openSmartEntry("TRANSFER") }
                    )
                    QuickActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_add_contact"),
                        icon = Icons.Default.PersonAdd,
                        label = Formatters.tr(lang, "Contacts", "কাস্টমার/মহাজন"),
                        tint = EmeraldPrimaryLight,
                        onClick = { viewModel.selectTab(MainTab.CONTACTS) }
                    )
                    QuickActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_reports"),
                        icon = Icons.Default.Assessment,
                        label = Formatters.tr(lang, "Reports & PDF", "রিপোর্ট ও পিডিএফ"),
                        tint = GoldSecondaryDark,
                        onClick = { viewModel.selectTab(MainTab.REPORTS) }
                    )
                }
            }
        }

        // 4. Separate Receivable, Payable, Owner Capital, Owner Withdrawal & Loan Indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                "Receivables, Payables & Capital Position",
                                "বাকি, দেনা, মূলধন ও ঋণ সূচক (Non-P&L)"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Formatters.tr(lang, "Sub-Ledgers", "সাব-লেজার"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BalanceSheetMetricBox(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectTab(MainTab.CONTACTS) },
                            title = Formatters.tr(lang, "Customer Receivable", "পাবো (Receivable)"),
                            amount = Formatters.formatBdt(summary?.totalReceivablePaisa ?: 0L, lang, numFmt),
                            accentColor = ReceivableAmber
                        )
                        BalanceSheetMetricBox(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectTab(MainTab.CONTACTS) },
                            title = Formatters.tr(lang, "Supplier Payable", "দেবো (Payable)"),
                            amount = Formatters.formatBdt(summary?.totalPayablePaisa ?: 0L, lang, numFmt),
                            accentColor = PayablePurple
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val totalOwnerEquity = (summary?.openingEquityPaisa ?: 0L) + (summary?.ownerCapitalPaisa ?: 0L)
                        BalanceSheetMetricBox(
                            modifier = Modifier.weight(1f),
                            title = Formatters.tr(lang, "Owner Capital", "মালিকের মূলধন"),
                            amount = Formatters.formatBdt(totalOwnerEquity, lang, numFmt),
                            accentColor = EquityBlue
                        )
                        BalanceSheetMetricBox(
                            modifier = Modifier.weight(1f),
                            title = Formatters.tr(lang, "Owner Drawings", "মালিকের উত্তোলন"),
                            amount = Formatters.formatBdt(summary?.ownerWithdrawalPaisa ?: 0L, lang, numFmt),
                            accentColor = CashOutRed
                        )
                        BalanceSheetMetricBox(
                            modifier = Modifier.weight(1f),
                            title = Formatters.tr(lang, "Loan Balance", "ব্যবসায়িক ঋণ"),
                            amount = Formatters.formatBdt(summary?.loanLiabilityPaisa ?: 0L, lang, numFmt),
                            accentColor = NagadOrange
                        )
                    }
                }
            }
        }

        // 5. Overdue Items, Upcoming Reminders & Backup Status Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(MainTab.AUTOMATION) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = Formatters.tr(lang, "Due & Bill Reminders", "তাগাদা ও বিল রিমাইন্ডার"),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "${pendingReminders.size} active items",
                                    "${Formatters.toBanglaDigits(pendingReminders.size.toString())}টি সক্রিয় রিমাইন্ডার"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(MainTab.SETTINGS) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = Formatters.tr(lang, "Local Backup", "লোকাল ব্যাকআপ"),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = if (latestBackup != null) {
                                    Formatters.tr(lang, "${uiState.backupHistory.size} saved (SHA-256)", "${uiState.backupHistory.size}টি সংরক্ষিত")
                                } else {
                                    Formatters.tr(lang, "Tap to backup now", "এখনই ব্যাকআপ নিন")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.tr(lang, "Recent Ledger Transactions", "সাম্প্রতিক লেনদেনসমূহ"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.selectTab(MainTab.TRANSACTIONS) }) {
                    Text(Formatters.tr(lang, "View All (${uiState.transactions.size})", "সব দেখুন"))
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Formatters.tr(
                                lang,
                                "No transactions recorded yet. Tap 'Add Entry' to record your first sale or expense!",
                                "এখনো কোনো লেনদেন নেই। প্রথম আয় বা ব্যয় যোগ করতে 'নতুন লেনদেন' বাটনে চাপুন!"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    lang = lang,
                    numFmt = numFmt,
                    dateFmt = dateFmt,
                    onClick = { onInspectTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BalanceSheetMetricBox(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.09f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionEntity,
    lang: String,
    numFmt: String,
    dateFmt: String,
    onClick: () -> Unit
) {
    val isInflow = tx.txType in setOf(
        "CASH_INCOME", "RECEIVE_DUE", "OWNER_INVESTMENT", "LOAN_TAKEN", "REFUND_EXPENSE", "OPENING_BALANCE"
    )
    val isCreditUnsettled = tx.txType in setOf("CREDIT_INCOME", "CREDIT_EXPENSE") && tx.dueAmountPaisa > 0L
    val badgeColor = when {
        tx.status == "DELETED" -> Color.Gray
        tx.txType == "TRANSFER" -> EquityBlue
        isCreditUnsettled -> ReceivableAmber
        isInflow -> CashInGreen
        else -> CashOutRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_card_${tx.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (tx.status == "DELETED")
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            tx.txType == "TRANSFER" -> Icons.AutoMirrored.Filled.CompareArrows
                            isInflow -> Icons.AutoMirrored.Filled.TrendingUp
                            else -> Icons.AutoMirrored.Filled.TrendingDown
                        },
                        contentDescription = tx.txType,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tx.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tx.version > 1 || tx.status == "DELETED") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (tx.status == "DELETED") "REVERSED" else "v${tx.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${tx.txType.replace("_", " ")} • ${Formatters.formatEpochDay(tx.dateEpochDay, dateFmt, lang)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val sign = when {
                    tx.txType == "TRANSFER" -> "⇄ "
                    isInflow -> "+ "
                    else -> "- "
                }
                Text(
                    text = sign + Formatters.formatBdt(tx.amountPaisa, lang, numFmt),
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
                if (tx.dueAmountPaisa > 0L) {
                    Text(
                        text = Formatters.tr(lang, "Due: ", "বাকি: ") +
                            Formatters.formatBdt(tx.dueAmountPaisa, lang, numFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = ReceivableAmber
                    )
                }
            }
        }
    }
}
