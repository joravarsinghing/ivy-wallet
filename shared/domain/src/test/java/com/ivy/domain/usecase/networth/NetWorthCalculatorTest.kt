package com.ivy.domain.usecase.networth

import com.ivy.data.model.AccountId
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.util.UUID
import org.junit.Test

class NetWorthCalculatorTest {
    @Test
    fun `readiness requires explicit classification`() {
        val asset = AccountId(UUID.randomUUID())
        val liability = AccountId(UUID.randomUUID())

        val readiness = NetWorthCalculator.readiness(
            includedAccountIds = setOf(asset, liability),
            classifications = listOf(
                NetWorthAccountClassification(asset, NetWorthAccountRole.ASSET)
            )
        )

        readiness.isReady shouldBe false
        readiness.unclassifiedAccountIds shouldBe setOf(liability)
    }

    @Test
    fun `snapshot keeps liabilities separate and derives net worth`() {
        val asset = AccountId(UUID.randomUUID())
        val liability = AccountId(UUID.randomUUID())

        val snapshot = NetWorthCalculator.calculate(
            balances = mapOf(
                asset to BigDecimal("1200.00"),
                liability to BigDecimal("-300.00"),
            ),
            classifications = listOf(
                NetWorthAccountClassification(asset, NetWorthAccountRole.ASSET),
                NetWorthAccountClassification(liability, NetWorthAccountRole.LIABILITY),
            )
        )

        snapshot.assets shouldBe BigDecimal("1200.00")
        snapshot.liabilities shouldBe BigDecimal("300.00")
        snapshot.netWorth shouldBe BigDecimal("900.00")
    }
}
