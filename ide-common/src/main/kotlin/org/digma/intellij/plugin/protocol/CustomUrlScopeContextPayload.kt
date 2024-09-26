package org.digma.intellij.plugin.protocol

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class CustomUrlScopeContextPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("targetTab")
constructor(val targetTab: String?)