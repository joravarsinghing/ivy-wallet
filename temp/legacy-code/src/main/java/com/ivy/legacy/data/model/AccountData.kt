package com.ivy.legacy.data.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.wallet.domain.data.Reorderable

@Immutable
data class AccountData(
    val account: Account,
    val balance: Double,
    val balanceBaseCurrency: Double?,
    val monthlyExpenses: Double,
    val monthlyIncome: Double,
    val reservedForGoals: Double = 0.0,
) : Reorderable {
    override fun getItemOrderNum() = account.orderNum

    override fun withNewOrderNum(newOrderNum: Double) = this.copy(
        account = account.copy(
            orderNum = newOrderNum
        )
    )
}
