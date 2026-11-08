package com.ivy.home

import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class HomeTrendGeometryTest {
    private fun point(day: Int, balance: String = "10000", income: String = "100", spent: String = "40",
                      observed: Boolean = true) = HomeTrendPoint("Oct $day", BigDecimal(income), BigDecimal(spent),
        BigDecimal(balance), observed, HomeTrendPeriodIdentity.day(LocalDate.of(2026, 10, day)))

    private fun scene(vararg points: HomeTrendPoint, metrics: Set<HomeTrendMetric> = HomeTrendMetric.entries.toSet()) =
        prepareTrendScene(HomeTrendState(points = points.toList().toImmutableList(), visibleMetrics = metrics))

    @Test fun `all measures share a scale rather than normalizing flows independently`() {
        val small = scene(point(1, balance = "100"), point(2, balance = "110"))
        val large = scene(point(1, balance = "1000000000"), point(2, balance = "1100000000"))
        assertNotEquals(small.plot.scale, large.plot.scale)
        val smallIncome = small.plot.series.first { it.metric == HomeTrendMetric.INCOME }.segments.single().first()
        val largeIncome = large.plot.series.first { it.metric == HomeTrendMetric.INCOME }.segments.single().first()
        assertTrue(largeIncome.y > smallIncome.y)
    }

    @Test fun `income and spending both rise from the same baseline without stacking`() {
        val band = scene(point(1, balance = "200"), point(2, balance = "200")).plot
        val income = band.series.first { it.metric == HomeTrendMetric.INCOME }.segments.single().first()
        val spent = band.series.first { it.metric == HomeTrendMetric.EXPENSES }.segments.single().first()
        assertEquals(1f, band.scale.y(BigDecimal.ZERO), 0f)
        assertTrue(income.y < 1f)
        assertTrue(spent.y < 1f)
        assertEquals(1f - 100f / 220, income.y, 0.00001f)
        assertEquals(1f - 40f / 220, spent.y, 0.00001f)
    }

    @Test fun `refunds and reversals retain their signs and do not mutate money`() {
        val point = point(1, income = "-100", spent = "-40")
        val flow = scene(point).plot
        val zero = flow.scale.y(BigDecimal.ZERO)
        assertTrue(flow.series[0].segments.single().single().y > zero)
        assertTrue(flow.series[1].segments.single().single().y > zero)
        assertEquals(BigDecimal("-40"), point.expenses)
        assertEquals(BigDecimal("-100"), point.income)
    }

    @Test fun `unknown periods split every series and do not expand scales`() {
        val scene = scene(point(1), point(2, balance = "99999999", income = "99999999", observed = false), point(3), point(4))
        scene.plot.series.forEach { series ->
            assertEquals(listOf(1, 2), series.segments.map { it.size })
        }
        assertTrue(scene.plot.scale.maximum < BigDecimal("20000"))
    }

    @Test fun `real zero flow is retained as baseline not missing data`() {
        val band = scene(point(1), point(2, income = "0", spent = "0"), point(3)).plot
        band.series.filter { it.metric != HomeTrendMetric.BALANCE }.forEach { series ->
            assertEquals(3, series.segments.single().size)
            assertEquals(band.scale.y(BigDecimal.ZERO), series.segments.single()[1].y, 0f)
        }
    }

    @Test fun `zero balances have a safe nonnegative scale`() {
        val band = scene(point(1, balance = "0", income = "0", spent = "0"), metrics = setOf(HomeTrendMetric.BALANCE)).plot
        assertEquals(BigDecimal.ZERO, band.scale.minimum)
        assertTrue(band.scale.maximum > band.scale.minimum)
        assertEquals(1f, band.series.single().segments.single().single().y, 0f)
    }

    @Test fun `negative balances keep zero visible and negative values below it`() {
        val band = scene(point(1, balance = "-200"), point(2, balance = "-100"), metrics = setOf(HomeTrendMetric.BALANCE)).plot
        assertEquals(BigDecimal.ZERO, band.scale.maximum)
        assertTrue(band.scale.minimum < BigDecimal("-200"))
        assertEquals(0f, band.scale.y(BigDecimal.ZERO), 0f)
        assertTrue(band.series.single().segments.single().all { it.y > 0f })
    }

    @Test fun `very large money projects to finite fractions before float conversion`() {
        val huge = "9".repeat(200)
        val scene = scene(point(1, balance = huge, income = huge, spent = huge), point(2, balance = "-$huge"))
        scene.plot.series.forEach { series ->
            assertTrue(series.segments.flatten().all { it.x.isFinite() && it.y.isFinite() && it.y in 0f..1f })
        }
    }

    @Test fun `calendar dates share their actual horizontal domain`() {
        val coords = scene(point(1), point(2), point(11)).plot.series.first().segments.single()
        assertEquals(listOf(0f, 0.1f, 1f), coords.map { it.x })
    }

    @Test fun `monthly positions use month starts not month ends`() {
        val points = (1..3).map { month -> point(1).copy(periodIdentity = HomeTrendPeriodIdentity.month(YearMonth.of(2026, month))) }
        val coords = scene(*points.toTypedArray()).plot.series.first().segments.single()
        assertEquals(0f, coords.first().x, 0f)
        assertEquals(31f / 59f, coords[1].x, 0.00001f)
        assertEquals(1f, coords.last().x, 0f)
    }

    @Test fun `a single observation does not become a synthetic line`() {
        val scene = scene(point(1, observed = false), point(2))
        assertTrue(scene.plot.series.all { it.segments.all { segment -> segment.size == 1 } })
        assertEquals(1f, scene.plot.series.first().segments.single().single().x, 0f)
    }

    @Test fun `hiding measures removes their geometry and scale dependencies`() {
        assertEquals(listOf(HomeTrendMetric.BALANCE), scene(point(1), metrics = setOf(HomeTrendMetric.BALANCE)).plot.series.map { it.metric })
        val incomeOnly = scene(point(1), metrics = setOf(HomeTrendMetric.INCOME))
        assertEquals(listOf(HomeTrendMetric.INCOME), incomeOnly.plot.series.map { it.metric })
        assertTrue(incomeOnly.plot.scale.maximum < BigDecimal("200"))
        assertEquals(listOf(HomeTrendMetric.EXPENSES), scene(point(1), metrics = setOf(HomeTrendMetric.EXPENSES))
            .plot.series.map { it.metric })
    }

    @Test fun `loading and editor changes do not create animation targets`() {
        val state = HomeTrendState(points = listOf(point(1), point(2)).toImmutableList())
        assertEquals(prepareTrendScene(state), prepareTrendScene(state.copy(loading = true, editorVisible = true)))
        // The range chip may update before its replacement data; keep the old chart steady.
        assertEquals(prepareTrendScene(state), prepareTrendScene(state.copy(range = HomeTrendRange.TWELVE_MONTHS)))
    }

    @Test fun `empty and unobserved inputs are finite and have no paths`() {
        listOf(scene(), scene(point(1, observed = false))).forEach { scene ->
            assertTrue(scene.plot.scale.y(BigDecimal.ZERO).isFinite())
            assertTrue(scene.plot.series.all { it.segments.isEmpty() })
        }
    }

    @Test fun `equal amounts occupy the same height for every measure`() {
        val plot = scene(point(1, balance = "100", income = "100", spent = "100")).plot
        assertEquals(1, plot.series.map { it.segments.single().single().y }.distinct().size)
    }

    @Test fun `balance is last so its line is drawn over both filled areas`() {
        assertEquals(listOf(HomeTrendMetric.INCOME, HomeTrendMetric.EXPENSES, HomeTrendMetric.BALANCE),
            scene(point(1)).plot.series.map { it.metric })
    }
}
