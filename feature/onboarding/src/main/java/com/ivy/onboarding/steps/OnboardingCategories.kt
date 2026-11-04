package com.ivy.onboarding.steps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.onboarding.components.Suggestions
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.OrangeLight
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.RedLight
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.GradientCutBottom
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.OnboardingButton
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.wallet.ui.theme.toComposeColor
import java.util.UUID

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.OnboardingCategories(
    suggestions: List<CreateCategoryData>,
    categories: List<Category>,
    suggestionSubmission: com.ivy.design.system.EditorSubmission = remember { com.ivy.design.system.EditorSubmission() },
    editorModals: com.ivy.legacy.ui.EditorModalHost = remember { com.ivy.legacy.ui.EditorModalHost() },

    onCreateCategory: (CreateCategoryData, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onAddSuggestedCategory: (CreateCategoryData, (Result<Unit>) -> Unit) -> Unit = onCreateCategory,
    onEditCategory: (Category, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },

    onSkip: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    val categoryModalData = editorModals.category
    val pendingBackId = remember { UUID.randomUUID() }
    com.ivy.wallet.ui.theme.modal.AddModalBackHandling(pendingBackId, suggestionSubmission.saving) {}

    val nav = navigation()
    com.ivy.design.system.JvySubsectionScaffold(
        title = stringResource(R.string.add_categories),
        onBack = { nav.onBackPressed() },
        backEnabled = !suggestionSubmission.saving,
        actions = {
            if (categories.isEmpty()) com.ivy.design.system.JvyTextButton(onSkip, enabled = !suggestionSubmission.saving) {
                Text(stringResource(R.string.skip))
            }
        },
        footer = {
            if (suggestionSubmission.saving) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            suggestionSubmission.error?.let { error ->
                Text(error, Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics {
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                }, color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = com.ivy.design.system.JvyTypography.Supporting)
            }
            if (categories.isNotEmpty()) com.ivy.design.system.JvyPrimaryAction(
                stringResource(R.string.finish), onDoneClick, enabled = !suggestionSubmission.saving)
        },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column {
                    Spacer(Modifier.height(8.dp))

                    if (categories.isEmpty()) {
                        Spacer(Modifier.height(16.dp))

                        Image(
                            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()
                                .heightIn(max = 160.dp),
                            painter = painterResource(id = R.drawable.onboarding_illustration_categories),
                            contentDescription = "categories illustration"
                        )

                        OnboardingProgressSlider(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            selectedStep = 3,
                            stepsCount = 4,
                            selectedColor = IvyDark
                        )

                        Spacer(Modifier.height(48.dp))
                    } else {
                        Spacer(Modifier.height(24.dp))
                    }

                    Categories(
                        categories = categories,
                        enabled = !suggestionSubmission.saving,
                        onClick = {
                            editorModals.category = CategoryModalData(
                                category = it
                            )
                        }
                    )

                    if (categories.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.suggestions),
                        style = UI.typo.b1.style(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Suggestions(
                        enabled = !suggestionSubmission.saving,
                        suggestions = suggestions.filter { suggestion ->
                            categories.map { it.name.value.toLowerCaseLocal() }
                                .contains(suggestion.name.toLowerCaseLocal()).not()
                        },
                        onAddSuggestion = {
                            suggestionSubmission.submit({ complete ->
                                onAddSuggestedCategory(it as CreateCategoryData, complete)
                            }, onSuccess = {})
                        },
                        onAddNew = {
                            editorModals.category = CategoryModalData(
                                category = null
                            )
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    CategoryModal(
        modal = categoryModalData,
        onCreateCategory = onCreateCategory,
        onEditCategory = onEditCategory,
        dismiss = {
            editorModals.closeCategory(categoryModalData?.id)
        }
    )
}

@Composable
private fun Categories(
    categories: List<Category>,
    enabled: Boolean = true,
    onClick: (Category) -> Unit,
) {
    for (category in categories) {
        CategoryCard(
            category = category,
            enabled = enabled,
        ) {
            onClick(category)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CategoryCard(category: Category, enabled: Boolean = true, onClick: () -> Unit) {
    val categoryColor = category.color.value.toComposeColor()
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = com.ivy.design.system.JvyShapes.Card,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            ItemIconSDefaultIcon(
                modifier = Modifier.background(categoryColor, CircleShape),
                iconName = category.icon?.id, defaultIcon = R.drawable.ic_custom_category_s,
                tint = findContrastTextColor(categoryColor),
            )
            Text(category.name.value, Modifier.weight(1f),
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Empty() {
    IvyWalletPreview {
        OnboardingCategories(
            suggestions = listOf(
                CreateCategoryData(
                    name = "Food & Drinks",
                    color = Green,
                    icon = "fooddrink"
                ),

                CreateCategoryData(
                    name = "Bills & Fees",
                    color = Red,
                    icon = "bills"
                ),

                CreateCategoryData(
                    name = "Transport",
                    color = OrangeLight,
                    icon = "transport"
                ),

                CreateCategoryData(
                    name = "Groceries",
                    color = Color(0xFF75ff4d),
                    icon = "groceries"
                ),

                CreateCategoryData(
                    name = "Entertainment",
                    color = Orange,
                    icon = "game"
                ),

                CreateCategoryData(
                    name = "Shopping",
                    color = Ivy,
                    icon = "shopping"
                ),

                CreateCategoryData(
                    name = "Gifts",
                    color = RedLight,
                    icon = "gift"
                ),

                CreateCategoryData(
                    name = "Health",
                    color = Color(0xFF4dfff3),
                    icon = "health"
                ),

                CreateCategoryData(
                    name = "Investments",
                    color = Color(0xFF1e5166),
                    icon = "leaf"
                ),
            ),
            categories = listOf()
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Categories() {
    IvyWalletPreview {
        OnboardingCategories(
            suggestions = listOf(
                CreateCategoryData(
                    name = "Food & Drinks",
                    color = Green,
                    icon = "fooddrink"
                ),

                CreateCategoryData(
                    name = "Bills & Fees",
                    color = Red,
                    icon = "bills"
                ),

                CreateCategoryData(
                    name = "Transport",
                    color = OrangeLight,
                    icon = "transport"
                ),

                CreateCategoryData(
                    name = "Groceries",
                    color = Color(0xFF75ff4d),
                    icon = "groceries"
                ),

                CreateCategoryData(
                    name = "Entertainment",
                    color = Orange,
                    icon = "game"
                ),

                CreateCategoryData(
                    name = "Shopping",
                    color = Ivy,
                    icon = "shopping"
                ),

                CreateCategoryData(
                    name = "Gifts",
                    color = RedLight,
                    icon = "gift"
                ),

                CreateCategoryData(
                    name = "Health",
                    color = Color(0xFF4dfff3),
                    icon = "health"
                ),

                CreateCategoryData(
                    name = "Investments",
                    color = Color(0xFF1e5166),
                    icon = "leaf"
                ),
            ),
            categories = listOf(
                Category(
                    name = NotBlankTrimmedString.unsafe("Food & Drinks"),
                    color = ColorInt(Orange.toArgb()),
                    icon = IconAsset.unsafe("fooddrinks"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                )
            )
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Premium() {
    IvyWalletPreview {
        OnboardingCategories(
            suggestions = listOf(
                CreateCategoryData(
                    name = "Food & Drinks",
                    color = Green,
                    icon = "fooddrink"
                ),

                CreateCategoryData(
                    name = "Bills & Fees",
                    color = Red,
                    icon = "bills"
                ),

                CreateCategoryData(
                    name = "Transport",
                    color = OrangeLight,
                    icon = "transport"
                ),

                CreateCategoryData(
                    name = "Groceries",
                    color = Color(0xFF75ff4d),
                    icon = "groceries"
                ),

                CreateCategoryData(
                    name = "Entertainment",
                    color = Orange,
                    icon = "game"
                ),

                CreateCategoryData(
                    name = "Shopping",
                    color = Ivy,
                    icon = "shopping"
                ),

                CreateCategoryData(
                    name = "Gifts",
                    color = RedLight,
                    icon = "gift"
                ),

                CreateCategoryData(
                    name = "Health",
                    color = Color(0xFF4dfff3),
                    icon = "health"
                ),

                CreateCategoryData(
                    name = "Investments",
                    color = Color(0xFF1e5166),
                    icon = "leaf"
                ),
            ),
            categories = List(12) {
                Category(
                    name = NotBlankTrimmedString.unsafe("Food & Drinks"),
                    color = ColorInt(Orange.toArgb()),
                    icon = IconAsset.unsafe("fooddrinks"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                )
            }
        )
    }
}
