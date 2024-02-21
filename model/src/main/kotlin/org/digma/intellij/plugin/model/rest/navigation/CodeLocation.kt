package org.digma.intellij.plugin.model.rest.navigation

import org.digma.intellij.plugin.model.code.CodeDetails

data class CodeLocation(
    val codeDetailsList: List<CodeDetails> = listOf(),
    val relatedCodeDetailsList: List<CodeDetails> = listOf(),
)
