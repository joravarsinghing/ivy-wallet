package com.ivy.design.system.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.base.analytics.AnalyticsCard
import com.ivy.design.system.JvyShapes

/** One useful visual per context. Different native currencies are selectable, never summed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JvyContextCharts(cards: List<AnalyticsCard>, modifier: Modifier = Modifier, horizontalPadding: Dp = 24.dp, hidden: Boolean = false) {
    if (cards.isEmpty()) return
    var selected by rememberSaveable(cards.first().id.substringBeforeLast('-')) { mutableStateOf(cards.first().unit) }
    val card = cards.firstOrNull { it.unit == selected } ?: cards.first()
    Column(modifier) {
        if (cards.size > 1) FlowRow(Modifier.padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.forEach { choice -> FilterChip(selected = choice.id == card.id, onClick = { selected = choice.unit },
                label = { Text(choice.unit) }, shape = JvyShapes.Control, modifier = Modifier.heightIn(min = 48.dp)) }
        }
        JvyAnalyticsCard(card, horizontalPadding = horizontalPadding, hidden = hidden, compact = true)
    }
}
