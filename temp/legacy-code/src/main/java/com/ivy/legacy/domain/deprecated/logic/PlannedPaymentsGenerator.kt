package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.legacy.Transaction
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.datamodel.temp.toDomain
import com.ivy.legacy.incrementDate
import java.time.Instant
import javax.inject.Inject

class PlannedPaymentsGenerator @Inject constructor(
    private val transactionMapper: TransactionMapper,
    private val transactionRepository: TransactionRepository
) {
    companion object {
        private const val GENERATED_INSTANCES_LIMIT = 72
    }

    suspend fun generate(rule: PlannedPaymentRule, requireComplete: Boolean = false) {
        // delete all not happened transactions
        transactionRepository.deletedByRecurringRuleIdAndNoDateTime(
            recurringRuleId = rule.id
        )

        if (rule.oneTime) {
            generateOneTime(rule, requireComplete)
        } else {
            generateRecurring(rule, requireComplete)
        }
    }

    private suspend fun generateOneTime(rule: PlannedPaymentRule, requireComplete: Boolean) {
        val trns = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id)

        if (trns.isEmpty()) {
            generateTransaction(rule, rule.startDate!!, requireComplete)
        }
    }

    @Suppress("MagicNumber")
    private suspend fun generateRecurring(rule: PlannedPaymentRule, requireComplete: Boolean) {
        val startDate = rule.startDate!!
        val endDate = startDate.plusSeconds(94_608_000)

        val trns = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id)
        var trnsToSkip = trns.size

        var generatedTransactions = 0

        var date = startDate
        while (date.isBefore(endDate)) {
            if (generatedTransactions >= GENERATED_INSTANCES_LIMIT) {
                break
            }

            if (trnsToSkip > 0) {
                // skip first N happened transactions
                trnsToSkip--
            } else {
                // generate transaction
                generateTransaction(
                    rule = rule,
                    dueDate = date,
                    requireComplete = requireComplete,
                )
                generatedTransactions++
            }

            val intervalN = rule.intervalN!!.toLong()
            date = rule.intervalType!!.incrementDate(
                date = date,
                intervalN = intervalN
            )
        }
    }

    private suspend fun generateTransaction(rule: PlannedPaymentRule, dueDate: Instant, requireComplete: Boolean) {
        val transaction = Transaction(
            type = rule.type,
            accountId = rule.accountId,
            recurringRuleId = rule.id,
            categoryId = rule.categoryId,
            amount = rule.amount.toBigDecimal(),
            title = rule.title,
            description = rule.description,
            dueDate = dueDate,
            dateTime = null,
            toAccountId = null,
            isSynced = false
        ).toDomain(transactionMapper)
        check(!requireComplete || transaction != null) { "Planned entry could not be created" }
        transaction?.let { transactionRepository.save(it) }
    }
}
