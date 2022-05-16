package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class SpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("instrumentationLibrary", "name", "displayName", "serviceName")
constructor(val instrumentationLibrary: String,
            val name: String,
            val displayName: String,
            val serviceName: String)
