package org.digma.intellij.plugin.model.rest.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
open class SpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "instrumentationLibrary",
        "name",
        "spanCodeObjectId",
        "displayName",
        "methodCodeObjectId",
        "kind",
)
constructor(
        open val instrumentationLibrary: String,
        open val name: String,
        open val spanCodeObjectId: String,
        open val displayName: String,
        open val methodCodeObjectId: String?,
        open val kind: String?,
)
