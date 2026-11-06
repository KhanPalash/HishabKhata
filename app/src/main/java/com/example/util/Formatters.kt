package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {
    private val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = ch - '0'
            if (idx in 0..9) sb.append(bnDigits[idx]) else sb.append(ch)
        }
        return sb.toString()
    }

    fun normalizeDigitsToAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val bnIdx = bnDigits.indexOf(ch)
            if (bnIdx >= 0) sb.append(enDigits[bnIdx]) else sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * Parses a user-entered BDT string (supports both English and Bangla digits) into integer paisa (1 BDT = 100 paisa).
     */
    fun parseTakaToPaisa(input: String): Long? {
        val clean = normalizeDigitsToAscii(input.trim()).replace(",", "").replace("৳", "").trim()
        if (clean.isEmpty()) return null
        val doubleVal = clean.toDoubleOrNull() ?: return null
        if (doubleVal.isNaN() || doubleVal.isInfinite()) return null
        return (doubleVal * 100.0).roundToLong()
    }

    fun paisaToTakaInputString(paisa: Long): String {
        if (paisa % 100L == 0L) {
            return (paisa / 100L).toString()
        }
        return String.format(Locale.US, "%.2f", paisa / 100.0)
    }

    /**
     * Formats paisa to BDT display string (e.g. "৳ 1,25,450.00" or "৳ ১,২৫,৪৫০.০০").
     * Uses South Asian Lakh/Crore grouping by default when numberFormat == "BD_LAKH".
     */
    fun formatBdt(
        paisa: Long,
        lang: String = "EN",
        numberFormat: String = "BD_LAKH",
        currencySymbol: String = "৳",
        showDecimals: Boolean = true
    ): String {
        val negative = paisa < 0
        val absPaisa = abs(paisa)
        val wholeTaka = absPaisa / 100L
        val decimals = absPaisa % 100L

        val groupedWhole = if (numberFormat == "INTL") {
            String.format(Locale.US, "%,d", wholeTaka)
        } else {
            formatSouthAsianGrouping(wholeTaka)
        }

        val decimalPart = if (showDecimals && decimals > 0L) {
            String.format(Locale.US, ".%02d", decimals)
        } else if (showDecimals) {
            ""
        } else {
            ""
        }

        val signPrefix = if (negative) "- " else ""
        val raw = "$signPrefix$currencySymbol $groupedWhole$decimalPart"
        return if (lang == "BN") toBanglaDigits(raw) else raw
    }

    private fun formatSouthAsianGrouping(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    fun currentEpochDay(): Long {
        return System.currentTimeMillis() / 86_400_000L
    }

    fun formatEpochDay(epochDay: Long, dateFormat: String = "DD_MM_YYYY", lang: String = "EN"): String {
        val millis = epochDay * 86_400_000L
        val pattern = when (dateFormat) {
            "YYYY_MM_DD" -> "yyyy-MM-dd"
            else -> "dd MMM yyyy"
        }
        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formatted = sdf.format(Date(millis))
        return if (lang == "BN") toBanglaDigits(formatted) else formatted
    }

    fun formatTimestamp(millis: Long, lang: String = "EN"): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val formatted = sdf.format(Date(millis))
        return if (lang == "BN") toBanglaDigits(formatted) else formatted
    }

    fun tr(lang: String, en: String, bn: String): String = if (lang == "BN") bn else en
}
