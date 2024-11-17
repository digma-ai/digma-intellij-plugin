package org.digma.intellij.plugin.model.rest.codelens

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.InsightImportance
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Decorator
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "title",
    "description",
    "codeObjectId",
    "spanCodeObjectId",
    "importance"
)
constructor(
    val title: String,
    val description: String,
    val codeObjectId: String,
    val spanCodeObjectId: String?,
    val importance: InsightImportance
) {
    @JsonIgnore
    fun getScopeCodeObjectId(): String? {
        return if (codeObjectId.startsWith("span:")) {
            codeObjectId
        } else if (spanCodeObjectId != null && spanCodeObjectId.startsWith("span:")) {
            spanCodeObjectId
        } else {
            null
        }
    }
}
