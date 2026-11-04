package com.ivy.disclaimer.composables

import androidx.compose.runtime.Composable
import com.ivy.design.system.JvyTopAppBar

@Composable
fun DisclaimerTopAppBar(
    onBack: (() -> Unit)? = null,
) {
    JvyTopAppBar("Important User Agreement", onBack)
}
