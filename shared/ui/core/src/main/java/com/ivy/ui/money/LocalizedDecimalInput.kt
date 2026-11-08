package com.ivy.ui.money

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import com.ivy.ui.R
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Locale input -> canonical decimal. Reject ambiguity instead of silently changing magnitude. */
fun canonicalDecimalInput(input: String, locale: Locale, precision: Int = 2): String? {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val decimal = symbols.decimalSeparator
    val group = symbols.groupingSeparator
    val normalized = input.trim().map { char ->
        val digit = Character.digit(char, 10)
        when {
            digit >= 0 -> ('0'.code + digit).toChar()
            group.isWhitespace() || group == '\u00A0' || group == '\u202F' ->
                if (char == ' ' || char == '\u00A0' || char == '\u202F') group else char
            else -> char
        }
    }.joinToString("")
    if (normalized.count { it == decimal } > 1) return null
    val parts = normalized.split(decimal, limit = 2)
    val integer = parts[0]
    val groups = integer.split(group)
    if (groups.size > 1 && (groups.first().length !in 1..3 || groups.drop(1).any { it.length != 3 })) return null
    if (groups.any { part -> part.any { it !in '0'..'9' } }) return null
    val digits = groups.joinToString("")
    if (digits.length > 24) return null
    val fraction = parts.getOrNull(1)
    if (fraction != null && (fraction.length > precision || fraction.any { it !in '0'..'9' })) return null
    return digits + if (fraction != null) ".$fraction" else ""
}

class GroupedLocalizedDecimalInput(locale: Locale) : VisualTransformation {
    private val symbols = DecimalFormatSymbols.getInstance(locale)
    override fun filter(text: AnnotatedString): TransformedText {
        val parts = text.text.split(symbols.decimalSeparator, limit = 2)
        val integer = parts[0].reversed().chunked(3).joinToString(symbols.groupingSeparator.toString()).reversed()
        val display = integer + if (parts.size > 1) "${symbols.decimalSeparator}${parts[1]}" else ""
        return TransformedText(AnnotatedString(display), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var seen = 0
                display.forEachIndexed { index, char ->
                    if (char != symbols.groupingSeparator) { if (seen == offset) return index; seen++ }
                }
                return display.length
            }
            override fun transformedToOriginal(offset: Int): Int = display.take(offset.coerceIn(0, display.length))
                .count { it != symbols.groupingSeparator }.coerceAtMost(text.length)
        })
    }
}

/** Persisted values stay canonical; the editable draft and keyboard use the app's locale. */
@Composable
fun JvyDecimalField(label: String, value: String, change: (String) -> Unit,
                    modifier: Modifier = Modifier.fillMaxWidth(), precision: Int = 2) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val decimal = remember(locale) { DecimalFormatSymbols.getInstance(locale).decimalSeparator }
    val transformation = remember(locale) { GroupedLocalizedDecimalInput(locale) }
    var invalid by remember(locale, label) { mutableStateOf(false) }
    OutlinedTextField(value = value.replace('.', decimal), onValueChange = { input ->
        val canonical = canonicalDecimalInput(input, locale, precision)
        invalid = canonical == null
        if (canonical != null) change(canonical)
    }, label = { Text(label) }, textStyle = JvyTypography.AmountSmall, shape = JvyShapes.Control,
        visualTransformation = transformation, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = invalid, supportingText = if (invalid) ({ Text(stringResource(R.string.jvy_decimal_input_error, decimal.toString(), precision)) }) else null,
        singleLine = true, modifier = modifier)
}
