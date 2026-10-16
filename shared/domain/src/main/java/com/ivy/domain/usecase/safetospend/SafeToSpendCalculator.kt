package com.ivy.domain.usecase.safetospend

import java.math.BigDecimal

/** Describes how much of the current balance can be spent safely. */
enum class SafeToSpendStatus(
    val explanation: String,
) {
    POSITIVE(
        "This amount can be spent while keeping your protected buffer intact.",
    ),
    ZERO(
        "Your balance matches your protected buffer, so there is nothing safe to spend.",
    ),
    BUFFER_EXCEEDED(
        "Your protected buffer is larger than your balance, so spending should pause until the balance recovers.",
    ),
}

data class SafeToSpendResult(
    val safeToSpend: BigDecimal,
    val status: SafeToSpendStatus,
)

/**
 * Calculates the amount available after preserving a protected buffer.
 *
 * Monetary arithmetic remains exact: the result is always calculated with
 * [BigDecimal] subtraction and is never rounded or converted to a floating
 * point type.
 */
fun calculateSafeToSpend(
    currentBalance: BigDecimal,
    protectedBuffer: BigDecimal,
): SafeToSpendResult {
    val safeToSpend = currentBalance.subtract(protectedBuffer)

    val status = when {
        safeToSpend.signum() > 0 -> SafeToSpendStatus.POSITIVE
        safeToSpend.signum() == 0 -> SafeToSpendStatus.ZERO
        else -> SafeToSpendStatus.BUFFER_EXCEEDED
    }

    return SafeToSpendResult(
        safeToSpend = safeToSpend,
        status = status,
    )
}
