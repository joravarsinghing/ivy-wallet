package com.ivy.legacy.utils

/** Compatibility shims only. No diagnostics SDK, network or potentially private logs. */
@Suppress("UNUSED_PARAMETER")
fun sendToCrashlytics(
    msg: String
) = Unit

@Suppress("UNUSED_PARAMETER")
fun Exception.sendToCrashlytics(
    clarification: String? = null
) = Unit

@Suppress("UNUSED_PARAMETER")
fun logToCrashlytics(msg: String) = Unit

class DeveloperException(msg: String) : Exception(msg)
