package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TransactionEntity
import com.example.ui.LedgerUiState
import com.example.ui.LedgerViewModel
import com.example.ui.MainTab
import com.example.ui.screens.AccountsAndTransfersScreen
import com.example.ui.screens.AutomationAndRemindersScreen
import com.example.ui.screens.ContactsAndSubLedgersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.ReportsAndExportsScreen
import com.example.ui.screens.SettingsAndBackupScreen
import com.example.ui.screens.SmartEntryBottomSheet
import com.example.ui.screens.TransactionAuditInspectorDialog
import com.example.ui.screens.TransactionsAndAuditScreen
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.HishabKhataTheme
import com.example.util.Formatters

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ledgerViewModel: LedgerViewModel = viewModel()
            val uiState by ledgerViewModel.uiState.collectAsStateWithLifecycle()

            HishabKhataTheme(themeMode = uiState.config.themeMode) {
                HishabKhataAppRoot(
                    uiState = uiState,
                    viewModel = ledgerViewModel
                )
            }
        }
    }
}

@Composable
fun HishabKhataAppRoot(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    if (!uiState.isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading HishabKhata Offline Ledger...", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    if (!uiState.config.isOnboardingCompleted) {
        OnboardingScreen(
            viewModel = viewModel,
            seededCategories = uiState.categories
        )
        return
    }

    if (uiState.isAppLocked) {
        PinLockScreen(
            viewModel = viewModel,
            businessName = uiState.config.businessName,
            ownerName = uiState.config.ownerName,
            pinHint = uiState.config.pinHint,
            biometricEnabled = uiState.config.biometricEnabled,
            lang = uiState.config.language
        )
        return
    }

    var inspectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // BackHandler returns to Dashboard when on any secondary tab
    BackHandler(enabled = uiState.activeTab != MainTab.DASHBOARD) {
        viewModel.selectTab(MainTab.DASHBOARD)
    }

    val lang = uiState.config.language

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Business Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.config.businessName.ifBlank { "HishabKhata" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.config.ownerName} • ${Formatters.tr(lang, "Offline SQLite", "অফলাইন লেজার")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Language Quick Pill
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .testTag("top_lang_toggle")
                                    .clickable { viewModel.toggleLanguage() }
                            ) {
                                Text(
                                    text = if (lang == "EN") "বাংলা" else "EN",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.selectTab(MainTab.AUTOMATION) },
                                modifier = Modifier.testTag("top_automation_btn")
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Recurring & Reminders",
                                    tint = if (uiState.activeTab == MainTab.AUTOMATION)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                                modifier = Modifier.testTag("top_settings_btn")
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings & Backup",
                                    tint = if (uiState.activeTab == MainTab.SETTINGS)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.config.pinHash.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.lockAppNow() },
                                    modifier = Modifier.testTag("top_lock_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Lock App",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Toast/Status Banner
                    AnimatedVisibility(visible = uiState.bannerMessage != null) {
                        uiState.bannerMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.clearBanner() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "OK",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.activeTab in setOf(MainTab.DASHBOARD, MainTab.TRANSACTIONS)) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openSmartEntry("CASH_INCOME") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(Formatters.tr(lang, "Smart Entry", "নতুন হিসাব")) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_smart_entry")
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.activeTab == MainTab.DASHBOARD,
                    onClick = { viewModel.selectTab(MainTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(Formatters.tr(lang, "Home", "হোম")) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = uiState.activeTab == MainTab.TRANSACTIONS,
                    onClick = { viewModel.selectTab(MainTab.TRANSACTIONS) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
                    label = { Text(Formatters.tr(lang, "Ledger", "খাতা")) },
                    modifier = Modifier.testTag("nav_transactions")
                )
                NavigationBarItem(
                    selected = uiState.activeTab == MainTab.CONTACTS,
                    onClick = { viewModel.selectTab(MainTab.CONTACTS) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                    label = { Text(Formatters.tr(lang, "Contacts", "বাকি/মহাজন")) },
                    modifier = Modifier.testTag("nav_contacts")
                )
                NavigationBarItem(
                    selected = uiState.activeTab == MainTab.ACCOUNTS,
                    onClick = { viewModel.selectTab(MainTab.ACCOUNTS) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                    label = { Text(Formatters.tr(lang, "Accounts", "অ্যাকাউন্ট")) },
                    modifier = Modifier.testTag("nav_accounts")
                )
                NavigationBarItem(
                    selected = uiState.activeTab == MainTab.REPORTS,
                    onClick = { viewModel.selectTab(MainTab.REPORTS) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text(Formatters.tr(lang, "Reports", "রিপোর্ট")) },
                    modifier = Modifier.testTag("nav_reports")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                MainTab.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onInspectTransaction = { inspectedTransaction = it }
                )
                MainTab.TRANSACTIONS -> TransactionsAndAuditScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onInspectTransaction = { inspectedTransaction = it }
                )
                MainTab.CONTACTS -> ContactsAndSubLedgersScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                MainTab.ACCOUNTS -> AccountsAndTransfersScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                MainTab.REPORTS -> ReportsAndExportsScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                MainTab.AUTOMATION -> AutomationAndRemindersScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                MainTab.SETTINGS -> SettingsAndBackupScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    if (uiState.showSmartEntrySheet) {
        SmartEntryBottomSheet(
            uiState = uiState,
            viewModel = viewModel
        )
    }

    if (inspectedTransaction != null) {
        val freshTx = uiState.transactions.find { it.id == inspectedTransaction!!.id } ?: inspectedTransaction!!
        TransactionAuditInspectorDialog(
            tx = freshTx,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { inspectedTransaction = null }
        )
    }
}
