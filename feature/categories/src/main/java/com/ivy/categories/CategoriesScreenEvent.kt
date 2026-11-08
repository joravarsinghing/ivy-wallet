package com.ivy.categories

import com.ivy.data.model.Category
import java.util.UUID
import com.ivy.wallet.domain.data.SortOrder
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData

sealed interface CategoriesScreenEvent {
    data class OnReorder(
        val newOrder: List<CategoryData>,
        val sortOrder: SortOrder = SortOrder.DEFAULT
    ) : CategoriesScreenEvent

    data class OnCreateCategory(val createCategoryData: CreateCategoryData, val onResult: (Result<Unit>) -> Unit = {}) :
        CategoriesScreenEvent
    data class OnEditCategory(val category: Category, val onResult: (Result<Unit>) -> Unit = {}) : CategoriesScreenEvent

    data class OnReorderModalVisible(val visible: Boolean) : CategoriesScreenEvent
    data class OnSortOrderModalVisible(val visible: Boolean) : CategoriesScreenEvent
    data class OnCategoryModalVisible(val categoryModalData: CategoryModalData?, val expectedId: UUID? = null) :
        CategoriesScreenEvent
    data class OnSearchQueryUpdate(val queryString: String) : CategoriesScreenEvent
}
