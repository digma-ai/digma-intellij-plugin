package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class CloseLiveViewMessage
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(val action: String, val payload: CloseLiveViewPayload)

data class CloseLiveViewPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId")
constructor(val codeObjectId: String)