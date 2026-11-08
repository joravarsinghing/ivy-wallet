package com.ivy.importdata.csvimport.flow

import com.ivy.data.backup.CSVRow
import com.ivy.design.system.FinancialVisibility

/** CSV columns vary by source; hide the whole raw payload rather than guessing which is money. */
internal fun failedImportRowText(row: CSVRow, visibility: FinancialVisibility): String =
    "Row ${row.index}: ${visibility.text(row.content.joinToString(" · "))}"
