package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.DeleteOperation
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import javax.inject.Inject

class RepositoryMemoFactory @Inject constructor(
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
) {
    fun <T : Identifiable<TID>, TID : UniqueId> createMemo(
        getDataWriteSaveEvent: (List<T>) -> DataWriteEvent,
        getDateWriteDeleteEvent: (DeleteOperation<TID>) -> DataWriteEvent
    ): RepositoryMemo<T, TID> = RepositoryMemo(
        dataObserver = dataObserver,
        dispatchers = dispatchers,
        getDataWriteSaveEvent = getDataWriteSaveEvent,
        getDataWriteDeleteEvent = getDateWriteDeleteEvent,
    )
}

class RepositoryMemo<T : Identifiable<TID>, TID : UniqueId> internal constructor(
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
    private val getDataWriteSaveEvent: (List<T>) -> DataWriteEvent,
    private val getDataWriteDeleteEvent: (DeleteOperation<TID>) -> DataWriteEvent,
) {

    private val _memo = mutableMapOf<TID, T>()
    private val cacheLock = Any()
    private var generation = 0L
    val items: Map<TID, T> get() = synchronized(cacheLock) { _memo.toMap() }
    @Volatile
    var findAllMemoized = false
        private set

    /** Discard derived reads after a transaction, including ambiguous cancellation. */
    internal fun invalidate() {
        synchronized(cacheLock) {
            generation++
            _memo.clear()
            findAllMemoized = false
        }
    }

    suspend fun findAll(
        findAllOperation: suspend () -> List<T>,
        sortMemo: Collection<T>.() -> List<T>,
    ): List<T> {
        if (currentCoroutineContext()[AtomicWriteSession] != null) return withContext(dispatchers.io) { findAllOperation().let(sortMemo) }
        val (cached, request) = synchronized(cacheLock) {
            (if (findAllMemoized) _memo.values.toList() else null) to generation
        }
        return cached?.let(sortMemo) ?: withContext(dispatchers.io) {
            findAllOperation().also {
                synchronized(cacheLock) {
                    if (generation != request) return@synchronized
                    memoize(it)
                    findAllMemoized = true
                }
            }
        }
    }

    suspend fun findById(
        id: TID,
        findByIdOperation: suspend (TID) -> T?
    ): T? {
        if (currentCoroutineContext()[AtomicWriteSession] != null) return withContext(dispatchers.io) { findByIdOperation(id) }
        val (cached, request) = synchronized(cacheLock) { _memo[id] to generation }
        return cached ?: withContext(dispatchers.io) {
            findByIdOperation(id)?.also {
                synchronized(cacheLock) {
                    if (generation == request) memoize(it)
                }
            }
        }
    }

    suspend fun findByIds(
        ids: List<TID>,
        findByIdOperation: suspend (TID) -> T?
    ): List<T> = ids.mapNotNull { id -> findById(id, findByIdOperation) }

    suspend fun save(
        value: T,
        writeOperation: suspend (T) -> Unit,
    ) {
        withContext(dispatchers.io) {
            val session = currentCoroutineContext()[AtomicWriteSession]
            session?.touch(this@RepositoryMemo, ::invalidate)
            writeOperation(value)
            if (session != null) {
                session.afterCommit { dataObserver.post(getDataWriteSaveEvent(listOf(value))) }
                return@withContext
            }
            synchronized(cacheLock) { generation++; memoize(value) }
            dataObserver.post(getDataWriteSaveEvent(listOf(value)))
        }
    }

    suspend fun saveMany(
        values: List<T>,
        writeOperation: suspend (List<T>) -> Unit,
    ) {
        withContext(dispatchers.io) {
            val session = currentCoroutineContext()[AtomicWriteSession]
            session?.touch(this@RepositoryMemo, ::invalidate)
            writeOperation(values)
            if (session != null) {
                session.afterCommit { dataObserver.post(getDataWriteSaveEvent(values)) }
                return@withContext
            }
            synchronized(cacheLock) { generation++; memoize(values) }
            dataObserver.post(getDataWriteSaveEvent(values))
        }
    }

    suspend fun deleteById(
        id: TID,
        deleteByIdOperation: suspend (TID) -> Unit,
    ) {
        withContext(dispatchers.io) {
            val session = currentCoroutineContext()[AtomicWriteSession]
            session?.touch(this@RepositoryMemo, ::invalidate)
            deleteByIdOperation(id)
            if (session != null) {
                session.afterCommit { dataObserver.post(getDataWriteDeleteEvent(DeleteOperation.Just(listOf(id)))) }
                return@withContext
            }
            synchronized(cacheLock) { generation++; _memo.remove(id) }
            dataObserver.post(
                getDataWriteDeleteEvent(DeleteOperation.Just(listOf(id)))
            )
        }
    }

    suspend fun deleteAll(
        deleteAllOperation: suspend () -> Unit,
    ) {
        withContext(dispatchers.io) {
            val session = currentCoroutineContext()[AtomicWriteSession]
            session?.touch(this@RepositoryMemo, ::invalidate)
            deleteAllOperation()
            if (session != null) {
                session.afterCommit { dataObserver.post(getDataWriteDeleteEvent(DeleteOperation.All)) }
                return@withContext
            }
            synchronized(cacheLock) { generation++; _memo.clear() }
            dataObserver.post(getDataWriteDeleteEvent(DeleteOperation.All))
        }
    }

    private fun memoize(items: List<T>) {
        items.forEach(::memoize)
    }

    private fun memoize(item: T) {
        _memo[item.id] = item
    }
}
