package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.ui.LedgerViewModel
import com.example.ui.theme.EmeraldPrimaryContainerDark
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.JetBrainsMono
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: LedgerViewModel,
    seededCategories: List<CategoryEntity>
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var lang by rememberSaveable { mutableStateOf("EN") }
    var numberFormat by rememberSaveable { mutableStateOf("BD_LAKH") }
    var dateFormat by rememberSaveable { mutableStateOf("DD_MM_YYYY") }

    var ownerName by rememberSaveable { mutableStateOf("") }
    var ownerPhone by rememberSaveable { mutableStateOf("") }
    var businessName by rememberSaveable { mutableStateOf("") }
    var businessType by rememberSaveable { mutableStateOf("Retail & General Store") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var businessContact by rememberSaveable { mutableStateOf("") }

    var accountType by rememberSaveable { mutableStateOf("CASH") }
    var accountName by rememberSaveable { mutableStateOf("Cash Drawer") }
    var accountNameBn by rememberSaveable { mutableStateOf("ক্যাশ বাক্স") }
    var accountNumber by rememberSaveable { mutableStateOf("") }
    var openingBalanceInput by rememberSaveable { mutableStateOf("0") }
    var openingDateOffsetDays by rememberSaveable { mutableLongStateOf(0L) }

    var pinInput by rememberSaveable { mutableStateOf("") }
    var pinHint by rememberSaveable { mutableStateOf("") }
    var biometricEnabled by rememberSaveable { mutableStateOf(true) }
    var autoBackupEnabled by rememberSaveable { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val todayEpoch = remember { Formatters.currentEpochDay() }
    val openingEpochDay = todayEpoch + openingDateOffsetDays

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldPrimaryLight, EmeraldPrimaryContainerDark)
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "HishabKhata Logo",
                                    tint = GoldSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "HishabKhata • হিসাবখাতা",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Formatters.tr(
                                        lang,
                                        "Offline Cash-Basis & Double-Entry Business Tracker",
                                        "অফলাইন নগদ ও ডাবল-এন্ট্রি ব্যবসা হিসাব"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        // Quick Language Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .testTag("onboarding_lang_toggle")
                                .clickable { lang = if (lang == "EN") "BN" else "EN" }
                        ) {
                            Text(
                                text = if (lang == "EN") "বাংলা" else "English",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { (step + 1) / 4f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GoldSecondaryDark,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Formatters.tr(
                            lang,
                            "Step ${step + 1} of 4 • 100% Offline SQLite Ledger",
                            "ধাপ ${Formatters.toBanglaDigits((step + 1).toString())}/৪ • সম্পূর্ণ অফলাইন ডাটাবেস"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            AnimatedContent(targetState = step, label = "onboarding_step") { currentStep ->
                when (currentStep) {
                    0 -> {
                        // Step 1: Language, Currency (BDT), Number & Date Formats
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.tr(lang, "1. Language & BDT Format Preferences", "১. ভাষা এবং টাকার ফরম্যাট নির্বাচন"),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = Formatters.tr(lang, "Choose Interface Language", "অ্যাপের ভাষা নির্বাচন করুন"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilterChip(
                                        selected = lang == "EN",
                                        onClick = { lang = "EN" },
                                        label = { Text("English (EN)") },
                                        modifier = Modifier.testTag("chip_lang_en")
                                    )
                                    FilterChip(
                                        selected = lang == "BN",
                                        onClick = { lang = "BN" },
                                        label = { Text("বাংলা (Bangla)") },
                                        modifier = Modifier.testTag("chip_lang_bn")
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = Formatters.tr(lang, "Currency & Number Grouping (Default: BDT ৳)", "মুদ্রা ও সংখ্যার ফরম্যাট (ডিফল্ট: BDT ৳)"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilterChip(
                                        selected = numberFormat == "BD_LAKH",
                                        onClick = { numberFormat = "BD_LAKH" },
                                        label = { Text("South Asian (৳ 1,25,000)") }
                                    )
                                    FilterChip(
                                        selected = numberFormat == "INTL",
                                        onClick = { numberFormat = "INTL" },
                                        label = { Text("International (৳ 125,000)") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = Formatters.tr(lang, "Date Display Format", "তারিখের ফরম্যাট"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilterChip(
                                        selected = dateFormat == "DD_MM_YYYY",
                                        onClick = { dateFormat = "DD_MM_YYYY" },
                                        label = { Text("04 Nov 2026") }
                                    )
                                    FilterChip(
                                        selected = dateFormat == "YYYY_MM_DD",
                                        onClick = { dateFormat = "YYYY_MM_DD" },
                                        label = { Text("2026-11-04 (ISO)") }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Step 2: Owner & Business Profile
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.tr(lang, "2. Owner & Single Business Profile", "২. মালিক এবং ব্যবসার প্রোফাইল"),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text(Formatters.tr(lang, "Owner Full Name *", "মালিকের নাম *")) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_owner_name")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = ownerPhone,
                                    onValueChange = { ownerPhone = it },
                                    label = { Text(Formatters.tr(lang, "Owner Phone Number (Used for PIN Recovery) *", "মালিকের মোবাইল নম্বর *")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_owner_phone")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it },
                                    label = { Text(Formatters.tr(lang, "Business / Shop Name *", "ব্যবসা বা দোকানের নাম *")) },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_business_name")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = Formatters.tr(lang, "Business Category", "ব্যবসার ধরন"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val types = listOf(
                                    "Retail & General Store",
                                    "Pharmacy & Medical",
                                    "Wholesale & Distribution",
                                    "Electronics & Mobile",
                                    "Service & Workshop",
                                    "Restaurant & Food"
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    types.forEach { t ->
                                        FilterChip(
                                            selected = businessType == t,
                                            onClick = { businessType = t },
                                            label = { Text(t) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = businessAddress,
                                    onValueChange = { businessAddress = it },
                                    label = { Text(Formatters.tr(lang, "Business Address / Market", "ব্যবসার ঠিকানা / মার্কেট")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    2 -> {
                        // Step 3: Mandatory First Financial Account & Opening Balance
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.tr(lang, "3. Primary Financial Account & Opening Balance", "৩. প্রথম আর্থিক অ্যাকাউন্ট ও প্রারম্ভিক ব্যালেন্স"),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = Formatters.tr(
                                        lang,
                                        "Select Account Type (Cash, Bank, bKash, Nagad, Rocket, or Wallet):",
                                        "অ্যাকাউন্টের ধরন নির্বাচন করুন:"
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val accountTypes = listOf(
                                    Triple("CASH", "Cash Drawer", "ক্যাশ বাক্স"),
                                    Triple("BKASH", "bKash Wallet", "বিকাশ ওয়ালেট"),
                                    Triple("NAGAD", "Nagad Wallet", "নগদ ওয়ালেট"),
                                    Triple("ROCKET", "Rocket Account", "রকেট অ্যাকাউন্ট"),
                                    Triple("BANK", "Bank Account", "ব্যাংক অ্যাকাউন্ট"),
                                    Triple("WALLET", "Other Wallet", "অন্যান্য ওয়ালেট")
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    accountTypes.forEach { (code, defEn, defBn) ->
                                        FilterChip(
                                            selected = accountType == code,
                                            onClick = {
                                                accountType = code
                                                accountName = defEn
                                                accountNameBn = defBn
                                            },
                                            label = { Text(code) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = accountName,
                                    onValueChange = { accountName = it },
                                    label = { Text(Formatters.tr(lang, "Account Name *", "অ্যাকাউন্টের নাম *")) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_first_account_name")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = openingBalanceInput,
                                    onValueChange = { openingBalanceInput = it },
                                    label = { Text(Formatters.tr(lang, "Opening Balance in BDT (0 is valid) *", "প্রারম্ভিক ব্যালেন্স (০ দিলেও চলবে) *")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    prefix = { Text("৳ ") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_opening_balance")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = Formatters.tr(lang, "Opening Date:", "প্রারম্ভিক তারিখ:") + " " +
                                            Formatters.formatEpochDay(openingEpochDay, dateFormat, lang),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { openingDateOffsetDays -= 1L }) {
                                            Text("-1d")
                                        }
                                        OutlinedButton(onClick = { openingDateOffsetDays = 0L }) {
                                            Text(Formatters.tr(lang, "Today", "আজ"))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = Formatters.tr(
                                                lang,
                                                "Accounting Rule 10: Opening balance is posted to Opening Equity/Capital (DR Asset / CR Opening Equity) — never counted as operating income.",
                                                "হিসাববিজ্ঞান নিয়ম ১০: প্রারম্ভিক ব্যালেন্স ওপেনিং ইক্যুইটি/মূলধনে জমা হবে, কখনোই আয় হিসেবে গণ্য হবে না।"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = Formatters.tr(
                                        lang,
                                        "Seeded Categories Ready (${seededCategories.size} editable categories):",
                                        "ডিফল্ট ক্যাটাগরি প্রস্তুত (${seededCategories.size}টি পরিবর্তনযোগ্য ক্যাটাগরি):"
                                    ),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = seededCategories.take(6).joinToString(" • ") {
                                        if (lang == "BN") it.nameBn else it.name
                                    } + " ...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        // Step 4: Security (PIN + Biometric) & Daily Automatic Local Backup
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.tr(lang, "4. Security PIN & Automatic Local Backup", "৪. নিরাপত্তা পিন এবং অটোমেটিক ব্যাকআপ"),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 6) pinInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text(Formatters.tr(lang, "Set 4 to 6 Digit Security PIN *", "৪ থেকে ৬ সংখ্যার গোপন পিন দিন *")) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_onboarding_pin")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = pinHint,
                                    onValueChange = { pinHint = it },
                                    label = { Text(Formatters.tr(lang, "Optional PIN Hint (For Local Recovery)", "পিন মনে রাখার ইঙ্গিত (ঐচ্ছিক)")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Formatters.tr(lang, "Enable Biometric Unlock", "বায়োমেট্রিক / ফিঙ্গারপ্রিন্ট আনলক"),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = Formatters.tr(lang, "Quickly unlock your ledger alongside PIN", "পিনের পাশাপাশি দ্রুত আনলক করুন"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = biometricEnabled,
                                        onCheckedChange = { biometricEnabled = it }
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Formatters.tr(lang, "Daily Automatic Local Backup (Keep Latest 7)", "প্রতিদিন অটোমেটিক লোকাল ব্যাকআপ (সর্বশেষ ৭টি)"),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = Formatters.tr(
                                                lang,
                                                "Includes SQLite data, attachments & SHA-256 checksum",
                                                "এসকিউলাইট ডাটা, রশিদ ও চেকসামসহ নিরাপদ ব্যাকআপ"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoBackupEnabled,
                                        onCheckedChange = { autoBackupEnabled = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Navigation Footer Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = {
                            validationError = null
                            step -= 1
                        },
                        modifier = Modifier.testTag("onboarding_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Formatters.tr(lang, "Back", "পিছনে"))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        validationError = null
                        when (step) {
                            0 -> step = 1
                            1 -> {
                                if (ownerName.isBlank() || businessName.isBlank()) {
                                    validationError = Formatters.tr(
                                        lang,
                                        "Please enter both Owner Name and Business Name.",
                                        "অনুগ্রহ করে মালিকের নাম এবং ব্যবসার নাম লিখুন।"
                                    )
                                } else {
                                    step = 2
                                }
                            }
                            2 -> {
                                val parsedPaisa = Formatters.parseTakaToPaisa(openingBalanceInput)
                                if (accountName.isBlank()) {
                                    validationError = Formatters.tr(
                                        lang,
                                        "Account name is required.",
                                        "অ্যাকাউন্টের নাম আবশ্যক।"
                                    )
                                } else if (parsedPaisa == null || parsedPaisa < 0L) {
                                    validationError = Formatters.tr(
                                        lang,
                                        "Enter a valid Opening Balance (0 or positive amount).",
                                        "সঠিক প্রারম্ভিক ব্যালেন্স দিন (০ বা ধনাত্মক সংখ্যা)।"
                                    )
                                } else {
                                    step = 3
                                }
                            }
                            3 -> {
                                val cleanPin = Formatters.normalizeDigitsToAscii(pinInput)
                                val parsedPaisa = Formatters.parseTakaToPaisa(openingBalanceInput) ?: 0L
                                if (cleanPin.length < 4) {
                                    validationError = Formatters.tr(
                                        lang,
                                        "Security PIN is required (minimum 4 digits).",
                                        "কমপক্ষে ৪ সংখ্যার গোপন পিন দেওয়া আবশ্যক।"
                                    )
                                } else {
                                    viewModel.completeOnboarding(
                                        language = lang,
                                        ownerName = ownerName,
                                        ownerPhone = ownerPhone,
                                        businessName = businessName,
                                        businessType = businessType,
                                        businessAddress = businessAddress,
                                        businessContact = businessContact,
                                        numberFormat = numberFormat,
                                        dateFormat = dateFormat,
                                        firstAccountName = accountName,
                                        firstAccountNameBn = accountNameBn,
                                        firstAccountType = accountType,
                                        firstAccountNumber = accountNumber,
                                        openingBalancePaisa = parsedPaisa,
                                        openingDateEpochDay = openingEpochDay,
                                        rawPin = cleanPin,
                                        pinHint = pinHint,
                                        biometricEnabled = biometricEnabled,
                                        autoBackupEnabled = autoBackupEnabled
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("onboarding_next_or_finish_btn")
                ) {
                    Text(
                        if (step < 3) Formatters.tr(lang, "Continue", "পরবর্তী")
                        else Formatters.tr(lang, "Launch Ledger Dashboard", "ড্যাশবোর্ডে প্রবেশ করুন")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        if (step < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    viewModel: LedgerViewModel,
    businessName: String,
    ownerName: String,
    pinHint: String,
    biometricEnabled: Boolean,
    lang: String
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryPhone by remember { mutableStateOf("") }
    var recoveryNewPin by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EmeraldPrimaryContainerDark, Color(0xFF090D16))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked Ledger",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = businessName.ifBlank { "HishabKhata" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Formatters.tr(
                        lang,
                        "Private Offline Ledger • Owner: ${ownerName.ifBlank { "Owner" }}",
                        "ব্যক্তিগত অফলাইন হিসাবখাতা • মালিক: ${ownerName.ifBlank { "Owner" }}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 6) {
                            pinInput = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        }
                    },
                    label = { Text(Formatters.tr(lang, "Enter Security PIN", "গোপন পিন দিন")) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_pin_input")
                )

                if (pinHint.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hint: $pinHint",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.attemptPinUnlock(pinInput) { ok, err ->
                                if (!ok) errorMsg = err
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("unlock_pin_button")
                    ) {
                        Text(Formatters.tr(lang, "Unlock", "আনলক করুন"))
                    }

                    if (biometricEnabled) {
                        OutlinedButton(
                            onClick = { viewModel.unlockWithBiometricSimulator() },
                            modifier = Modifier.testTag("unlock_biometric_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Unlock")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.tr(lang, "Biometric", "বায়োমেট্রিক"))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { showRecoveryDialog = true }) {
                    Text(Formatters.tr(lang, "Forgot PIN? Local Owner Recovery", "পিন ভুলে গেছেন? পুনরুদ্ধার করুন"))
                }
            }
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(Formatters.tr(lang, "Local PIN Recovery", "লোকাল পিন পুনরুদ্ধার")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = Formatters.tr(
                            lang,
                            "Verify your registered Owner Phone Number to set a new PIN locally without losing your database.",
                            "আপনার নিবন্ধিত মালিকের মোবাইল নম্বর যাচাই করে নতুন পিন সেট করুন।"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = recoveryPhone,
                        onValueChange = { recoveryPhone = it },
                        label = { Text("Registered Owner Phone") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = recoveryNewPin,
                        onValueChange = { if (it.length <= 6) recoveryNewPin = it.filter { c -> c.isDigit() } },
                        label = { Text("New 4–6 Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (recoveryError != null) {
                        Text(recoveryError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetPinWithOwnerPhoneVerification(recoveryPhone, recoveryNewPin) { ok, msg ->
                        if (ok) {
                            showRecoveryDialog = false
                        } else {
                            recoveryError = msg
                        }
                    }
                }) {
                    Text("Reset PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
