package com.ivy.networth

import com.ivy.data.model.wealth.*
import kotlinx.serialization.Serializable

@Serializable
data class WealthSnapshotPosition(
    val itemId: String,
    val nativeAmount: String?,
    val reportingAmount: String?,
    val valuationDate: String?,
    val exchangeRate: String?,
    val rateIds: List<String>,
    val included: Boolean,
    val issue: String?,
)

/** Only contributing inputs, rather than a growing copy of the entire valuation history. */
@Serializable
data class WealthSnapshotInputs(
    val version: Int = 1,
    val asOf: String,
    val currency: String,
    val accountingZone: String,
    val items: List<WealthItem>,
    val policies: List<WealthPolicy>,
    val valuations: List<WealthValuation>,
    val rates: List<WealthFx>,
    val positions: List<WealthSnapshotPosition>,
    val issues: List<String>,
)
