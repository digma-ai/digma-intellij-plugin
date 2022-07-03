package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class ParamStats
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("paramName","alwaysNoneValue")
constructor(
    val paramName: String,
    val alwaysNoneValue: String
)
