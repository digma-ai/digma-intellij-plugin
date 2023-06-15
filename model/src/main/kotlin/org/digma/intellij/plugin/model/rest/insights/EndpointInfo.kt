package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("route", "serviceName", "instrumentationLibrary", "codeObjectId","spanCodeObjectId")
constructor(
    val route: String,
    val serviceName: String,
    val instrumentationLibrary: String,
    val codeObjectId: String?,
    val spanCodeObjectId:String
)
