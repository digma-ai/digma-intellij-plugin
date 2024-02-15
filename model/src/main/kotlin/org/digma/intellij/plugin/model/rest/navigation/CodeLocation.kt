package org.digma.intellij.plugin.model.rest.navigation

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.code.CodeDetails

data class CodeLocation(
    @get:JsonProperty("isAlreadyAtCode")
    @param:JsonProperty("isAlreadyAtCode")
    val isAlreadyAtCode: Boolean,
    val codeDetailsList: List<CodeDetails> = listOf(),
    val relatedCodeDetailsList: List<CodeDetails> = listOf(),
)
