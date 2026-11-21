/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.snapshotFlow
import coil3.SingletonImageLoader
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.puremusic_content_blocked
import com.metrolist.shared.puremusic.PureMusicEnforcer
import com.metrolist.shared.puremusic.PureMusicPolicy
import com.metrolist.shared.puremusic.PureMusicSettingsHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** Entry point for everything PureMusic adds to the Android app. Called once from `App.initializeApp`. */
object PureMusic {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var lastNoticeAt = 0L
    private lateinit var appContext: Context

    fun install(application: Application) {
        appContext = application.applicationContext
        PureMusicLegacyMigration.run(application)
        PureMusicEnforcer.start(scope)
        scope.launch {
            snapshotFlow { PureMusicPolicy.imagesAllowed }.distinctUntilChanged().collect { allowed ->
                // Cached bitmaps are served without passing the interceptor, so drop them.
                if (!allowed) SingletonImageLoader.get(application).memoryCache?.clear()
            }
        }
        application.registerActivityLifecycleCallbacks(LockRedirect)
        PureMusicSettingsHome.onOpenControl = { PureMusicControlActivity.start(appContext) }
    }

    /** Shows the "blocked by your settings" toast, at most once every few seconds. */
    fun notifyContentBlocked() {
        if (!::appContext.isInitialized) return
        val now = System.currentTimeMillis()
        if (now - lastNoticeAt < NOTICE_INTERVAL_MS) return
        lastNoticeAt = now
        scope.launch {
            Toast.makeText(appContext, getString(Res.string.puremusic_content_blocked), Toast.LENGTH_SHORT).show()
        }
    }

    /** While the app is locked, any screen other than the control screen sends the user to it. */
    private object LockRedirect : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (PureMusicPolicy.isLocked && activity !is PureMusicControlActivity) {
                PureMusicControlActivity.start(activity)
            }
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private const val NOTICE_INTERVAL_MS = 3_000L
}
