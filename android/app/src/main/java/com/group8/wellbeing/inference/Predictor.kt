package com.group8.wellbeing.inference

data class FeatureVector(
    val screenSeconds: Double,
    val sessionsStarted: Int,
    val longestRunSeconds: Double,
    val appSwitches: Int,
    val previousSessionSeconds: Double,
    val hour: Int,
    val dayOfWeek: Int,
    val schemaVersion: String
)

interface Predictor {
    val modelVersion: String
    val isReady: Boolean
}
