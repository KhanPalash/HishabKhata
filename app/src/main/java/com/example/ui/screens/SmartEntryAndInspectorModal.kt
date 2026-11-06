package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AttachmentEntity
import com.example.data.ContactEntity
import com.example.data.TransactionEntity
import com.example.domain.AccountingEngine
import com.example.ui.LedgerUiState
import com.example.ui.LedgerViewModel
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.ReceivableAmber
import com.example.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartEntryBottomSheet(
    uiState: LedgerUiState,
    viewModel: LedgerViewModel
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat
    val editingTx = uiState.editingTransaction
    val isEdit = editingTx != null
    val scope = rememberCoroutineScope()

    val activeAccounts = remember(uiState.accounts) {
        uiState.accounts.filter { !it.isArchived }
    }
    val defaultAccountId = remember(activeAccounts, cfg.lastUsedAccountId) {
        editingTx?.accountId
            ?: activeAccounts.find { it.id == cfg.lastUsedAccountId }?.id
            ?: activeAccounts.firstOrNull()?.id
            ?: ""
    }

    var txType by remember {
        mutableStateOf(editingTx?.txType ?: uiState.preselectedSmartEntryType ?: "CASH_INCOME")
    }
    var amountInput by remember {
        mutableStateOf(
            if (editingTx != null) Formatters.paisaToTakaInputString(editingTx.amountPaisa) else ""
        )
    }
    var partialSettledInput by remember {
        mutableStateOf(
            if (editingTx != null && editingTx.txType in setOf("CREDIT_INCOME", "CREDIT_EXPENSE"))
                Formatters.paisaToTakaInputString(editingTx.settledAmountPaisa)
            else "0"
        )
    }
    var loanInterestInput by remember {
        mutableStateOf(
            if (editingTx != null && editingTx.loanInterestPaisa > 0L)
                Formatters.paisaToTakaInputString(editingTx.loanInterestPaisa)
            else "0"
        )
    }
    var titleInput by remember { mutableStateOf(editingTx?.title ?: "") }
    var selectedAccountId by remember { mutableStateOf(defaultAccountId) }
    var selectedToAccountId by remember {
        mutableStateOf(
            editingTx?.toAccountId ?: activeAccounts.firstOrNull { it.id != defaultAccountId }?.id ?: ""
        )
    }

    val appropriateCategories = remember(txType, uiState.categories) {
        val wantExpense = txType in setOf("CASH_EXPENSE", "CREDIT_EXPENSE", "PAY_DUE", "LOAN_REPAID", "REFUND_EXPENSE")
        uiState.categories.filter {
            !it.isArchived && it.categoryType == (if (wantExpense) "EXPENSE" else "INCOME")
        }
    }
    var selectedCategoryId by remember(txType) {
        mutableStateOf(editingTx?.categoryId ?: appropriateCategories.firstOrNull()?.id)
    }

    val appropriateContacts = remember(txType, uiState.contacts) {
        when (txType) {
            "CREDIT_INCOME", "RECEIVE_DUE", "CASH_INCOME", "REFUND_INCOME" ->
                uiState.contacts.filter { !it.isArchived && it.contactType == "CUSTOMER" }
            "CREDIT_EXPENSE", "PAY_DUE", "CASH_EXPENSE", "REFUND_EXPENSE", "LOAN_TAKEN", "LOAN_REPAID" ->
                uiState.contacts.filter { !it.isArchived && it.contactType == "SUPPLIER" }
            else -> uiState.contacts.filter { !it.isArchived }
        }
    }
    var selectedContactId by remember(txType) {
        mutableStateOf(
            editingTx?.contactId ?: uiState.preselectedContactId ?: appropriateContacts.firstOrNull()?.id
        )
    }

    var dateEpochDay by remember {
        mutableLongStateOf(editingTx?.dateEpochDay ?: Formatters.currentEpochDay())
    }
    var dueDaysOffset by remember { mutableLongStateOf(7L) }
    var referenceInput by remember { mutableStateOf(editingTx?.referenceCode ?: "") }
    var paymentDetailsInput by remember { mutableStateOf(editingTx?.paymentDetails ?: "") }
    var internalNoteInput by remember { mutableStateOf(editingTx?.internalNote ?: "") }
    var editReasonInput by remember { mutableStateOf("") }
    var allowOverpayment by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(isEdit) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showUnsavedConfirm by remember { mutableStateOf(false) }
    var duplicateConfirmed by remember { mutableStateOf(false) }

    val existingAttachments = remember(editingTx, uiState.attachments) {
        if (editingTx != null) uiState.attachments.filter { it.transactionId == editingTx.id } else emptyList()
    }
    val stagedAttachments = remember { mutableStateListOf<AttachmentEntity>() }

    // Zero-permission Photo Picker for receipt images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val att = viewModel.repository.createLocalAttachmentFromUri(
                    uri = uri,
                    customTitle = titleInput.ifBlank { txType },
                    mimeType = "IMAGE"
                )
                stagedAttachments.add(att)
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val parsedAmountPaisa = Formatters.parseTakaToPaisa(amountInput) ?: 0L
    val parsedSettledPaisa = when (txType) {
        "CREDIT_INCOME", "CREDIT_EXPENSE" -> (Formatters.parseTakaToPaisa(partialSettledInput) ?: 0L).coerceIn(0L, parsedAmountPaisa)
        else -> parsedAmountPaisa
    }
    val computedDuePaisa = (parsedAmountPaisa - parsedSettledPaisa).coerceAtLeast(0L)
    val parsedInterestPaisa = (Formatters.parseTakaToPaisa(loanInterestInput) ?: 0L).coerceIn(0L, parsedAmountPaisa)

    // Selected contact open balance for Receive Due / Pay Due validation
    val selectedContactLedger = uiState.contactLedgers.find { it.contact.id == selectedContactId }
    val openDueForContact = when (txType) {
        "RECEIVE_DUE" -> selectedContactLedger?.receivablePaisa ?: 0L
        "PAY_DUE" -> selectedContactLedger?.payablePaisa ?: 0L
        else -> 0L
    }

    // Live Double-Entry Journal Preview
    val previewTx = TransactionEntity(
        id = editingTx?.id ?: "PREVIEW",
        txType = txType,
        amountPaisa = parsedAmountPaisa.coerceAtLeast(0L),
        settledAmountPaisa = parsedSettledPaisa,
        dueAmountPaisa = computedDuePaisa,
        loanInterestPaisa = parsedInterestPaisa,
        accountId = selectedAccountId.ifBlank { null },
        toAccountId = if (txType == "TRANSFER") selectedToAccountId.ifBlank { null } else null,
        categoryId = selectedCategoryId,
        contactId = selectedContactId,
        dateEpochDay = dateEpochDay,
        dueDateEpochDay = if (computedDuePaisa > 0L) dateEpochDay + dueDaysOffset else null,
        title = titleInput.ifBlank { txType.replace("_", " ") },
        referenceCode = referenceInput.trim(),
        paymentDetails = paymentDetailsInput.trim(),
        internalNote = internalNoteInput.trim()
    )
    val previewJournal = remember(previewTx) {
        AccountingEngine.createJournalForTransaction(previewTx)
    }

    // Duplicate check
    val potentialDuplicate = remember(parsedAmountPaisa, txType, dateEpochDay, uiState.transactions) {
        if (!cfg.duplicateWarningEnabled || isEdit || parsedAmountPaisa <= 0L) null
        else uiState.transactions.firstOrNull {
            it.status == "POSTED" &&
                it.txType == txType &&
                it.amountPaisa == parsedAmountPaisa &&
                it.dateEpochDay == dateEpochDay
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (amountInput.isNotBlank() && !isEdit) {
                showUnsavedConfirm = true
            } else {
                viewModel.closeSmartEntry()
            }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEdit) {
                        Formatters.tr(lang, "Edit Transaction (Audit Reversal)", "লেনদেন সংশোধন (অডিট রিভার্সাল)")
                    } else {
                        Formatters.tr(lang, "Smart Transaction Entry", "স্মার্ট লেনদেন এন্ট্রি")
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    if (amountInput.isNotBlank() && !isEdit) showUnsavedConfirm = true
                    else viewModel.closeSmartEntry()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Dynamic Transaction Type Selector
            val txTypes = listOf(
                "CASH_INCOME" to Formatters.tr(lang, "Cash Income", "নগদ আয়"),
                "CREDIT_INCOME" to Formatters.tr(lang, "Credit Sale (Due)", "বাকি বিক্রয়"),
                "CASH_EXPENSE" to Formatters.tr(lang, "Cash Expense", "নগদ ব্যয়"),
                "CREDIT_EXPENSE" to Formatters.tr(lang, "Credit Purchase", "বাকি ক্রয়"),
                "RECEIVE_DUE" to Formatters.tr(lang, "Receive Due", "বাকি আদায়"),
                "PAY_DUE" to Formatters.tr(lang, "Pay Due", "দেনা পরিশোধ"),
                "TRANSFER" to Formatters.tr(lang, "Transfer", "ট্রান্সফার"),
                "OWNER_INVESTMENT" to Formatters.tr(lang, "Owner Capital", "মালিকের মূলধন জমা"),
                "OWNER_WITHDRAWAL" to Formatters.tr(lang, "Owner Drawing", "মালিকের উত্তোলন"),
                "LOAN_TAKEN" to Formatters.tr(lang, "Loan Taken", "ঋণ গ্রহণ"),
                "LOAN_REPAID" to Formatters.tr(lang, "Loan Repaid", "ঋণ পরিশোধ"),
                "REFUND_INCOME" to Formatters.tr(lang, "Sales Refund", "বিক্রয় ফেরত"),
                "REFUND_EXPENSE" to Formatters.tr(lang, "Expense Refund", "খরচ ফেরত")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                txTypes.forEach { (code, label) ->
                    FilterChip(
                        selected = txType == code,
                        onClick = {
                            txType = code
                            validationError = null
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("smart_type_$code")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Amount Input (Auto-Focused) & Title
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        validationError = null
                    },
                    label = { Text(Formatters.tr(lang, "Amount (BDT) *", "টাকার পরিমাণ *")) },
                    prefix = { Text("৳ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("smart_amount_input")
                )
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(Formatters.tr(lang, "Short Title / Memo", "বিবরণ / মেমো")) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("smart_title_input")
                )
            }

            // Partial payment for Credit Sale / Credit Purchase
            if (txType in setOf("CREDIT_INCOME", "CREDIT_EXPENSE")) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = partialSettledInput,
                    onValueChange = { partialSettledInput = it },
                    label = {
                        Text(
                            Formatters.tr(
                                lang,
                                "Immediate Cash Paid/Received Now (0 = Full Credit)",
                                "নগদ জমা/পরিশোধ (০ দিলে সম্পূর্ণ বাকি)"
                            )
                        )
                    },
                    prefix = { Text("৳ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = Formatters.tr(lang, "Recognized in Cash P&L now: ", "নগদ হিসাবে যুক্ত হবে: ") +
                        Formatters.formatBdt(parsedSettledPaisa, lang, numFmt) + " • " +
                        Formatters.tr(lang, "Open Due: ", "বাকি থাকবে: ") +
                        Formatters.formatBdt(computedDuePaisa, lang, numFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = ReceivableAmber,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Principal vs Interest split for Loan Repayment (Rule 8)
            if (txType == "LOAN_REPAID") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = loanInterestInput,
                    onValueChange = { loanInterestInput = it },
                    label = {
                        Text(
                            Formatters.tr(
                                lang,
                                "Interest Portion (Expense) • Rest reduces Loan Principal",
                                "সুদের অংশ (ব্যয়) • বাকি অংশ আসল ঋণ কমাবে"
                            )
                        )
                    },
                    prefix = { Text("৳ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Account Selector (Remembered Last-Used Active Account)
            val needsAccount = !(txType in setOf("CREDIT_INCOME", "CREDIT_EXPENSE") && parsedSettledPaisa == 0L)
            if (needsAccount) {
                Text(
                    text = if (txType == "TRANSFER")
                        Formatters.tr(lang, "From Source Account *", "উৎস অ্যাকাউন্ট (যেখান থেকে যাবে) *")
                    else
                        Formatters.tr(lang, "Financial Account (Last-Used Remembered) *", "অ্যাকাউন্ট নির্বাচন *"),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeAccounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${if (lang == "BN") acc.nameBn else acc.name} (${acc.accountType})") }
                        )
                    }
                }
            }

            if (txType == "TRANSFER") {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Formatters.tr(lang, "To Destination Account *", "গন্তব্য অ্যাকাউন্ট (যেখানে জমা হবে) *"),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeAccounts.forEach { acc ->
                        FilterChip(
                            selected = selectedToAccountId == acc.id,
                            onClick = { selectedToAccountId = acc.id },
                            label = { Text("${if (lang == "BN") acc.nameBn else acc.name} (${acc.accountType})") }
                        )
                    }
                }
            }

            // 4. Contact Selector (Mandatory for Credit Income/Expense & Receive/Pay Due)
            val requiresContact = txType in setOf("CREDIT_INCOME", "CREDIT_EXPENSE", "RECEIVE_DUE", "PAY_DUE")
            if (requiresContact || showAdvanced) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Formatters.tr(
                        lang,
                        if (requiresContact) "Customer / Supplier Counterparty *" else "Customer / Supplier (Optional)",
                        if (requiresContact) "কাস্টমার / মহাজন নির্বাচন *" else "কাস্টমার / মহাজন (ঐচ্ছিক)"
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appropriateContacts.forEach { c ->
                        FilterChip(
                            selected = selectedContactId == c.id,
                            onClick = { selectedContactId = c.id },
                            label = { Text(c.name) }
                        )
                    }
                }
                if (txType in setOf("RECEIVE_DUE", "PAY_DUE") && selectedContactLedger != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = Formatters.tr(lang, "Open Balance: ", "বর্তমান বাকি: ") +
                                Formatters.formatBdt(openDueForContact, lang, numFmt),
                            style = MaterialTheme.typography.labelMedium,
                            color = ReceivableAmber
                        )
                        if (parsedAmountPaisa > openDueForContact) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allowOverpayment,
                                    onCheckedChange = { allowOverpayment = it }
                                )
                                Text(
                                    text = Formatters.tr(lang, "Allow Overpayment", "অতিরিক্ত জমা অনুমোদন"),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // 5. Suggested Category Selector
            val showCategories = txType in setOf(
                "CASH_INCOME", "CREDIT_INCOME", "CASH_EXPENSE", "CREDIT_EXPENSE",
                "RECEIVE_DUE", "PAY_DUE", "REFUND_INCOME", "REFUND_EXPENSE"
            )
            if (showCategories && appropriateCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Formatters.tr(lang, "Suggested Categories (By Usage)", "পরামর্শকৃত ক্যাটাগরি"),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    appropriateCategories.take(8).forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(if (lang == "BN") cat.nameBn else cat.name) }
                        )
                    }
                }
            }

            // 6. Expandable Advanced Fields & Attachments
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.testTag("toggle_advanced_fields_btn")
            ) {
                Icon(
                    if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    Formatters.tr(
                        lang,
                        if (showAdvanced) "Hide Advanced Fields & Attachments" else "Show Date, Ref, Notes & Receipt Attachment",
                        if (showAdvanced) "অ্যাডভান্সড তথ্য লুকান" else "তারিখ, রেফারেন্স, নোট ও রশিদ যুক্ত করুন"
                    )
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Formatters.tr(lang, "Transaction Date: ", "লেনদেনের তারিখ: ") +
                                Formatters.formatEpochDay(dateEpochDay, dateFmt, lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { dateEpochDay -= 1L }) { Text("-1d") }
                            OutlinedButton(onClick = { dateEpochDay = Formatters.currentEpochDay() }) { Text("Today") }
                            OutlinedButton(onClick = { dateEpochDay += 1L }) { Text("+1d") }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = referenceInput,
                            onValueChange = { referenceInput = it },
                            label = { Text(Formatters.tr(lang, "Invoice / TrxID / Check #", "ভাউচার / TrxID")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paymentDetailsInput,
                            onValueChange = { paymentDetailsInput = it },
                            label = { Text(Formatters.tr(lang, "Payment Channel Details", "পেমেন্টের বিবরণ")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = internalNoteInput,
                        onValueChange = { internalNoteInput = it },
                        label = { Text(Formatters.tr(lang, "Private Internal Note", "ব্যক্তিগত নোট")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Attachments Section (Feature 4)
                    Text(
                        text = Formatters.tr(lang, "Attachments (Receipt, Photo, Payment Proof)", "সংযুক্তি (রশিদ বা পেমেন্ট প্রমাণ)"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Pick Photo", "ছবি যুক্ত করুন"))
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val att = viewModel.repository.createLocalAttachmentFromUri(
                                        uri = null,
                                        customTitle = titleInput.ifBlank { "Voucher_${txType}" },
                                        mimeType = "RECEIPT",
                                        noteContentIfGenerated = "Amount: $amountInput BDT | Ref: $referenceInput"
                                    )
                                    stagedAttachments.add(att)
                                }
                            }
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "+ Receipt Voucher", "+ রশিদ ভাউচার"))
                        }
                    }

                    (existingAttachments + stagedAttachments).forEach { att ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${att.fileName} (${att.mimeType} • ${att.fileSizeBytes} B)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (stagedAttachments.contains(att)) {
                                            stagedAttachments.remove(att)
                                        } else {
                                            scope.launch { viewModel.repository.removeAttachment(att.id) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Attachment")
                                }
                            }
                        }
                    }
                }
            }

            if (isEdit) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = editReasonInput,
                    onValueChange = { editReasonInput = it },
                    label = { Text(Formatters.tr(lang, "Audit Reason for Edit (Optional)", "সংশোধনের কারণ (অডিট লগের জন্য)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 7. Live Double-Entry Journal Impact Preview Card
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.tr(lang, "Double-Entry Journal Preview", "ডাবল-এন্ট্রি জার্নাল প্রিভিউ"),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = if (previewJournal.entry.isBalanced) "DR = CR BALANCED" else "UNBALANCED",
                            style = MaterialTheme.typography.labelSmall,
                            color = CashInGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    previewJournal.lines.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (line.debitPaisa > 0) "DR" else "CR"} • ${line.ledgerName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = Formatters.formatBdt(
                                    maxOf(line.debitPaisa, line.creditPaisa),
                                    lang,
                                    numFmt
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Duplicate Warning Banner
            if (potentialDuplicate != null && !duplicateConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ReceivableAmber.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ReceivableAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formatters.tr(
                                    lang,
                                    "Duplicate Warning: A similar transaction (${potentialDuplicate.title}) of the same amount exists today.",
                                    "সতর্কতা: আজ একই টাকার সমপরিমাণ আরেকটি লেনদেন রয়েছে।"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { duplicateConfirmed = true }) {
                            Text(Formatters.tr(lang, "Proceed Anyway", "তবুও সেভ করুন"))
                        }
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (parsedAmountPaisa <= 0L) {
                        validationError = Formatters.tr(lang, "Amount must be greater than 0 BDT.", "টাকার পরিমাণ ০-এর বেশি হতে হবে।")
                        return@Button
                    }
                    if (needsAccount && selectedAccountId.isBlank()) {
                        validationError = Formatters.tr(lang, "Please select an active Financial Account.", "একটি সক্রিয় অ্যাকাউন্ট নির্বাচন করুন।")
                        return@Button
                    }
                    if (txType == "TRANSFER" && (selectedToAccountId.isBlank() || selectedToAccountId == selectedAccountId)) {
                        validationError = Formatters.tr(
                            lang,
                            "Select two different active accounts for Transfer.",
                            "ট্রান্সফারের জন্য দুটি ভিন্ন অ্যাকাউন্ট নির্বাচন করুন।"
                        )
                        return@Button
                    }
                    if (requiresContact && selectedContactId.isNullOrBlank()) {
                        validationError = Formatters.tr(
                            lang,
                            "Please select a Customer or Supplier for credit/settlement transactions.",
                            "বাকি বা আদায়/পরিশোধের জন্য কাস্টমার বা মহাজন নির্বাচন আবশ্যক।"
                        )
                        return@Button
                    }
                    if (txType in setOf("RECEIVE_DUE", "PAY_DUE") && parsedAmountPaisa > openDueForContact && !allowOverpayment) {
                        validationError = Formatters.tr(
                            lang,
                            "Settlement exceeds open due balance (${Formatters.formatBdt(openDueForContact, lang, numFmt)}). Check 'Allow Overpayment' or reduce amount.",
                            "পরিশোধের পরিমাণ বর্তমান বাকির চেয়ে বেশি। পরিমাণ কমান অথবা 'অতিরিক্ত জমা অনুমোদন' টিক দিন।"
                        )
                        return@Button
                    }
                    if (potentialDuplicate != null && !duplicateConfirmed) {
                        validationError = Formatters.tr(
                            lang,
                            "Please confirm 'Proceed Anyway' on the duplicate warning above.",
                            "উপরের ডুপ্লিকেট সতর্কতায় 'তবুও সেভ করুন' নিশ্চিত করুন।"
                        )
                        return@Button
                    }

                    val finalTx = previewTx.copy(
                        id = editingTx?.id ?: java.util.UUID.randomUUID().toString(),
                        title = titleInput.trim().ifBlank { txType.replace("_", " ") },
                        version = editingTx?.version ?: 1
                    )
                    viewModel.saveTransactionFromSmartForm(
                        tx = finalTx,
                        attachments = stagedAttachments.toList(),
                        isEdit = isEdit,
                        editReason = editReasonInput.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_save_tx_button")
            ) {
                Text(
                    if (isEdit) Formatters.tr(lang, "Post Reversal & Replacement Journal", "রিভার্সাল ও নতুন জার্নাল পোস্ট করুন")
                    else Formatters.tr(lang, "Confirm & Post Balanced Journal", "নিশ্চিত করুন ও জার্নাল সংরক্ষণ করুন")
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showUnsavedConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsavedConfirm = false },
            title = { Text(Formatters.tr(lang, "Discard Unsaved Entry?", "অসংরক্ষিত তথ্য বাতিল করবেন?")) },
            text = {
                Text(
                    Formatters.tr(
                        lang,
                        "You have entered an amount that has not been posted to the ledger yet.",
                        "আপনি যে তথ্য লিখেছেন তা এখনো লেজারে সংরক্ষিত হয়নি।"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedConfirm = false
                        viewModel.closeSmartEntry()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashOutRed)
                ) {
                    Text(Formatters.tr(lang, "Discard", "বাতিল করুন"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedConfirm = false }) {
                    Text(Formatters.tr(lang, "Keep Editing", "ফিরে যান"))
                }
            }
        )
    }
}

@Composable
fun TransactionAuditInspectorDialog(
    tx: TransactionEntity,
    uiState: LedgerUiState,
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit
) {
    val cfg = uiState.config
    val lang = cfg.language
    val numFmt = cfg.numberFormat
    val dateFmt = cfg.dateFormat

    val entriesForTx = uiState.journalEntries.filter { it.transactionId == tx.id }
    val linesForTx = uiState.journalLines.filter { it.transactionId == tx.id }
    val logsForTx = uiState.auditLogs.filter { it.transactionId == tx.id }
    val attachmentsForTx = uiState.attachments.filter { it.transactionId == tx.id }

    var deleteReason by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tx.txType} • Status: ${tx.status} • v${tx.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = Formatters.formatBdt(tx.amountPaisa, lang, numFmt),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Date: ${Formatters.formatEpochDay(tx.dateEpochDay, dateFmt, lang)}  |  Ref: ${tx.referenceCode.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (tx.internalNote.isNotBlank()) {
                            Text(
                                text = "Note: ${tx.internalNote}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Attachments
                if (attachmentsForTx.isNotEmpty()) {
                    Text(
                        text = Formatters.tr(lang, "Attachments (${attachmentsForTx.size})", "সংযুক্ত রশিদ (${attachmentsForTx.size})"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    attachmentsForTx.forEach { att ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${att.fileName} ${if (att.isMissingOrCorrupt) "[MISSING FILE]" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = {
                                    kotlinx.coroutines.MainScope().launch {
                                        viewModel.repository.removeAttachment(att.id)
                                    }
                                }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Double-Entry Journal Entries & Lines
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Formatters.tr(
                            lang,
                            "Immutable Double-Entry Journal (${entriesForTx.size} entries)",
                            "ডাবল-এন্ট্রি জার্নাল ইতিহাস (${entriesForTx.size}টি এন্ট্রি)"
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                entriesForTx.forEach { je ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${je.entryType} • ${je.memo}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val jeLines = linesForTx.filter { it.journalEntryId == je.id }
                            jeLines.forEach { ln ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${if (ln.debitPaisa > 0) "DR" else "CR"} ${ln.ledgerName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = Formatters.formatBdt(maxOf(ln.debitPaisa, ln.creditPaisa), lang, numFmt),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Audit Log History
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Formatters.tr(lang, "Audit Trail History (${logsForTx.size})", "অডিট লগ ইতিহাস"),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                logsForTx.forEach { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${log.action} • by ${log.actorName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reason: ${log.reason} • ${Formatters.formatTimestamp(log.timestamp, lang)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (showDeleteConfirm) {
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for Logical Reversal/Deletion") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (tx.status != "DELETED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!showDeleteConfirm) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CashOutRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Formatters.tr(lang, "Reverse/Delete", "রিভার্স/ডিলিট"))
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                viewModel.openSmartEntry(editTx = tx)
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Formatters.tr(lang, "Edit", "সংশোধন"))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.deleteTransactionLogically(tx.id, deleteReason)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CashOutRed)
                        ) {
                            Text(Formatters.tr(lang, "Confirm Logical Reversal", "নিশ্চিত রিভার্সাল"))
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Formatters.tr(lang, "Close", "বন্ধ করুন"))
            }
        }
    )
}
