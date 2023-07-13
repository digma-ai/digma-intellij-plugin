package org.digma.intellij.plugin.insights

import org.digma.intellij.plugin.errors.ErrorsListContainer
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse

class CodelessSpanErrorsContainer(val errorsContainer: ErrorsListContainer, val  insightsResponse: InsightsOfSingleSpanResponse)