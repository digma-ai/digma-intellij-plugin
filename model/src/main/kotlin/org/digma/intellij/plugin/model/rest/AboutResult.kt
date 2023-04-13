package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class AboutResult @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("applicationVersion")
constructor(
    val applicationVersion: String
)