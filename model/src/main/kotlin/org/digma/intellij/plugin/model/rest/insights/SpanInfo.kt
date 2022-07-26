package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("instrumentationLibrary", "name", "displayName", "serviceName", "codeObjectId")
constructor(val instrumentationLibrary: String,
            val name: String,
            val displayName: String,
            val serviceName: String?,
            val codeObjectId: String?)
