package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class MethodWithCodeObjects // this is like a methodInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("codeObjectId", "relatedSpansCodeObjectIds", "relatedEndpointCodeObjectIds")
constructor(
        val codeObjectId: String, // CodeObjectId of that method
        val relatedSpansCodeObjectIds: List<String>,
        val relatedEndpointCodeObjectIds: List<String>,
)
