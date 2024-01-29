package org.digma.intellij.plugin.model.rest.tests

data class FilterForLatestTests(
    var environments: Set<String>,
    var pageNumber: Int = 1,
)
