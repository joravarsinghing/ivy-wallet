package com.ivy.budgets

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BudgetAlertScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(insight: BudgetInsight, currency: String) {
        if (insight.overBudgetCount == 0 && insight.atRiskCount == 0) return
        val data = Data.Builder()
            .putInt(BudgetAlertWorker.KEY_EXCEEDED, insight.overBudgetCount)
            .putInt(BudgetAlertWorker.KEY_AT_RISK, insight.atRiskCount)
            .putString(BudgetAlertWorker.KEY_CURRENCY, currency)
            .build()
        val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
            .setInputData(data)
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
