package com.ivy.accounts

import com.ivy.domain.usecase.networth.NetWorthAccountRole
import java.util.UUID

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<com.ivy.legacy.data.model.AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent
}
