package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class OriginService
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("serviceName")
constructor(
    val serviceName: String
)
