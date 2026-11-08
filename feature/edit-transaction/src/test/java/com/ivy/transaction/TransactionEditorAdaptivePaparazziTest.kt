package com.ivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.navigation.EditTransactionScreen
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.data.CustomExchangeRateState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/** Calls the production form directly: no nested demo Preview or preview-only Surface. */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class TransactionEditorAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(error: Boolean = false, transfer: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
            val account = Account("Everyday household and spending account", 0xff18ba92.toInt(), currency = "AED", id = id)
            val destination = Account("Travel savings and future plans", 0xff6344fa.toInt(), currency = "USD")
            val category = Category(CategoryId(id), NotBlankTrimmedString.from("Household essentials").getOrNull()!!,
                ColorInt(0xffffcc3d.toInt()), null, 0.0)
            val type = if (transfer) TransactionType.TRANSFER else TransactionType.EXPENSE
            UI(screen = EditTransactionScreen(id, type), transactionType = type,
                baseCurrency = "AED", initialTitle = "Household shopping and essential supplies",
                titleSuggestions = persistentSetOf(), description = "A longer note kept with this payment.",
                category = category, suggestedCategory = null, dateTime = Instant.parse("2026-10-28T12:30:00Z"),
                account = account, toAccount = if (transfer) destination else null, dueDate = null,
                amount = 1234567.89, showRememberCategoryRule = false, rememberCategoryRule = false,
                customExchangeRateState = CustomExchangeRateState(showCard = transfer, toCurrencyCode = "USD",
                    fromCurrencyCode = "AED", exchangeRate = .2723, convertedAmount = 336197.83),
                categories = persistentListOf(category), accounts = persistentListOf(account, destination),
                tags = persistentListOf(), transactionAssociatedTags = persistentListOf(),
                onTitleChange = {}, onDescriptionChange = {}, onAmountChange = {}, onCategoryChange = {},
                onRememberCategoryRuleChange = {}, onAccountChange = {}, onToAccountChange = {},
                onDueDateChange = {}, onSetDate = {}, onSetTime = {}, onSetTransactionType = {},
                onCreateCategory = { _, _ -> }, onEditCategory = { _, _ -> }, onPayPlannedPayment = {}, onSave = {},
                onSetHasChanges = {}, onDelete = {}, onDuplicate = {}, onCreateAccount = { _, _ -> },
                saveError = if (error) "Could not save. Your changes are still here. Try again." else null,
            )
        }
    }
    @Test fun populatedExpense() = snapshot()
    @Test fun failedSaveKeepsDraft() = snapshot(error = true)
    @Test fun transferWithCurrencyConversion() = snapshot(transfer = true)
}
