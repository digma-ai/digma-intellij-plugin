package org.digma.intellij.plugin.posthog

import org.digma.intellij.plugin.model.rest.insights.InsightType

data class InsightToReopenCount(val insightType: InsightType, val reopenCount: Int)
