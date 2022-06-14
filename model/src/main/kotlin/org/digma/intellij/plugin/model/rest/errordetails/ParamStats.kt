package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class ParamStats
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("paramName","alwaysNoneValue")
constructor(
    val paramName: String,
    val alwaysNoneValue: String
)
