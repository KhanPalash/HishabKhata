# Product Feature Catalogue

## Product boundary

An offline-first Android finance tracker for one owner and one universal local business. It uses cash-basis reporting while retaining customer receivables, supplier payables, and a double-entry-ready internal journal. There is one business profile, no cloud dependency, and no final app name selected yet.

## Feature catalogue

### 1. Onboarding and business setup

- Welcome with Bangla/English selection.
- Owner profile: name, phone, optional photo.
- Single business profile: name, business type, address, contact details.
- Currency defaults to BDT (৳); configurable display/date/number formats.
- Mandatory creation of at least one financial account and an opening balance (zero is valid).
- Account types: cash, bank, bKash, Nagad, Rocket, and other mobile wallet/account.
- Opening balance is dated and posted against opening equity/capital, never income.
- Seeded income/expense categories, editable after setup.
- Security and automatic backup setup before entering the dashboard.

### 2. Dashboard

- Total balance across active cash, bank, and wallet accounts.
- Period cash income, cash expense, and cash-basis profit.
- Separate receivable, payable, owner capital, owner withdrawal, and loan indicators.
- Recent transactions, overdue items, upcoming reminders, and backup status.
- Quick actions: add transaction, receive due, pay due, transfer, add contact, view reports.

### 3. Smart transaction entry

- Basic-first form: type, amount, category, account, payment status.
- Advanced fields: customer/supplier, date, payment details, description, reference, internal note, attachment, partial payment.
- Dynamic forms for cash income, credit income, cash expense, credit expense, receive due, pay due, owner investment/withdrawal, loan, refund/adjustment, and account transfer.
- Amount auto-focus; last-used account remembered; recent/type-appropriate categories suggested.
- Real-time validation, duplicate warning (optional), unsaved-change warning, save confirmation.
- Works fully offline and records locally.

### 4. Attachments

- Optional camera capture, gallery image, PDF, receipt, or payment proof.
- Local file storage with database metadata and configurable size/type limits.
- Included in local backup/export; removable without deleting the transaction.

### 5. Contacts and sub-ledgers

- Customer and supplier records with phone/address/notes.
- Per-contact receivable/payable balance, transaction history, settlement history, and statement export.
- Partial settlement and overpayment prevention/handling.

### 6. Accounts and transfers

- Multiple active accounts; account-specific balance and statement.
- Add, edit, archive accounts; accounts with history cannot be deleted.
- Transfers move value between accounts without income/expense impact.
- Reconciliation view derived from journal entries.

### 7. Recurring transactions and reminders (Q15/Q16)

- Automatic recurring transactions plus manual recurring setup.
- Frequencies: daily, weekly, monthly, yearly, and custom interval; start/end or indefinite; next-run preview.
- Automatic runs create a pending/posted transaction according to the chosen policy and remain auditable.
- Due reminders for customer receivables and supplier payables.
- Recurring bill reminders for rent, payroll, utilities, subscriptions, and similar obligations.
- Local notifications with snooze, dismiss, mark paid, and reminder settings; no server required.

### 8. Accounting and ledger

- Cash-basis income and expense recognition when money is actually received/paid.
- Credit sale/purchase creates sub-ledger receivable/payable but no cash P&L impact until settlement.
- Partial payment recognizes only the settled amount.
- Owner investment/withdrawal, loans, transfers, and opening balances are balance-sheet/cashflow events, not operating P&L.
- Refunds and adjustments use reversing/correcting entries.
- Double-entry-ready journal with balanced debit/credit lines; balances and reports are derived from the ledger.

### 9. Editing, deletion, and audit

- Transactions are always editable.
- Edits create reversal/replacement or adjustment journal entries; original posted values remain in history.
- Delete means logical deletion/reversal, never raw row removal.
- Audit log records transaction ID, action, before/after values, timestamp, actor (owner), and optional reason.
- Unlimited historical reporting and filtering.

### 10. Reports and exports

- Custom unlimited date range plus day/week/month/year presets and comparisons.
- Cash income, cash expense, cash-basis P&L, cashflow, account statement, transaction register, receivable, payable, category, and contact reports.
- PDF and CSV export; localized labels, BDT formatting, and deterministic totals.
- Preview before export; large reports generated without blocking data entry.

### 11. Backup and restore

- Daily local automatic backup when device conditions allow.
- Retain the latest seven automatic backups, rotating older copies.
- Manual exports are separately retained and never removed by automatic rotation.
- Backup includes SQLite database, schema/version metadata, attachments, and integrity checksum.
- Restore flow lists backup date/type, validates checksum/schema, warns about overwrite, and records restore audit event.
- Optional user-selected folder/share destination; no cloud sync required.

### 12. Security and privacy

- PIN required; biometric unlock optional when device supports it.
- Lock on launch/background timeout; rate-limit failed PIN attempts.
- Sensitive local data protected using Android platform encryption/keystore where available.
- No account, analytics, or business data sent to a server.
- Explicit export/share confirmation and private attachment handling.

### 13. Preferences and accessibility

- Bangla/English; light/dark/system themes.
- BDT default, configurable locale formatting.
- Large text compatibility, content descriptions, contrast, touch targets, and screen-reader-friendly forms.
- Notification, backup, reminder, and confirmation preferences.

## Explicitly out of scope

Multi-user roles, multi-business/branch management, cloud synchronization, inventory, POS, payroll processing, invoicing marketplace, payment gateway integration, tax filing, and online banking feeds.

## Release shape

1. Foundation: onboarding, SQLite schema, accounts, opening balances, security, core ledger.
2. Daily operations: Smart Entry, contacts, receivable/payable, transfers, edit/audit.
3. Insight and continuity: reports, PDF/CSV, backups/restore, attachments, localization/themes.
4. Automation: recurring transactions, due/bill reminders, notification controls, hardening and accessibility.
