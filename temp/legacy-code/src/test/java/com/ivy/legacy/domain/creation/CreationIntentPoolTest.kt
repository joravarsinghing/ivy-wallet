package com.ivy.legacy.domain.creation

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CreationIntentPoolTest {
    private val source = UUID.randomUUID()
    private val pool = CreationIntentPool<Any>()
    @Test fun `failed creation retries the same intent`() {
        val request = pool.request(source) { Any() }
        pool.complete(source, request, Result.failure(IllegalStateException()))
        assertSame(request, pool.request(source) { Any() })
    }
    @Test fun `acknowledged creation followed by the same suggestion starts fresh`() {
        val request = pool.request(source) { Any() }
        pool.complete(source, request, Result.success(Unit))
        assertNotSame(request, pool.request(source) { Any() })
    }
    @Test fun `different suggestions have independent intents`() {
        val request = pool.request(source) { Any() }
        assertNotSame(request, pool.request(UUID.randomUUID()) { Any() })
        assertSame(request, pool.request(source) { Any() })
    }
    @Test fun `late completion cannot clear a newer intent`() {
        val old = pool.request(source) { Any() }
        pool.complete(source, old, Result.success(Unit))
        val next = pool.request(source) { Any() }
        pool.complete(source, old, Result.success(Unit))
        assertSame(next, pool.request(source) { Any() })
    }
}
