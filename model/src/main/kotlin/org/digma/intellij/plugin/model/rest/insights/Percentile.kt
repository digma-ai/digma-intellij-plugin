package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class Percentile
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("fraction", "maxDuration")
constructor(val fraction: Double,
            val maxDuration: Duration)
