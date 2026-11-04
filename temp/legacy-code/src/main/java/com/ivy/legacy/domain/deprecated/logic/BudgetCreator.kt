package com.ivy.legacy.domain.deprecated.logic

import com.ivy.data.db.dao.read.BudgetDao
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.legacy.datamodel.Budget
import com.ivy.legacy.utils.ioThread
import com.ivy.wallet.domain.deprecated.logic.model.CreateBudgetData
import com.ivy.wallet.domain.pure.util.nextOrderNum
import javax.inject.Inject

class BudgetCreator @Inject constructor(
    private val budgetDao: BudgetDao,
    private val budgetWriter: WriteBudgetDao,
) {
    suspend fun createBudget(
        data: CreateBudgetData,
        onRefreshUI: suspend (Budget) -> Unit
    ) {
        val name = data.name
        require(name.isNotBlank()) { "Enter a budget name." }
        require(data.amount.isFinite() && data.amount > 0) { "Enter a positive budget amount." }

        try {
            val newBudget = ioThread {
                val budget = Budget(
                    name = name.trim(),
                    amount = data.amount,
                    categoryIdsSerialized = data.categoryIdsSerialized,
                    accountIdsSerialized = data.accountIdsSerialized,
                    orderId = budgetDao.findMaxOrderNum().nextOrderNum(),
                    isSynced = false
                )

                budgetWriter.save(budget.toEntity())
                budget
            }

            onRefreshUI(newBudget)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun editBudget(
        updatedBudget: Budget,
        onRefreshUI: suspend (Budget) -> Unit
    ) {
        require(updatedBudget.name.isNotBlank()) { "Enter a budget name." }
        require(updatedBudget.amount.isFinite() && updatedBudget.amount > 0.0) { "Enter a positive budget amount." }

        try {
            ioThread {
                budgetWriter.save(
                    updatedBudget.toEntity().copy(
                        isSynced = false
                    )
                )
            }

            onRefreshUI(updatedBudget)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteBudget(
        budget: Budget,
        onRefreshUI: suspend () -> Unit
    ) {
        try {
            ioThread {
                budgetWriter.deleteById(budget.id)
            }

            onRefreshUI()
        } catch (e: Exception) {
            throw e
        }
    }
}
