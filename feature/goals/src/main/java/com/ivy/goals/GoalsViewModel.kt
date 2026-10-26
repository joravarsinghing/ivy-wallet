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
import kotlinx.coroutines.CancellationException
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
    private val _uiState = MutableStateFlow(GoalScreenState.empty().copy(loading = true))
    val uiState: StateFlow<GoalScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: GoalsScreenEvent) {
        when (event) {
            GoalsScreenEvent.Back -> Unit
            GoalsScreenEvent.Retry -> refresh()
            is GoalsScreenEvent.GoalCreated -> save(event.goal)
            is GoalsScreenEvent.GoalUpdated -> save(event.goal)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                loadState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Couldn't load goals. Please retry.")
            }
        }
    }

    private suspend fun loadState() {
        val currency = baseCurrencyAct(Unit)
        val asOfDate = timeProvider.localDateNow()
        val accounts = loadAccounts()
        val goals = goalRepository.findActiveGoals()
        val allocations = goals.flatMap { goal -> goalRepository.findAllocations(goal.id) }

        _uiState.value = _uiState.value.copy(
            baseCurrency = currency,
            accounts = accounts,
            asOfDate = asOfDate,
            goals = goals.map { goal ->
                goal.toUiGoal(
                    allocations = allocations.filter { it.goalId == goal.id },
                    baseCurrency = currency,
                    asOfDate = asOfDate,
                )
            },
            loading = false,
        )
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
        if (_uiState.value.saving || _uiState.value.loading) return
        _uiState.value = _uiState.value.copy(saving = true, error = null)
        viewModelScope.launch {
            var persisted = false
            try {
                require(goalInputError(goal.name, goal.targetAmount.toString()) == null) {
                    "Enter a name and a positive target amount."
                }
                require(goal.currency == _uiState.value.baseCurrency) { "Use the wallet currency for this goal." }
                require(goal.allocations.all { it.amount.isFinite() && it.amount >= 0 }) { "Enter valid reservation amounts." }
                val goalId = UUID.fromString(goal.id)
                val now = timeProvider.utcNow()
                val previous = goalRepository.findGoal(goalId)
                val entity = GoalEntity(
                    id = goalId,
                    name = goal.name,
                    kind = goal.type.toEntityKind(),
                    targetAmount = goal.targetAmount,
                    deadline = goal.deadlineInstant(),
                    isActive = goal.status != GoalStatus.PAUSED,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                )
                val allocations = goal.allocations.map { allocation ->
                    GoalAllocationEntity(
                        goalId = goalId,
                        accountId = UUID.fromString(allocation.accountId),
                        reservedAmount = allocation.amount,
                        createdAt = previous?.createdAt ?: now,
                        updatedAt = now,
                    )
                }
                goalRepository.saveGoalWithAllocations(entity, allocations)
                persisted = true
                loadState()
                _uiState.value = _uiState.value.copy(saving = false, saveRevision = _uiState.value.saveRevision + 1)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    saveRevision = _uiState.value.saveRevision + if (persisted) 1 else 0,
                    error = when {
                        persisted -> "The goal was saved, but the list couldn't refresh. Please retry."
                        failure is IllegalArgumentException -> failure.message ?: "Check the goal details and try again."
                        else -> "Couldn't save the goal. Please retry."
                    },
                )
            }
        }
    }
}
private fun GoalType.toEntityKind(): GoalKind = when (this) {
    GoalType.EMERGENCY_FUND -> GoalKind.EMERGENCY_FUND
    GoalType.PURCHASE -> GoalKind.PURCHASE
    GoalType.SINKING_FUND -> GoalKind.SINKING_FUND
    GoalType.CUSTOM -> GoalKind.CUSTOM
}
