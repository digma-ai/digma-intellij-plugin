package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("route", "serviceName", "instrumentationLibrary", "codeObjectId","spanCodeObjectId", "entrySpanCodeObjectId")
constructor(
    val route: String,
    val serviceName: String,
    val instrumentationLibrary: String,
    val codeObjectId: String?,
    val spanCodeObjectId:String,
    val entrySpanCodeObjectId:String?, // Nullable for BC
)
