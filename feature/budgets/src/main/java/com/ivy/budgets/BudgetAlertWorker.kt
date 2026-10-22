package com.ivy.budgets

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivy.base.legacy.SharedPrefs
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notificationService: NotificationService,
    private val sharedPrefs: SharedPrefs,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true)) return Result.success()

        val exceeded = inputData.getInt(KEY_EXCEEDED, 0)
        val atRisk = inputData.getInt(KEY_AT_RISK, 0)
        if (exceeded == 0 && atRisk == 0) return Result.success()

        val currency = inputData.getString(KEY_CURRENCY).orEmpty()
        val signature = "$exceeded:$atRisk:$currency"
        if (sharedPrefs.getString(SharedPrefs.LAST_BUDGET_ALERT_SIGNATURE, null) == signature) {
            return Result.success()
        }
        sharedPrefs.putString(SharedPrefs.LAST_BUDGET_ALERT_SIGNATURE, signature)

        val message = when {
            exceeded > 0 && atRisk > 0 -> "$exceeded budget(s) exceeded; $atRisk more are at risk."
            exceeded > 0 -> "$exceeded budget(s) exceeded."
            else -> "$atRisk budget(s) are at risk."
        }
        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.BUDGET_ALERT,
                priority = NotificationCompat.PRIORITY_DEFAULT,
            )
            .setContentTitle("Jvy Wallet budget alert")
            .setContentText(message)
        notificationService.showNotification(notification, NOTIFICATION_ID)
        return Result.success()
    }

    companion object {
        const val KEY_EXCEEDED = "exceeded"
        const val KEY_AT_RISK = "at_risk"
        const val KEY_CURRENCY = "currency"
        private const val NOTIFICATION_ID = 2301
    }
}
