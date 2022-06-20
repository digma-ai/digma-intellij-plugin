package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class OriginService
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("serviceName")
constructor(
    val serviceName: String
)
