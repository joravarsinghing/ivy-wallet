package com.ivy.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.entity.GoalAllocationEntity
import com.ivy.data.db.entity.GoalEntity
import com.ivy.data.db.entity.GoalKind
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.GoalRepository
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountsAct: AccountsAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val currencyRepository: CurrencyRepository,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoalScreenState.empty())
    val uiState: StateFlow<GoalScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: GoalsScreenEvent) {
        when (event) {
            GoalsScreenEvent.Back -> Unit
            is GoalsScreenEvent.GoalCreated -> save(event.goal)
            is GoalsScreenEvent.GoalUpdated -> save(event.goal)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val currency = baseCurrencyAct(Unit)
            val accounts = loadAccounts()
            val goals = goalRepository.findActiveGoals()
            val allocations = goals.flatMap { goal ->
                goalRepository.findAllocations(goal.id)
            }

            _uiState.value = GoalScreenState(
                baseCurrency = currency,
                accounts = accounts,
                goals = goals.map { goal ->
                    goal.toUiGoal(
                        allocations = allocations.filter { it.goalId == goal.id },
                        baseCurrency = currency,
                    )
                }
            )
        }
    }

    private suspend fun loadAccounts(): List<GoalAccount> {
        val allTime = ClosedTimeRange.allTimeIvy(timeProvider)
        return accountsAct(Unit).mapNotNull { legacyAccount ->
            val domainAccount = legacyAccount.toDomainAccount(currencyRepository).getOrNull()
                ?: return@mapNotNull null
            val balance = calcAccBalanceAct(
                CalcAccBalanceAct.Input(
                    account = domainAccount,
                    range = allTime,
                )
            ).balance.toDouble()
            GoalAccount(
                id = legacyAccount.id.toString(),
                name = legacyAccount.name,
                balance = balance,
                currency = legacyAccount.currency ?: currencyRepository.getBaseCurrency().code,
            )
        }
    }

    private fun save(goal: Goal) {
        viewModelScope.launch {
            val goalId = goal.id.toUuidOrNew()
            val now = Instant.now()
            val previous = goalRepository.findGoal(goalId)
            val entity = GoalEntity(
                id = goalId,
                name = goal.name,
                kind = goal.type.toEntityKind(),
                targetAmount = goal.targetAmount,
                deadline = goal.deadlineLabel.toDeadlineInstantOrNull(),
                isActive = goal.status != GoalStatus.PAUSED,
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
            )
            val allocations = goal.allocations.mapNotNull { allocation ->
                allocation.accountId.toUuidOrNull()?.let { accountId ->
                    GoalAllocationEntity(
                        goalId = goalId,
                        accountId = accountId,
                        reservedAmount = allocation.amount.coerceAtLeast(0.0),
                        createdAt = previous?.createdAt ?: now,
                        updatedAt = now,
                    )
                }
            }
            goalRepository.saveGoalWithAllocations(entity, allocations)
            refresh()
        }
    }
}

private fun GoalEntity.toUiGoal(
    allocations: List<GoalAllocationEntity>,
    baseCurrency: String,
): Goal = Goal(
    id = id.toString(),
    name = name,
    type = kind.toUiType(),
    targetAmount = targetAmount,
    currency = baseCurrency,
    deadlineLabel = deadline?.atZone(ZoneOffset.UTC)?.let {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US).format(it)
    },
    status = if (isActive) GoalStatus.ACTIVE else GoalStatus.PAUSED,
    allocations = allocations.map {
        GoalAllocation(
            accountId = it.accountId.toString(),
            amount = it.reservedAmount,
        )
    },
)

private fun GoalType.toEntityKind(): GoalKind = when (this) {
    GoalType.EMERGENCY_FUND -> GoalKind.EMERGENCY_FUND
    GoalType.PURCHASE -> GoalKind.PURCHASE
    GoalType.SINKING_FUND -> GoalKind.SINKING_FUND
    GoalType.CUSTOM -> GoalKind.CUSTOM
}

private fun GoalKind.toUiType(): GoalType = when (this) {
    GoalKind.EMERGENCY_FUND -> GoalType.EMERGENCY_FUND
    GoalKind.PURCHASE -> GoalType.PURCHASE
    GoalKind.SINKING_FUND -> GoalType.SINKING_FUND
    GoalKind.CUSTOM -> GoalType.CUSTOM
}

private fun String?.toDeadlineInstantOrNull(): Instant? {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return null
    return runCatching {
        YearMonth.parse(value, DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            .atDay(1)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
    }.getOrNull()
}

private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

private fun String.toUuidOrNew(): UUID = toUuidOrNull() ?: UUID.randomUUID()
