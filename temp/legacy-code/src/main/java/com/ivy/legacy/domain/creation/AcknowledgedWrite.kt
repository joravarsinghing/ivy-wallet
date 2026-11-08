package com.ivy.legacy.domain.creation

import com.ivy.base.threading.DispatchersProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/** Storage determines success. An optional refresh cannot undo a committed write. */
internal suspend fun <T> acknowledgedWrite(
    dispatchers: DispatchersProvider,
    onResult: (Result<Unit>) -> Unit,
    write: suspend () -> T,
    refresh: suspend (T) -> Unit,
): T? {
    val saved = try {
        withContext(dispatchers.io) { write() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        withContext(dispatchers.main) { onResult(Result.failure(failure)) }
        return null
    }
    try {
        refresh(saved)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) { /* Committed; refreshing is not another save. */ }
    withContext(dispatchers.main) { onResult(Result.success(Unit)) }
    return saved
}
