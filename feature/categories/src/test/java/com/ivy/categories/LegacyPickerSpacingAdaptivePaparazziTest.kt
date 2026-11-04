package com.ivy.categories

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.model.IntervalType
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.modal.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

/** Actual legacy selectors catch header/footer compensation gaps, not just a proxy. */
@RunWith(TestParameterInjector::class)
class LegacyPickerSpacingAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private fun snapshot(content: @Composable BoxWithConstraintsScope.() -> Unit) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT,
            content = content)
    }
    @Test fun currencySearchAndList() = snapshot {
        CurrencyModal("Choose currency", IvyCurrency.fromCode("AED"), true, {}, id) {}
    }
    @Test fun monthChoices() = snapshot {
        MonthPickerModal(LocalDate.of(2026, 10, 31), true, {}, {}, id)
    }
    @Test fun iconChoices() = snapshot {
        ChooseIconModal(true, "account", Color(0xff18ba92), {}, id, {})
    }
    @Test fun recurringChoices() = snapshot {
        RecurringRuleModal(RecurringRuleModalData(LocalDate.of(2026, 10, 31).atStartOfDay(), 1,
            IntervalType.MONTH, id = id), {}, { _, _, _, _ -> })
    }
    @Test fun oneTimeChoices() = snapshot {
        RecurringRuleModal(RecurringRuleModalData(LocalDate.of(2026, 10, 31).atStartOfDay(), 1,
            IntervalType.MONTH, initialOneTime = true, id = id), {}, { _, _, _, _ -> })
    }
    @Test fun invalidRecurringCountOffersCorrection() = snapshot {
        RecurringRuleModal(RecurringRuleModalData(LocalDate.of(2026, 10, 31).atStartOfDay(), 0,
            IntervalType.MONTH, id = id), {}, { _, _, _, _ -> })
    }
}
