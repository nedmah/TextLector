package com.nedmah.textlector.common.platform.review

interface ReviewRequester {
    suspend fun requestReview()
}