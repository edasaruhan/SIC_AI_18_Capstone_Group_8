package com.group8.wellbeing.domain

enum class FeedbackAnswer {
    YES,
    NO
}

enum class FeedbackResponseStatus {
    ANSWERED,
    SKIPPED,
    UNANSWERED,
    EXPIRED
}

data class FeedbackRecord(
    val participantId: String,
    val windowId: Long?,
    val predictionId: Long?,
    val windowStartMs: Long,
    val windowEndMs: Long,
    val answer: FeedbackAnswer?,
    val responseStatus: FeedbackResponseStatus,
    val askedAtMs: Long,
    val answeredAtMs: Long?,
    val updatedAtMs: Long?,
    val questionVersion: String
) {
    companion object {
        const val QUESTION_VERSION: String = "q-v1"
    }
}
