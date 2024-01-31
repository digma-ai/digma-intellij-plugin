package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import java.beans.ConstructorProperties


enum class Scope {
    application, project
}

class SaveToPersistenceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SaveToPersistencePayload,
)

class SaveToPersistencePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("key", "value", "scope")
constructor(val key: String, val value: String, val scope: Scope)


class GetFromPersistenceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: GetFromPersistencePayload,
)

class GetFromPersistencePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("key", "scope")
constructor(val key: String, val scope: Scope)


class SetFromPersistenceMessage(val payload: SetFromPersistenceMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_FROM_PERSISTENCE
}

class SetFromPersistenceMessagePayload(val key: String, val value: String?, val scope: Scope, val error: ErrorPayload? = null)
