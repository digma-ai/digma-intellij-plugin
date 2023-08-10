package org.digma.intellij.plugin.model.rest.event

data class LatestCodeObjectEventsRequest(val environments: List<String>,val fromDate: String)
