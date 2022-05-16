package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class Duration
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("value", "unit", "raw")
constructor(val value: Double,
val unit: String,
val raw: Long)