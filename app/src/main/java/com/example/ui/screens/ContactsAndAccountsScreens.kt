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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AccountEntity
import com.example.data.ContactEntity
import com.example.domain.ContactLedgerSummary
import com.example.ui.LedgerUiState
import com.example.ui.LedgerViewModel
import com.example.ui.theme.BKashPink
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.PayablePurple
import com.example.ui.theme.ReceivableAmber
import com.example.ui.theme.RocketPurple
import com.example.util.Formatters

@Composable
fun ContactsAndSubLedgersScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat

    var roleFilter by remember { mutableStateOf("ALL") } // "ALL", "CUSTOMER", "SUPPLIER"
    var searchQuery by remember { mutableStateOf("") }
    var editingContact by remember { mutableStateOf<ContactEntity?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }
    var statementContact by remember { mutableStateOf<ContactLedgerSummary?>(null) }

    val filteredLedgers = uiState.contactLedgers.filter { cl ->
        val matchesRole = roleFilter == "ALL" || cl.contact.contactType == roleFilter
        val matchesQuery = searchQuery.isBlank() ||
            cl.contact.name.contains(searchQuery, ignoreCase = true) ||
            cl.contact.phone.contains(searchQuery, ignoreCase = true)
        matchesRole && matchesQuery
    }

    val totalCustomerReceivable = uiState.contactLedgers.sumOf { it.receivablePaisa }
    val totalSupplierPayable = uiState.contactLedgers.sumOf { it.payablePaisa }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contacts_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Summary Banner
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
                        Column {
                            Text(
                                text = Formatters.tr(lang, "Contacts & Sub-Ledgers", "কাস্টমার ও মহাজন সাব-লেজার"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Receivables, Payables, Partial Settlements & Statements",
                                    "বাকি হিসাব, আংশিক পরিশোধ ও স্টেটমেন্ট"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                editingContact = null
                                showContactDialog = true
                            },
                            modifier = Modifier.testTag("add_contact_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Add", "যোগ করুন"))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ReceivableAmber.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Formatters.tr(lang, "Total Receivable (Customers)", "মোট পাবো (কাস্টমার)"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = Formatters.formatBdt(totalCustomerReceivable, lang, numFmt),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = ReceivableAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PayablePurple.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Formatters.tr(lang, "Total Payable (Suppliers)", "মোট দেবো (মহাজন)"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = Formatters.formatBdt(totalSupplierPayable, lang, numFmt),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = PayablePurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = roleFilter == "ALL",
                        onClick = { roleFilter = "ALL" },
                        label = { Text(Formatters.tr(lang, "All (${uiState.contactLedgers.size})", "সবাই")) }
                    )
                    FilterChip(
                        selected = roleFilter == "CUSTOMER",
                        onClick = { roleFilter = "CUSTOMER" },
                        label = { Text(Formatters.tr(lang, "Customers", "কাস্টমার")) }
                    )
                    FilterChip(
                        selected = roleFilter == "SUPPLIER",
                        onClick = { roleFilter = "SUPPLIER" },
                        label = { Text(Formatters.tr(lang, "Suppliers", "মহাজন / সাপ্লায়ার")) }
                    )
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(Formatters.tr(lang, "Search Contact Name or Phone", "নাম বা মোবাইল নম্বর খুঁজুন")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredLedgers.isEmpty()) {
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
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(Formatters.tr(lang, "No contacts match your filter.", "কোনো কাস্টমার বা মহাজন পাওয়া যায়নি।"))
                    }
                }
            }
        } else {
            items(filteredLedgers, key = { it.contact.id }) { cl ->
                val isCust = cl.contact.contactType == "CUSTOMER"
                val openBalance = if (isCust) cl.receivablePaisa else cl.payablePaisa
                val accent = if (isCust) ReceivableAmber else PayablePurple

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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cl.contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = cl.contact.contactType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${cl.contact.phone.ifBlank { "No phone" }} • ${cl.contact.address.ifBlank { "Local" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isCust) Formatters.tr(lang, "Receivable", "পাবো")
                                    else Formatters.tr(lang, "Payable", "দেবো"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatBdt(openBalance, lang, numFmt),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = if (openBalance > 0L) accent else CashInGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Formatters.tr(lang, "Total Billed: ", "মোট বাকি: ") +
                                    Formatters.formatBdt(cl.totalBilledPaisa, lang, numFmt),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = Formatters.tr(lang, "Total Settled: ", "মোট পরিশোধ: ") +
                                    Formatters.formatBdt(cl.totalSettledPaisa, lang, numFmt),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.openSmartEntry(
                                        txType = if (isCust) "RECEIVE_DUE" else "PAY_DUE",
                                        contactId = cl.contact.id
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (isCust) Icons.Default.CallReceived else Icons.Default.CallMade,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isCust) Formatters.tr(lang, "Receive Due", "বাকি আদায়") else Formatters.tr(lang, "Pay Due", "দেনা পরিশোধ"))
                            }
                            OutlinedButton(
                                onClick = { statementContact = cl },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Formatters.tr(lang, "Statement", "স্টেটমেন্ট"))
                            }
                            IconButton(
                                onClick = {
                                    editingContact = cl.contact
                                    showContactDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Contact")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContactDialog) {
        var name by remember { mutableStateOf(editingContact?.name ?: "") }
        var phone by remember { mutableStateOf(editingContact?.phone ?: "") }
        var address by remember { mutableStateOf(editingContact?.address ?: "") }
        var notes by remember { mutableStateOf(editingContact?.notes ?: "") }
        var type by remember { mutableStateOf(editingContact?.contactType ?: "CUSTOMER") }

        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = {
                Text(
                    if (editingContact == null) Formatters.tr(lang, "New Customer / Supplier", "নতুন কাস্টমার / মহাজন")
                    else Formatters.tr(lang, "Edit Contact", "তথ্য সংশোধন")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "CUSTOMER",
                            onClick = { type = "CUSTOMER" },
                            label = { Text("Customer (Receivable)") }
                        )
                        FilterChip(
                            selected = type == "SUPPLIER",
                            onClick = { type = "SUPPLIER" },
                            label = { Text("Supplier (Payable)") }
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Shop") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Credit Terms") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val contact = (editingContact ?: ContactEntity(name = name, contactType = type)).copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                notes = notes.trim(),
                                contactType = type
                            )
                            viewModel.saveContact(contact)
                            showContactDialog = false
                        }
                    }
                ) {
                    Text(Formatters.tr(lang, "Save Contact", "সংরক্ষণ করুন"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text(Formatters.tr(lang, "Cancel", "বাতিল"))
                }
            }
        )
    }

    if (statementContact != null) {
        val sc = statementContact!!
        val contactTxs = uiState.transactions.filter { it.contactId == sc.contact.id }
        AlertDialog(
            onDismissRequest = { statementContact = null },
            title = { Text("${sc.contact.name} • Sub-Ledger Statement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Billed: ${Formatters.formatBdt(sc.totalBilledPaisa, lang, numFmt)} | Settled: ${Formatters.formatBdt(sc.totalSettledPaisa, lang, numFmt)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    if (contactTxs.isEmpty()) {
                        Text("No transactions recorded for this contact yet.")
                    } else {
                        contactTxs.take(10).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${Formatters.formatEpochDay(tx.dateEpochDay, dateFmt, lang)} • ${tx.txType}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = Formatters.formatBdt(tx.amountPaisa, lang, numFmt),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        statementContact = null
                        viewModel.prepareReportPreviewAndExport(
                            reportType = "RECEIVABLES_PAYABLES",
                            filterContactId = sc.contact.id
                        )
                    }
                ) {
                    Text(Formatters.tr(lang, "Export PDF / CSV", "পিডিএফ / সিএসভি এক্সপোর্ট"))
                }
            },
            dismissButton = {
                TextButton(onClick = { statementContact = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AccountsAndTransfersScreen(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat

    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedStatementAccountId by remember { mutableStateOf<String?>(null) }

    val recon = uiState.reconciliation

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("accounts_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Transfer / Add Account Actions
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
                                text = Formatters.tr(lang, "Accounts, Wallets & Reconciliation", "অ্যাকাউন্ট, ওয়ালেট ও সমন্বয়"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Cash, Bank, bKash, Nagad, Rocket & Double-Entry Ledger",
                                    "ক্যাশ, ব্যাংক, বিকাশ, নগদ, রকেট এবং ডাবল-এন্ট্রি লেজার"
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
                            onClick = {
                                editingAccount = null
                                showAccountDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_account_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Add Account", "নতুন অ্যাকাউন্ট"))
                        }
                        OutlinedButton(
                            onClick = { viewModel.openSmartEntry("TRANSFER") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("account_transfer_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Transfer", "ট্রান্সফার"))
                        }
                    }
                }
            }
        }

        // Double-Entry Reconciliation Card (Feature 6 & FR-06)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (recon.isHealthy) CashInGreen.copy(alpha = 0.12f) else CashOutRed.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (recon.isHealthy) CashInGreen else CashOutRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (recon.isHealthy)
                                    Formatters.tr(lang, "Double-Entry Reconciliation: Verified", "ডাবল-এন্ট্রি সমন্বয়: সম্পূর্ণ সঠিক")
                                else
                                    "Reconciliation Alert: Check Unbalanced Entries",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Entries: ${recon.totalEntries} | Total DR: ${Formatters.formatBdt(recon.totalDebitPaisa, lang, numFmt)} = CR: ${Formatters.formatBdt(recon.totalCreditPaisa, lang, numFmt)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Account Cards
        items(uiState.accountBalances, key = { it.account.id }) { ab ->
            val acc = ab.account
            val badgeColor = when (acc.accountType) {
                "BKASH" -> BKashPink
                "NAGAD" -> NagadOrange
                "ROCKET" -> RocketPurple
                "BANK" -> EquityBlue
                else -> CashInGreen
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (acc.isArchived)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = badgeColor)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (lang == "BN") acc.nameBn else acc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = if (acc.isArchived) "${acc.accountType} (ARCHIVED)" else acc.accountType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Opening: ${Formatters.formatBdt(acc.openingBalancePaisa, lang, numFmt)} (${Formatters.formatEpochDay(acc.openingDateEpochDay, dateFmt, lang)}) • ${ab.txCount} txs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatBdt(ab.currentBalancePaisa, lang, numFmt),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = if (ab.currentBalancePaisa >= 0L) MaterialTheme.colorScheme.onSurface else CashOutRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inflow (DR): +${Formatters.formatBdt(ab.totalInflowPaisa, lang, numFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CashInGreen
                        )
                        Text(
                            text = "Outflow (CR): -${Formatters.formatBdt(ab.totalOutflowPaisa, lang, numFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CashOutRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.prepareReportPreviewAndExport(
                                    reportType = "ACCOUNT_STATEMENT",
                                    filterAccountId = acc.id
                                )
                            }
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Statement PDF/CSV", "স্টেটমেন্ট"))
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    editingAccount = acc
                                    showAccountDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Account")
                            }
                            IconButton(
                                onClick = { viewModel.toggleArchiveAccount(acc) }
                            ) {
                                Icon(
                                    if (acc.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Archive Account"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccountDialog) {
        val isNew = editingAccount == null
        var name by remember { mutableStateOf(editingAccount?.name ?: "") }
        var nameBn by remember { mutableStateOf(editingAccount?.nameBn ?: "") }
        var accountType by remember { mutableStateOf(editingAccount?.accountType ?: "BKASH") }
        var accountNumber by remember { mutableStateOf(editingAccount?.accountNumber ?: "") }
        var openingInput by remember {
            mutableStateOf(
                if (editingAccount != null) Formatters.paisaToTakaInputString(editingAccount!!.openingBalancePaisa)
                else "0"
            )
        }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Text(
                    if (isNew) Formatters.tr(lang, "Add Financial Account", "নতুন আর্থিক অ্যাকাউন্ট")
                    else Formatters.tr(lang, "Edit Account", "অ্যাকাউন্ট সংশোধন")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "BANK", "BKASH", "NAGAD", "ROCKET", "WALLET").forEach { t ->
                            FilterChip(
                                selected = accountType == t,
                                onClick = { accountType = t },
                                label = { Text(t) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (English) *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text("Account Name (বাংলা)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account / Wallet Number (Optional)") },
                        singleLine = true
                    )
                    if (isNew) {
                        OutlinedTextField(
                            value = openingInput,
                            onValueChange = { openingInput = it },
                            label = { Text("Opening Balance in BDT (Posted to Opening Equity)") },
                            prefix = { Text("৳ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val openPaisa = Formatters.parseTakaToPaisa(openingInput) ?: 0L
                            val acc = (editingAccount ?: AccountEntity(
                                name = name.trim(),
                                nameBn = nameBn.trim().ifBlank { name.trim() },
                                accountType = accountType,
                                openingBalancePaisa = openPaisa,
                                openingDateEpochDay = Formatters.currentEpochDay()
                            )).copy(
                                name = name.trim(),
                                nameBn = nameBn.trim().ifBlank { name.trim() },
                                accountType = accountType,
                                accountNumber = accountNumber.trim()
                            )
                            viewModel.saveAccount(acc, isNew = isNew)
                            showAccountDialog = false
                        }
                    }
                ) {
                    Text(Formatters.tr(lang, "Save Account", "সংরক্ষণ করুন"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text(Formatters.tr(lang, "Cancel", "বাতিল"))
                }
            }
        )
    }
}
