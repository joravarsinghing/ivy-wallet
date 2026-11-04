package com.ivy.design.system

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** UI-thread save acknowledgement. No payload or credentials are retained here. */
class EditorSubmission(private val terminalFailure: (Throwable) -> Boolean = { false },
    private val failureMessage: (Throwable) -> String = {
    "Could not save. Your changes are still here. Try again."
}) {
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var requiresReview by mutableStateOf(false)
        private set
    private var generation = 0L

    fun submit(commit: (complete: (Result<Unit>) -> Unit) -> Unit, onSuccess: () -> Unit) {
        if (saving || requiresReview) return
        saving = true
        error = null
        val request = ++generation
        val complete: (Result<Unit>) -> Unit = { result ->
            if (request == generation && saving) {
                saving = false
                if (result.isSuccess) onSuccess()
                else {
                    val failure = checkNotNull(result.exceptionOrNull())
                    requiresReview = terminalFailure(failure)
                    error = failureMessage(failure)
                }
            }
        }
        try { commit(complete) } catch (error: Exception) { complete(Result.failure(error)) }
    }
}
