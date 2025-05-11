package org.digma.intellij.plugin.ui.jaegerui.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.digma.intellij.plugin.common.CodeObjectsUtil
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Span
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "id",
    "name",
    "instrumentationLibrary",
    "spanCodeObjectId",
    "methodCodeObjectId",
    "function",
    "namespace"
)
constructor(
    val id: String,
    val name: String,
    val instrumentationLibrary: String,
    val spanCodeObjectId: String?,
    // can be null or empty
    val methodCodeObjectId: String?,
    // deprecated - use methodCodeObjectId
    val function: String?,
    // deprecated - use methodCodeObjectId
    val namespace: String?
) {

    fun spanId(): String {
        return if (spanCodeObjectId != null) {
            // TODO: once spanCodeObjectId is required, remove this if
            CodeObjectsUtil.stripSpanPrefix(spanCodeObjectId)
        } else {
            // backward compatibility (not fully accurate)
            CodeObjectsUtil.createSpanId(instrumentationLibrary, name)
        }
    }

    fun methodId(): String? {
        return when {
            methodCodeObjectId != null -> CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId)
            function != null && namespace != null -> CodeObjectsUtil.createMethodCodeObjectId(namespace, function)
            else -> null
        }
    }
}