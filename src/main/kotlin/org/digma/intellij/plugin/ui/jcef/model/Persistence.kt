package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties


enum class Scope {
    application, project
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SaveToPersistenceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SaveToPersistencePayload,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SaveToPersistencePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("key", "value", "scope")
constructor(val key: String, val value: JsonNode, val scope: Scope)


@JsonIgnoreProperties(ignoreUnknown = true)
class GetFromPersistenceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: GetFromPersistencePayload,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class GetFromPersistencePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("key", "scope")
constructor(val key: String, val scope: Scope)


class SetFromPersistenceMessage(val payload: SetFromPersistenceMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_FROM_PERSISTENCE
}

class SetFromPersistenceMessagePayload(val key: String, val value: JsonNode?, val scope: Scope, val error: ErrorPayload? = null)
