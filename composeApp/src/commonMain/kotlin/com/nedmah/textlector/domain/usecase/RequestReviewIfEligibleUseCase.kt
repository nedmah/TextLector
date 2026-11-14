package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.common.platform.review.ReviewRequester
import com.nedmah.textlector.domain.repository.PreferencesRepository

// Called when the user listened to a document to the end
// How often the form is actually shown is throttled by the store itself
class RequestReviewIfEligibleUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val reviewRequester: ReviewRequester,
) {
    suspend operator fun invoke() {
        val finishedCount = preferencesRepository.incrementFinishedDocumentsCount()
        if (finishedCount >= MIN_FINISHED_DOCUMENTS) reviewRequester.requestReview()
    }

    private companion object {
        const val MIN_FINISHED_DOCUMENTS = 2
    }
}