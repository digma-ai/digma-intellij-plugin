package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class UnmappedInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "actualStartTime", "customStartTime", "prefixedCodeObjectId","type")
constructor(
        override val codeObjectId: String,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        @JsonProperty("type")
        val theType: String,
) : CodeObjectInsight {

    override val type: InsightType = InsightType.Unmapped
}
