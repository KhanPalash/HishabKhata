package com.example

import com.example.data.AccountEntity
import com.example.data.CategoryEntity
import com.example.data.ContactEntity
import com.example.data.JournalEntryEntity
import com.example.data.JournalLineEntity
import com.example.data.TransactionEntity
import com.example.domain.AccountingEngine
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun accountingEngine_enforcesCashBasisAndDoubleEntryRules() {
    val cashAcc = AccountEntity(id = "acc_cash", name = "Cash", nameBn = "ক্যাশ", accountType = "CASH")
    val bkashAcc = AccountEntity(id = "acc_bkash", name = "bKash", nameBn = "বিকাশ", accountType = "BKASH")
    val salesCat = CategoryEntity(id = "cat_sales", name = "Sales", nameBn = "বিক্রয়", categoryType = "INCOME")
    val rentCat = CategoryEntity(id = "cat_rent", name = "Rent", nameBn = "ভাড়া", categoryType = "EXPENSE")
    val customer = ContactEntity(id = "con_cust", name = "Rahim", contactType = "CUSTOMER")

    val entries = mutableListOf<JournalEntryEntity>()
    val lines = mutableListOf<JournalLineEntity>()

    // 1. Rule 10: Opening balance posts to Opening Equity, never Cash Income
    val openTx = TransactionEntity(
      id = "tx_open",
      txType = "OPENING_BALANCE",
      amountPaisa = 50_000_00L,
      settledAmountPaisa = 50_000_00L,
      dueAmountPaisa = 0L,
      accountId = cashAcc.id,
      dateEpochDay = 100L,
      title = "Opening Cash"
    )
    val openBundle = AccountingEngine.createJournalForTransaction(openTx)
    assertTrue(openBundle.entry.isBalanced)
    entries.add(openBundle.entry)
    lines.addAll(openBundle.lines)

    // 2. Rule 3 & 5: Credit Sale of 10,000 BDT with 4,000 BDT immediate partial payment -> 4,000 Cash Income, 6,000 Receivable
    val creditSaleTx = TransactionEntity(
      id = "tx_credit_sale",
      txType = "CREDIT_INCOME",
      amountPaisa = 10_000_00L,
      settledAmountPaisa = 4_000_00L,
      dueAmountPaisa = 6_000_00L,
      accountId = cashAcc.id,
      categoryId = salesCat.id,
      contactId = customer.id,
      dateEpochDay = 101L,
      title = "Partial Credit Sale"
    )
    val creditBundle = AccountingEngine.createJournalForTransaction(creditSaleTx)
    assertTrue(creditBundle.entry.isBalanced)
    entries.add(creditBundle.entry)
    lines.addAll(creditBundle.lines)

    // 3. Rule 6: Transfer 5,000 BDT from Cash to bKash -> zero P&L impact
    val transferTx = TransactionEntity(
      id = "tx_transfer",
      txType = "TRANSFER",
      amountPaisa = 5_000_00L,
      settledAmountPaisa = 5_000_00L,
      dueAmountPaisa = 0L,
      accountId = cashAcc.id,
      toAccountId = bkashAcc.id,
      dateEpochDay = 102L,
      title = "Cash to bKash"
    )
    val transferBundle = AccountingEngine.createJournalForTransaction(transferTx)
    assertTrue(transferBundle.entry.isBalanced)
    entries.add(transferBundle.entry)
    lines.addAll(transferBundle.lines)

    val summary = AccountingEngine.computePeriodSummary(
      startEpochDay = 90L,
      endEpochDay = 110L,
      accounts = listOf(cashAcc, bkashAcc),
      categories = listOf(salesCat, rentCat),
      allJournalLines = lines
    )

    // Cash Income must be ONLY the 4,000 BDT settled portion (Opening balance & Transfer & 6,000 unpaid credit excluded!)
    assertEquals(4_000_00L, summary.cashIncomePaisa)
    assertEquals(0L, summary.cashExpensePaisa)
    assertEquals(6_000_00L, summary.totalReceivablePaisa)
    assertEquals(50_000_00L, summary.openingEquityPaisa)

    val recon = AccountingEngine.verifyLedgerReconciliation(entries, lines)
    assertTrue(recon.isHealthy)
  }

  @Test
  fun formatters_formatsSouthAsianBdtAndBanglaDigits() {
    val formattedEn = Formatters.formatBdt(12500000L, lang = "EN", numberFormat = "BD_LAKH")
    assertEquals("৳ 1,25,000", formattedEn)

    val formattedBn = Formatters.formatBdt(12500000L, lang = "BN", numberFormat = "BD_LAKH")
    assertEquals("৳ ১,২৫,০০০", formattedBn)
  }
}
