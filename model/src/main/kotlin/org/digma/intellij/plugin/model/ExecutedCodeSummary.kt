package org.digma.intellij.plugin.model

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class ExecutedCodeSummary
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("code", "exceptionType", "exceptionMessage", "handled", "unexpected", "codeLineNumber")
constructor(
    val code: String,
    val exceptionType: String,
    val exceptionMessage: String,
    val handled: Boolean,
    val unexpected: Boolean,
    val codeLineNumber: Int
)
