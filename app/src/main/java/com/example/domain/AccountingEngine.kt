package com.example.domain

import com.example.data.AccountEntity
import com.example.data.CategoryEntity
import com.example.data.ContactEntity
import com.example.data.JournalEntryEntity
import com.example.data.JournalLineEntity
import com.example.data.TransactionEntity
import java.util.UUID

data class GeneratedJournalBundle(
    val entry: JournalEntryEntity,
    val lines: List<JournalLineEntity>
)

data class AccountBalanceSummary(
    val account: AccountEntity,
    val currentBalancePaisa: Long,
    val totalInflowPaisa: Long,
    val totalOutflowPaisa: Long,
    val txCount: Int
)

data class ContactLedgerSummary(
    val contact: ContactEntity,
    val receivablePaisa: Long, // Customer owes business
    val payablePaisa: Long,    // Business owes supplier
    val totalBilledPaisa: Long,
    val totalSettledPaisa: Long
)

data class CategoryReportRow(
    val category: CategoryEntity,
    val totalPaisa: Long,
    val percentage: Float
)

data class ReconciliationReport(
    val isHealthy: Boolean,
    val totalEntries: Int,
    val totalLines: Int,
    val totalDebitPaisa: Long,
    val totalCreditPaisa: Long,
    val unbalancedEntryIds: List<String>
)

data class PeriodFinancialSummary(
    val startEpochDay: Long,
    val endEpochDay: Long,
    val cashIncomePaisa: Long,
    val cashExpensePaisa: Long,
    val cashProfitPaisa: Long,
    val netCashflowPaisa: Long,
    val totalActiveCashBalancePaisa: Long,
    val totalReceivablePaisa: Long,
    val totalPayablePaisa: Long,
    val ownerCapitalPaisa: Long,
    val ownerWithdrawalPaisa: Long,
    val loanLiabilityPaisa: Long,
    val openingEquityPaisa: Long,
    val incomeByCategory: List<CategoryReportRow>,
    val expenseByCategory: List<CategoryReportRow>
)

object AccountingEngine {
    // Standard Chart of Accounts Ledger Codes
    const val LEDGER_ASSET_ACCOUNT = "ASSET_ACCOUNT"
    const val LEDGER_ASSET_RECEIVABLE = "ASSET_RECEIVABLE"
    const val LEDGER_LIABILITY_PAYABLE = "LIABILITY_PAYABLE"
    const val LEDGER_LIABILITY_LOAN = "LIABILITY_LOAN"
    const val LEDGER_EQUITY_OPENING = "EQUITY_OPENING"
    const val LEDGER_EQUITY_OWNER_CAPITAL = "EQUITY_OWNER_CAPITAL"
    const val LEDGER_EQUITY_OWNER_DRAWING = "EQUITY_OWNER_DRAWING"
    const val LEDGER_INCOME_CASH = "INCOME_CASH"
    const val LEDGER_EXPENSE_CASH = "EXPENSE_CASH"
    const val LEDGER_INCOME_UNREALIZED = "INCOME_UNREALIZED_CREDIT"
    const val LEDGER_EXPENSE_UNREALIZED = "EXPENSE_UNREALIZED_CREDIT"

    /**
     * Generates a strictly balanced double-entry JournalEntry + JournalLines for a TransactionEntity.
     * Enforces all 11 Accounting Rules from the PRD.
     */
    fun createJournalForTransaction(
        tx: TransactionEntity,
        entryType: String = "ORIGINAL",
        memoPrefix: String = ""
    ): GeneratedJournalBundle {
        val entryId = UUID.randomUUID().toString()
        val lines = mutableListOf<JournalLineEntity>()
        val date = tx.dateEpochDay

        fun addLine(
            code: String,
            name: String,
            debit: Long,
            credit: Long,
            accId: String? = null,
            conId: String? = null,
            catId: String? = null
        ) {
            if (debit == 0L && credit == 0L) return
            lines.add(
                JournalLineEntity(
                    journalEntryId = entryId,
                    transactionId = tx.id,
                    ledgerCode = code,
                    ledgerName = name,
                    accountId = accId,
                    contactId = conId,
                    categoryId = catId,
                    debitPaisa = debit,
                    creditPaisa = credit,
                    dateEpochDay = date
                )
            )
        }

        when (tx.txType) {
            "OPENING_BALANCE" -> {
                // Rule 10: Opening balance posts to opening equity/capital, never income.
                if (tx.amountPaisa >= 0) {
                    addLine(LEDGER_ASSET_ACCOUNT, "Account Opening Balance", tx.amountPaisa, 0L, accId = tx.accountId)
                    addLine(LEDGER_EQUITY_OPENING, "Opening Equity / Capital", 0L, tx.amountPaisa)
                } else {
                    val absAmt = -tx.amountPaisa
                    addLine(LEDGER_EQUITY_OPENING, "Opening Equity / Capital", absAmt, 0L)
                    addLine(LEDGER_ASSET_ACCOUNT, "Account Opening Balance", 0L, absAmt, accId = tx.accountId)
                }
            }

            "CASH_INCOME" -> {
                // Rule 2: Cash income recognized when money is received.
                addLine(LEDGER_ASSET_ACCOUNT, "Cash/Wallet Inflow", tx.amountPaisa, 0L, accId = tx.accountId)
                addLine(LEDGER_INCOME_CASH, "Cash Income", 0L, tx.amountPaisa, catId = tx.categoryId, conId = tx.contactId)
            }

            "CREDIT_INCOME" -> {
                // Rule 3 & 5: Credit sale creates receivable, not cash income; partial payment recognizes only settled amount.
                val settled = tx.settledAmountPaisa.coerceIn(0L, tx.amountPaisa)
                val due = (tx.amountPaisa - settled).coerceAtLeast(0L)
                if (settled > 0L && tx.accountId != null) {
                    addLine(LEDGER_ASSET_ACCOUNT, "Partial Cash Received", settled, 0L, accId = tx.accountId)
                    addLine(LEDGER_INCOME_CASH, "Cash Income (Partial Settlement)", 0L, settled, catId = tx.categoryId, conId = tx.contactId)
                }
                if (due > 0L) {
                    addLine(LEDGER_ASSET_RECEIVABLE, "Accounts Receivable", due, 0L, conId = tx.contactId)
                    addLine(LEDGER_INCOME_UNREALIZED, "Unrealized Credit Sale", 0L, due, catId = tx.categoryId, conId = tx.contactId)
                }
            }

            "CASH_EXPENSE" -> {
                // Rule 2: Cash expense recognized when money is paid.
                addLine(LEDGER_EXPENSE_CASH, "Cash Expense", tx.amountPaisa, 0L, catId = tx.categoryId, conId = tx.contactId)
                addLine(LEDGER_ASSET_ACCOUNT, "Cash/Wallet Outflow", 0L, tx.amountPaisa, accId = tx.accountId)
            }

            "CREDIT_EXPENSE" -> {
                // Rule 4 & 5: Credit purchase creates payable, not cash expense; partial payment recognizes only settled amount.
                val settled = tx.settledAmountPaisa.coerceIn(0L, tx.amountPaisa)
                val due = (tx.amountPaisa - settled).coerceAtLeast(0L)
                if (settled > 0L && tx.accountId != null) {
                    addLine(LEDGER_EXPENSE_CASH, "Cash Expense (Partial Settlement)", settled, 0L, catId = tx.categoryId, conId = tx.contactId)
                    addLine(LEDGER_ASSET_ACCOUNT, "Partial Cash Paid", 0L, settled, accId = tx.accountId)
                }
                if (due > 0L) {
                    addLine(LEDGER_EXPENSE_UNREALIZED, "Unpaid Credit Purchase", due, 0L, catId = tx.categoryId, conId = tx.contactId)
                    addLine(LEDGER_LIABILITY_PAYABLE, "Accounts Payable", 0L, due, conId = tx.contactId)
                }
            }

            "RECEIVE_DUE" -> {
                // Rule 3 & 5: Customer settlement reduces receivable and recognizes cash income for settled amount.
                addLine(LEDGER_ASSET_ACCOUNT, "Customer Due Received", tx.amountPaisa, 0L, accId = tx.accountId)
                addLine(LEDGER_ASSET_RECEIVABLE, "Accounts Receivable Settled", 0L, tx.amountPaisa, conId = tx.contactId)
                // Recognize Cash-Basis Income upon settlement
                addLine(LEDGER_INCOME_UNREALIZED, "Realize Credit Sale on Settlement", tx.amountPaisa, 0L, catId = tx.categoryId, conId = tx.contactId)
                addLine(LEDGER_INCOME_CASH, "Cash Income (Due Settled)", 0L, tx.amountPaisa, catId = tx.categoryId, conId = tx.contactId)
            }

            "PAY_DUE" -> {
                // Rule 4 & 5: Supplier settlement reduces payable and recognizes cash expense for settled amount.
                addLine(LEDGER_LIABILITY_PAYABLE, "Accounts Payable Settled", tx.amountPaisa, 0L, conId = tx.contactId)
                addLine(LEDGER_ASSET_ACCOUNT, "Supplier Due Paid", 0L, tx.amountPaisa, accId = tx.accountId)
                // Recognize Cash-Basis Expense upon settlement
                addLine(LEDGER_EXPENSE_CASH, "Cash Expense (Payable Settled)", tx.amountPaisa, 0L, catId = tx.categoryId, conId = tx.contactId)
                addLine(LEDGER_EXPENSE_UNREALIZED, "Realize Credit Purchase on Settlement", 0L, tx.amountPaisa, catId = tx.categoryId, conId = tx.contactId)
            }

            "TRANSFER" -> {
                // Rule 6: Transfers change account balances only, no P&L impact.
                addLine(LEDGER_ASSET_ACCOUNT, "Transfer In", tx.amountPaisa, 0L, accId = tx.toAccountId)
                addLine(LEDGER_ASSET_ACCOUNT, "Transfer Out", 0L, tx.amountPaisa, accId = tx.accountId)
            }

            "OWNER_INVESTMENT" -> {
                // Rule 7: Owner investment increases cash/equity, not income.
                addLine(LEDGER_ASSET_ACCOUNT, "Owner Capital Injection", tx.amountPaisa, 0L, accId = tx.accountId)
                addLine(LEDGER_EQUITY_OWNER_CAPITAL, "Owner Equity / Capital", 0L, tx.amountPaisa)
            }

            "OWNER_WITHDRAWAL" -> {
                // Rule 8: Owner withdrawal is not operating expense.
                addLine(LEDGER_EQUITY_OWNER_DRAWING, "Owner Drawing / Withdrawal", tx.amountPaisa, 0L)
                addLine(LEDGER_ASSET_ACCOUNT, "Owner Cash Withdrawal", 0L, tx.amountPaisa, accId = tx.accountId)
            }

            "LOAN_TAKEN" -> {
                // Rule 7: Loan increases cash and liability, not income.
                addLine(LEDGER_ASSET_ACCOUNT, "Loan Proceeds Received", tx.amountPaisa, 0L, accId = tx.accountId)
                addLine(LEDGER_LIABILITY_LOAN, "Loan Liability", 0L, tx.amountPaisa, conId = tx.contactId)
            }

            "LOAN_REPAID" -> {
                // Rule 8: Loan principal repayment is not operating expense; loan interest is expense.
                val interest = tx.loanInterestPaisa.coerceIn(0L, tx.amountPaisa)
                val principal = (tx.amountPaisa - interest).coerceAtLeast(0L)
                if (principal > 0L) {
                    addLine(LEDGER_LIABILITY_LOAN, "Loan Principal Repayment", principal, 0L, conId = tx.contactId)
                }
                if (interest > 0L) {
                    addLine(LEDGER_EXPENSE_CASH, "Loan Interest Expense", interest, 0L, catId = tx.categoryId, conId = tx.contactId)
                }
                addLine(LEDGER_ASSET_ACCOUNT, "Loan Repayment Outflow", 0L, tx.amountPaisa, accId = tx.accountId)
            }

            "REFUND_INCOME" -> {
                // Rule 9: Refund to customer reverses cash income.
                addLine(LEDGER_INCOME_CASH, "Sales Refund / Return", tx.amountPaisa, 0L, catId = tx.categoryId, conId = tx.contactId)
                addLine(LEDGER_ASSET_ACCOUNT, "Refund Paid Out", 0L, tx.amountPaisa, accId = tx.accountId)
            }

            "REFUND_EXPENSE" -> {
                // Rule 9: Refund from supplier reverses cash expense.
                addLine(LEDGER_ASSET_ACCOUNT, "Expense Refund Received", tx.amountPaisa, 0L, accId = tx.accountId)
                addLine(LEDGER_EXPENSE_CASH, "Purchase Refund / Rebate", 0L, tx.amountPaisa, catId = tx.categoryId, conId = tx.contactId)
            }

            else -> {
                // "ADJUSTMENT"
                addLine(LEDGER_ASSET_ACCOUNT, "Balance Adjustment", tx.amountPaisa, 0L, accId = tx.accountId)
                addLine(LEDGER_EQUITY_OWNER_CAPITAL, "Adjustment Equity Offset", 0L, tx.amountPaisa)
            }
        }

        val totalDebit = lines.sumOf { it.debitPaisa }
        val totalCredit = lines.sumOf { it.creditPaisa }
        val memo = if (memoPrefix.isNotBlank()) "$memoPrefix: ${tx.title}" else tx.title

        val entry = JournalEntryEntity(
            id = entryId,
            transactionId = tx.id,
            entryType = entryType,
            dateEpochDay = date,
            memo = memo,
            totalDebitPaisa = totalDebit,
            totalCreditPaisa = totalCredit,
            isBalanced = (totalDebit == totalCredit)
        )
        return GeneratedJournalBundle(entry, lines)
    }

    /**
     * Rule 9 & FR-07: Creates an exact compensating Reversal Journal Entry that swaps Debit & Credit
     * for all prior lines of a transaction, preserving full immutable audit history.
     */
    fun createReversalJournal(
        transactionId: String,
        existingLinesForTx: List<JournalLineEntity>,
        reversalDateEpochDay: Long,
        reason: String
    ): GeneratedJournalBundle {
        val entryId = UUID.randomUUID().toString()
        // Compute net active effect per (ledgerCode, ledgerName, accountId, contactId, categoryId)
        val reversedLines = existingLinesForTx.map { line ->
            JournalLineEntity(
                journalEntryId = entryId,
                transactionId = transactionId,
                ledgerCode = line.ledgerCode,
                ledgerName = "REVERSAL - ${line.ledgerName}",
                accountId = line.accountId,
                contactId = line.contactId,
                categoryId = line.categoryId,
                debitPaisa = line.creditPaisa,
                creditPaisa = line.debitPaisa,
                dateEpochDay = reversalDateEpochDay
            )
        }
        val totalDebit = reversedLines.sumOf { it.debitPaisa }
        val totalCredit = reversedLines.sumOf { it.creditPaisa }

        val entry = JournalEntryEntity(
            id = entryId,
            transactionId = transactionId,
            entryType = "REVERSAL",
            dateEpochDay = reversalDateEpochDay,
            memo = "Reversal ($reason)",
            totalDebitPaisa = totalDebit,
            totalCreditPaisa = totalCredit,
            isBalanced = (totalDebit == totalCredit)
        )
        return GeneratedJournalBundle(entry, reversedLines)
    }

    /**
     * Derives account balances strictly from journal_lines (Rule 11).
     */
    fun computeAccountBalances(
        accounts: List<AccountEntity>,
        journalLines: List<JournalLineEntity>
    ): List<AccountBalanceSummary> {
        val assetLinesByAccount = journalLines
            .filter { it.ledgerCode == LEDGER_ASSET_ACCOUNT && it.accountId != null }
            .groupBy { it.accountId!! }

        return accounts.map { acc ->
            val lines = assetLinesByAccount[acc.id].orEmpty()
            val inflow = lines.sumOf { it.debitPaisa }
            val outflow = lines.sumOf { it.creditPaisa }
            val txCount = lines.map { it.transactionId }.distinct().size
            AccountBalanceSummary(
                account = acc,
                currentBalancePaisa = inflow - outflow,
                totalInflowPaisa = inflow,
                totalOutflowPaisa = outflow,
                txCount = txCount
            )
        }
    }

    /**
     * Derives Customer Receivable & Supplier Payable sub-ledgers strictly from journal_lines.
     */
    fun computeContactLedgers(
        contacts: List<ContactEntity>,
        journalLines: List<JournalLineEntity>
    ): List<ContactLedgerSummary> {
        val linesByContact = journalLines
            .filter { it.contactId != null }
            .groupBy { it.contactId!! }

        return contacts.map { contact ->
            val lines = linesByContact[contact.id].orEmpty()
            val recLines = lines.filter { it.ledgerCode == LEDGER_ASSET_RECEIVABLE }
            val payLines = lines.filter { it.ledgerCode == LEDGER_LIABILITY_PAYABLE }

            val recDebit = recLines.sumOf { it.debitPaisa }
            val recCredit = recLines.sumOf { it.creditPaisa }
            val receivable = (recDebit - recCredit).coerceAtLeast(0L)

            val payCredit = payLines.sumOf { it.creditPaisa }
            val payDebit = payLines.sumOf { it.debitPaisa }
            val payable = (payCredit - payDebit).coerceAtLeast(0L)

            val totalBilled = if (contact.contactType == "CUSTOMER") recDebit else payCredit
            val totalSettled = if (contact.contactType == "CUSTOMER") recCredit else payDebit

            ContactLedgerSummary(
                contact = contact,
                receivablePaisa = receivable,
                payablePaisa = payable,
                totalBilledPaisa = totalBilled,
                totalSettledPaisa = totalSettled
            )
        }
    }

    /**
     * Computes comprehensive Cash-Basis P&L, Cashflow, and Balance Sheet indicators for a date range.
     */
    fun computePeriodSummary(
        startEpochDay: Long,
        endEpochDay: Long,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        allJournalLines: List<JournalLineEntity>
    ): PeriodFinancialSummary {
        val periodLines = allJournalLines.filter { it.dateEpochDay in startEpochDay..endEpochDay }

        // Cash Income in period: credit - debit on LEDGER_INCOME_CASH
        val incomeLines = periodLines.filter { it.ledgerCode == LEDGER_INCOME_CASH }
        val cashIncome = (incomeLines.sumOf { it.creditPaisa } - incomeLines.sumOf { it.debitPaisa }).coerceAtLeast(0L)

        // Cash Expense in period: debit - credit on LEDGER_EXPENSE_CASH
        val expenseLines = periodLines.filter { it.ledgerCode == LEDGER_EXPENSE_CASH }
        val cashExpense = (expenseLines.sumOf { it.debitPaisa } - expenseLines.sumOf { it.creditPaisa }).coerceAtLeast(0L)

        val cashProfit = cashIncome - cashExpense

        // Net Cashflow in period across all accounts
        val periodAccountLines = periodLines.filter { it.ledgerCode == LEDGER_ASSET_ACCOUNT }
        val netCashflow = periodAccountLines.sumOf { it.debitPaisa } - periodAccountLines.sumOf { it.creditPaisa }

        // Cumulative balance sheet metrics (up to endEpochDay)
        val cumulativeLines = allJournalLines.filter { it.dateEpochDay <= endEpochDay }
        val activeAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()

        val totalActiveCash = cumulativeLines
            .filter { it.ledgerCode == LEDGER_ASSET_ACCOUNT && it.accountId in activeAccountIds }
            .sumOf { it.debitPaisa - it.creditPaisa }

        val totalReceivable = cumulativeLines
            .filter { it.ledgerCode == LEDGER_ASSET_RECEIVABLE }
            .sumOf { it.debitPaisa - it.creditPaisa }
            .coerceAtLeast(0L)

        val totalPayable = cumulativeLines
            .filter { it.ledgerCode == LEDGER_LIABILITY_PAYABLE }
            .sumOf { it.creditPaisa - it.debitPaisa }
            .coerceAtLeast(0L)

        val openingEquity = cumulativeLines
            .filter { it.ledgerCode == LEDGER_EQUITY_OPENING }
            .sumOf { it.creditPaisa - it.debitPaisa }

        val ownerCapital = cumulativeLines
            .filter { it.ledgerCode == LEDGER_EQUITY_OWNER_CAPITAL }
            .sumOf { it.creditPaisa - it.debitPaisa }

        val ownerWithdrawal = cumulativeLines
            .filter { it.ledgerCode == LEDGER_EQUITY_OWNER_DRAWING }
            .sumOf { it.debitPaisa - it.creditPaisa }
            .coerceAtLeast(0L)

        val loanLiability = cumulativeLines
            .filter { it.ledgerCode == LEDGER_LIABILITY_LOAN }
            .sumOf { it.creditPaisa - it.debitPaisa }
            .coerceAtLeast(0L)

        val categoryMap = categories.associateBy { it.id }

        val incomeByCategory = incomeLines
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapNotNull { (catId, lines) ->
                val cat = categoryMap[catId] ?: return@mapNotNull null
                val amt = (lines.sumOf { it.creditPaisa } - lines.sumOf { it.debitPaisa }).coerceAtLeast(0L)
                if (amt <= 0L) null else CategoryReportRow(
                    category = cat,
                    totalPaisa = amt,
                    percentage = if (cashIncome > 0L) (amt.toFloat() / cashIncome.toFloat()) * 100f else 0f
                )
            }
            .sortedByDescending { it.totalPaisa }

        val expenseByCategory = expenseLines
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapNotNull { (catId, lines) ->
                val cat = categoryMap[catId] ?: return@mapNotNull null
                val amt = (lines.sumOf { it.debitPaisa } - lines.sumOf { it.creditPaisa }).coerceAtLeast(0L)
                if (amt <= 0L) null else CategoryReportRow(
                    category = cat,
                    totalPaisa = amt,
                    percentage = if (cashExpense > 0L) (amt.toFloat() / cashExpense.toFloat()) * 100f else 0f
                )
            }
            .sortedByDescending { it.totalPaisa }

        return PeriodFinancialSummary(
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            cashIncomePaisa = cashIncome,
            cashExpensePaisa = cashExpense,
            cashProfitPaisa = cashProfit,
            netCashflowPaisa = netCashflow,
            totalActiveCashBalancePaisa = totalActiveCash,
            totalReceivablePaisa = totalReceivable,
            totalPayablePaisa = totalPayable,
            ownerCapitalPaisa = ownerCapital,
            ownerWithdrawalPaisa = ownerWithdrawal,
            loanLiabilityPaisa = loanLiability,
            openingEquityPaisa = openingEquity,
            incomeByCategory = incomeByCategory,
            expenseByCategory = expenseByCategory
        )
    }

    /**
     * Verifies that every journal entry is balanced (Debits == Credits) and that all lines reconcile.
     */
    fun verifyLedgerReconciliation(
        entries: List<JournalEntryEntity>,
        lines: List<JournalLineEntity>
    ): ReconciliationReport {
        val linesByEntry = lines.groupBy { it.journalEntryId }
        val unbalanced = mutableListOf<String>()

        for (entry in entries) {
            val eLines = linesByEntry[entry.id].orEmpty()
            val d = eLines.sumOf { it.debitPaisa }
            val c = eLines.sumOf { it.creditPaisa }
            if (d != c || d != entry.totalDebitPaisa || c != entry.totalCreditPaisa || !entry.isBalanced) {
                unbalanced.add(entry.id)
            }
        }

        val totalDebit = lines.sumOf { it.debitPaisa }
        val totalCredit = lines.sumOf { it.creditPaisa }

        return ReconciliationReport(
            isHealthy = unbalanced.isEmpty() && (totalDebit == totalCredit),
            totalEntries = entries.size,
            totalLines = lines.size,
            totalDebitPaisa = totalDebit,
            totalCreditPaisa = totalCredit,
            unbalancedEntryIds = unbalanced
        )
    }
}
