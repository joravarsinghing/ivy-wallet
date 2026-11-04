package com.ivy.data.repository

import androidx.room.withTransaction
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.IvyRoomDatabase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/** Defers derived cache/events until Room has acknowledged the entire operation. */
internal class AtomicWriteSession : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AtomicWriteSession>
    private val invalidators = linkedMapOf<Any, () -> Unit>()
    private val notifications = mutableListOf<suspend () -> Unit>()
    @Synchronized fun touch(owner: Any, invalidate: () -> Unit) { invalidators[owner] = invalidate }
    @Synchronized fun afterCommit(notify: suspend () -> Unit) { notifications += notify }
    fun invalidate() = synchronized(this) { invalidators.values.toList() }.forEach { it() }
    suspend fun publish() { synchronized(this) { notifications.toList() }.forEach { it() } }
}

@Singleton
class AtomicWriteRepository internal constructor(
    private val transaction: suspend (suspend () -> Unit) -> Unit,
    private val dispatchers: DispatchersProvider,
) {
    @Inject constructor(db: IvyRoomDatabase, dispatchers: DispatchersProvider) :
        this({ block -> db.withTransaction { block() } }, dispatchers)

    private val writes = Mutex()

    suspend fun <T> run(block: suspend () -> T): T {
        if (currentCoroutineContext()[AtomicWriteSession] != null) return block()
        return withContext(dispatchers.io) {
            writes.withLock {
                val session = AtomicWriteSession()
                var result: Result<T>? = null
                try {
                    transaction { withContext(session) { result = Result.success(block()) } }
                } finally {
                    // Also discard reads if commit and cancellation acknowledgement race.
                    session.invalidate()
                }
                session.publish()
                checkNotNull(result).getOrThrow()
            }
        }
    }
}
