package com.ivy.ui.money

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/** Shared exact formatter; display rounding never changes the underlying value. */
fun formatDecimalMoney(value: BigDecimal, currency: String): String =
    DecimalFormat("#,##0.00").format(value.setScale(2, RoundingMode.HALF_UP)) + " " + currency

object GroupedDecimalInput : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        fun grouped(value: String): String {
            val parts = value.split('.', limit = 2)
            val integer = parts[0].reversed().chunked(3).joinToString(",").reversed()
            return if (parts.size > 1) "$integer.${parts[1]}" else integer
        }
        val display = grouped(raw)
        return TransformedText(AnnotatedString(display), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var seen = 0
                display.forEachIndexed { index, char ->
                    if (char != ',') { if (seen == offset) return index; seen++ }
                }
                return display.length
            }
            override fun transformedToOriginal(offset: Int): Int = display.take(offset.coerceIn(0, display.length)).count { it != ',' }.coerceAtMost(raw.length)
        })
    }
}
