package com.ivy.wallet.domain.deprecated.logic

import androidx.compose.ui.graphics.toArgb
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.CategoryRepository
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.domain.pure.util.nextOrderNum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryCreator @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val dispatchers: DispatchersProvider,
) {
    private val writes = Mutex()

    suspend fun createCategory(
        data: CreateCategoryData,
        onResult: (Result<Unit>) -> Unit = {},
        onRefreshUI: suspend (Category) -> Unit
    ) = persist(onResult, onRefreshUI) {
        val name = requireNotNull(NotBlankTrimmedString.from(data.name.trim()).getOrNull()) {
            "A category name is required."
        }
        val id = CategoryId(data.id)
        // Retrying an ambiguously acknowledged write must upsert the same record,
        // not create a duplicate or move its existing position in the category list.
        val existing = categoryRepository.findById(id)
        val category = Category(
            id = id, name = name, color = ColorInt(data.color.toArgb()),
            icon = data.icon?.let(IconAsset::from)?.getOrNull(),
            orderNum = existing?.orderNum ?: categoryRepository.findMaxOrderNum().nextOrderNum(),
        )
        categoryRepository.save(category)
        category
    }

    suspend fun editCategory(
        updatedCategory: Category,
        onResult: (Result<Unit>) -> Unit = {},
        onRefreshUI: suspend (Category) -> Unit
    ) = persist(onResult, onRefreshUI) {
        require(updatedCategory.name.value.isNotBlank()) { "A category name is required." }
        categoryRepository.save(updatedCategory)
        updatedCategory
    }

    private suspend fun persist(
        onResult: (Result<Unit>) -> Unit,
        refresh: suspend (Category) -> Unit,
        write: suspend () -> Category,
    ) {
        val saved = try {
            withContext(dispatchers.io) { writes.withLock { write() } }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            withContext(dispatchers.main) { onResult(Result.failure(failure)) }
            return
        }
        try {
            // Preserve the caller's refresh context (some list refreshes are computational).
            refresh(saved)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Storage already committed. A refresh failure is not permission
            // to retry with another identity or report the write as failed.
        }
        withContext(dispatchers.main) { onResult(Result.success(Unit)) }
    }
}
