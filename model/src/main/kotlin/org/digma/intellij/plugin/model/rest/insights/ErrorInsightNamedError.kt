package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorInsightNamedError
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("uid", "errorType", "codeObjectId", "sourceCodeObjectId")
constructor(val uid: String,
            val errorType: String,
            val codeObjectId: String,
            val sourceCodeObjectId: String)

