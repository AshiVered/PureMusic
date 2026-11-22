/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import com.metrolist.shared.puremusic.PureMusicPolicy

/** First interceptor of the app's Coil loader: while images are hidden, no image is fetched, decoded or read from cache. */
object PureMusicImageInterceptor : Interceptor {
    /** PureMusic's own images that are not content: always shown. */
    private val alwaysShown = setOf(DEVELOPER_AVATAR)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        if (PureMusicPolicy.imagesAllowed || chain.request.data in alwaysShown) return chain.proceed()
        return ErrorResult(
            image = null,
            request = chain.request,
            throwable = ImagesHiddenException(),
        )
    }

    private class ImagesHiddenException : IllegalStateException("Images are hidden by PureMusic")
}
