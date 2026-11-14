package com.nedmah.textlector.common.platform.review

import android.content.Context

// todo: Google Play In-App Review
class StoreReviewRequester(@Suppress("UNUSED_PARAMETER") context: Context) : ReviewRequester {
    override suspend fun requestReview() = Unit
}