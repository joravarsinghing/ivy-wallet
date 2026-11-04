package com.ivy.legacy.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import java.util.UUID

/** Live form ownership, not serialized database objects or pending work in a Bundle. */
@Stable
class EditorModalHost {
    val tags = com.ivy.legacy.ui.component.tags.TagEditorHost()
    var account: AccountModalData? by mutableStateOf(null)
    var category: CategoryModalData? by mutableStateOf(null)

    fun closeAccount(expected: UUID?) {
        if (account?.id == expected) account = null
    }

    fun closeCategory(expected: UUID?) {
        if (category?.id == expected) category = null
    }
}
