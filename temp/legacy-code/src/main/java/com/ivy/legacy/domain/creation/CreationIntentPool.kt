package com.ivy.legacy.domain.creation

import java.util.UUID

/** VM-owned live intents: retry failures, but start fresh after acknowledged creation. */
class CreationIntentPool<T : Any> {
    private val pending = mutableMapOf<UUID, T>()

    @Synchronized fun request(source: UUID, fresh: () -> T): T = pending.getOrPut(source, fresh)

    @Synchronized fun complete(source: UUID, request: T, result: Result<Unit>) {
        if (result.isSuccess && pending[source] === request) pending.remove(source)
    }
}
