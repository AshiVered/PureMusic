package com.metrolist.shared.utils

import com.metrolist.shared.BuildConfig

@Suppress("UNUSED_PARAMETER")
fun isCrashReportingAvailable(appVersion: String = BuildConfig.APP_VERSION): Boolean = false // PUREMUSIC-HOOK: no-crash-reports
