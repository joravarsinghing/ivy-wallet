package com.ivy.wallet.domain.deprecated.logic.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class CreateCategoryData(
    val name: String,
    val color: Color,
    val icon: String?,
    val id: UUID = UUID.randomUUID(),
)
