package com.ivy.importdata.csvimport

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.data.backup.AiImportContract
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import org.junit.Assert.assertTrue

@RunWith(TestParameterInjector::class)
class AiImportPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    @Test fun guide() = render { AiImportScreenContent(null, null, false, {}, {}, {}) }
    @Test fun onboardingGuide() = render { AiImportScreenContent(null, null, true, {}, {}, {}) }
    @Test fun validationError() = render { AiImportScreenContent(null, "A category reference is missing from this file.", false, {}, {}, {}) }
    @Test fun review() = render {
        val payload = AiImportContract.guide.substringAfter("MINIMAL FICTIONAL EXAMPLE (do not reuse its IDs for real data):").substringBefore("JSON SCHEMA:").trim()
        AiImportScreenContent(AiImportContract.prepare(payload), null, true, {}, {}, {})
    }
    @Test fun backControlRemainsVisible() {
        var bounds = Rect.Zero
        render { AiImportScreenContent(null, null, false, {}, {}, {}, backModifier = Modifier.onGloballyPositioned { bounds = it.boundsInRoot() }) }
        assertTrue("Back must retain a 48dp target: $bounds", bounds.width >= 48f && bounds.height >= 48f)
        assertTrue("Back must be inside the viewport: $bounds", bounds.top >= 0 && bounds.bottom <= window.height && bounds.left >= 0 && bounds.right <= window.width)
    }
    @Test fun reviewBackControlRemainsVisible() {
        var bounds = Rect.Zero
        val payload = AiImportContract.guide.substringAfter("MINIMAL FICTIONAL EXAMPLE (do not reuse its IDs for real data):").substringBefore("JSON SCHEMA:").trim()
        val preview = AiImportContract.prepare(payload)
        render { AiImportScreenContent(preview, null, true, {}, {}, {}, backModifier = Modifier.onGloballyPositioned { bounds = it.boundsInRoot() }) }
        assertTrue("Review Back must retain a 48dp target: $bounds", bounds.width >= 48f && bounds.height >= 48f)
        assertTrue("Review Back must be inside the viewport: $bounds", bounds.top >= 0 && bounds.bottom <= window.height && bounds.left >= 0 && bounds.right <= window.width)
    }
}
