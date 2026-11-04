package com.ivy.wallet.ui.edit.core

import com.ivy.legacy.datamodel.Account
import java.util.UUID

/** Selection is identity-based; changing a label or balance must not lose its scroll target. */
internal fun selectedAccountIndex(accounts: List<Account>, selectedId: UUID?): Int? =
    selectedId?.let { id -> accounts.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
