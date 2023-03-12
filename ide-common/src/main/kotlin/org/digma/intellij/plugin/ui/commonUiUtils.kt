package org.digma.intellij.plugin.ui

import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import kotlin.math.abs


fun needToShowDurationChange(percentile: SpanDurationsPercentile): Boolean {
    val tolerationConstant: Long = 10000

    if (percentile.previousDuration != null && percentile.changeTime != null) {
        val rawDiff: Long = abs(percentile.currentDuration.raw - percentile.previousDuration!!.raw)
        return ((rawDiff.toFloat() / percentile.previousDuration!!.raw) > 0.1) && (rawDiff > tolerationConstant)
    }
    return false
}