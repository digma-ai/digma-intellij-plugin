package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RootCauseSpan
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("sampleTraceId",
                        "name",
                        "displayName",
                        "instrumentationLibrary",
                        "serviceName",
                        "codeObjectId",
                        "kind")
constructor(val sampleTraceId: String,
            val name: String,
            val displayName: String,
            val instrumentationLibrary: String,
            val serviceName: String?,
            val codeObjectId: String?,
            val kind: String = "Internal")

