package com.ivy.design.system

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.R

/** The only full-page Up/Back glyph. Keep the caller's existing navigation callback. */
@Composable
fun JvyBackButton(onClick: () -> Unit, modifier: Modifier = Modifier,
                  tint: Color = Color.Unspecified, enabled: Boolean = true) {
    val iconTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onBackground else tint
    IconButton(onClick, modifier.size(48.dp).testTag("subsection_back"), enabled = enabled) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.jvy_navigation_back),
            tint = if (enabled) iconTint else iconTint.copy(alpha = .38f))
    }
}

@Composable
fun JvyCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick, modifier.size(48.dp), enabled = enabled) {
        Icon(Icons.Outlined.Close, stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f))
    }
}

/** Scaffold top bars own the status inset; in-content headers do not add it again. */
@Composable
fun JvyTopAppBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, subtitle: String? = null,
                 actions: @Composable RowScope.() -> Unit = {}) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.statusBarsPadding()) { JvyScreenHeader(title, onBack, subtitle, actions = actions) }
    }
}

/** A subsection has one top-left Back, a bounded body and an in-flow optional action footer. */
@Composable
fun JvySubsectionScaffold(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null,
                          backEnabled: Boolean = true,
                          actions: @Composable RowScope.() -> Unit = {},
                          footer: @Composable () -> Unit = {},
                          content: @Composable BoxWithConstraintsScope.() -> Unit) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 960.dp).fillMaxSize()) {
                JvyScreenHeader(title, onBack, subtitle, backEnabled = backEnabled, actions = actions)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), content = content)
                footer()
            }
        }
    }
}
