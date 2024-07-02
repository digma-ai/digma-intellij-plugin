package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties

data class ChangeScopeSpan
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId"
) constructor(val spanCodeObjectId: String? = null)

//GLOBAL/CHANGE_SCOPE
data class ChangeScopeMessage
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "span",
    "environmentId",
    "forceNavigation",
    "context"
)
constructor(
    val span: ChangeScopeSpan? = null,
    val environmentId: String? = null,
    @get:JsonProperty("forceNavigation")
    @param:JsonProperty("forceNavigation")
    val forceNavigation: Boolean? = null,
    val context: ScopeContext? = null
)


data class SetScopeMessage(
    val payload: SetScopeMessagePayload
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_SCOPE
}

data class SetScopeMessagePayload(
    val span: SpanScope?,
    val code: CodeLocation,
    @get:JsonProperty("hasErrors")
    @param:JsonProperty("hasErrors")
    val hasErrors: Boolean,
    val analyticsInsightsCount: Number,
    val issuesInsightsCount: Number,
    val unreadInsightsCount: Number,
    val context: ScopeContext? = null,
    val environmentId: String? = null
)

