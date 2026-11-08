package com.ivy.categories

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.design.system.JvyButton
import com.ivy.design.system.JvyFormDialogFrame
import com.ivy.design.system.JvyShapes
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.modal.IvyModal
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(TestParameterInjector::class)
class LegacyDialogNavigationPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    @Test fun compactTitledModalHasOneAlignedContentColumn() = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            IvyModal(id = UUID.fromString("00000000-0000-0000-0000-000000000002"), visible = true,
                dismiss = {}, title = "Edit tag", PrimaryAction = {
                    JvyButton({}, Modifier.fillMaxWidth()) { Text("Done") }
                }) {
                OutlinedTextField("Fictional travel", {}, Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    label = { Text("Tag name") }, shape = JvyShapes.Control)
            }
        }
    }
    @Test fun modernFormUsesTheSameHeaderRhythm() = render {
        JvyFormDialogFrame("Edit a fictional goal with a long name", {}, footer = {
            JvyButton({}, Modifier.fillMaxWidth()) { Text("Save") }
        }) {
            OutlinedTextField("Fictional travel", {}, Modifier.fillMaxWidth(),
                label = { Text("Goal name") }, shape = JvyShapes.Control)
            repeat(12) { Text("Related fields remain in the scrolling content column.") }
        }
    }
    @Test fun closeStaysAtTheTopWhileLongContentScrolls() = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            IvyModal(id = UUID.fromString("00000000-0000-0000-0000-000000000001"), visible = true,
                dismiss = {}, title = "Edit a fictional category with a long name",
                PrimaryAction = { JvyButton({}) { Text("Save") } }) {
                repeat(20) { Text("A field or explanation that can scroll without hiding Close.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
            }
        }
    }
}
