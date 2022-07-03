package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Percentile
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("fraction", "maxDuration")
constructor(val fraction: Double,
            val maxDuration: Duration)
