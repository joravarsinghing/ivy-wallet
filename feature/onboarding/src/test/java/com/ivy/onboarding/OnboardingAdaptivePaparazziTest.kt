package com.ivy.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountBalance
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class OnboardingAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(step: OnboardingState, populated: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
            UI(onboardingState = step, currency = checkNotNull(IvyCurrency.fromCode("AED")),
                accountSuggestions = persistentListOf(CreateAccountData("Emergency savings and future plans", "AED",
                    Color(0xff18ba92), null, 0.0)),
                accounts = if (populated) persistentListOf(AccountBalance(
                    Account("Everyday household and spending account", 0xff18ba92.toInt(), currency = "AED", id = id),
                    1234567.89)) else persistentListOf(),
                categorySuggestions = persistentListOf(CreateCategoryData("Household and daily essentials", Color(0xffffcc3d), null)),
                categories = if (populated) persistentListOf(Category(CategoryId(id),
                    NotBlankTrimmedString.from("Household essentials and regular shopping").getOrNull()!!,
                    ColorInt(0xffffcc3d.toInt()), null, 0.0)) else persistentListOf(),
            )
        }
    }
    @Test fun welcome() = snapshot(OnboardingState.LOGIN)
    @Test fun chooseImportOrStartFresh() = snapshot(OnboardingState.CHOOSE_PATH)
    @Test fun chooseCurrency() = snapshot(OnboardingState.CURRENCY)
    @Test fun emptyAccounts() = snapshot(OnboardingState.ACCOUNTS)
    @Test fun populatedAccounts() = snapshot(OnboardingState.ACCOUNTS, true)
    @Test fun emptyCategories() = snapshot(OnboardingState.CATEGORIES)
    @Test fun populatedCategories() = snapshot(OnboardingState.CATEGORIES, true)
}
