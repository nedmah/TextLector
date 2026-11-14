package com.nedmah.textlector.common.platform.review

import android.content.Context
import com.nedmah.textlector.common.platform.logging.CrashReporter
import kotlinx.coroutines.CancellationException
import ru.rustore.sdk.review.RuStoreReviewManagerFactory

class StoreReviewRequester(context: Context) : ReviewRequester {

    private val manager = RuStoreReviewManagerFactory.create(context)

    override suspend fun requestReview() {
        try {
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(reviewInfo).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // RuStore not installed, user not signed in,
            // already reviewed, request limit reached, Never shown to the user
            CrashReporter.log("RuStore review not shown: ${e::class.simpleName}", tag = "Review")
        }
    }

}