package org.digma.intellij.plugin.model.rest.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanInfoByUid
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("environmentId", "spanCodeObjectId")
constructor(
    val environmentId: String,
    val spanCodeObjectId: String,
)
