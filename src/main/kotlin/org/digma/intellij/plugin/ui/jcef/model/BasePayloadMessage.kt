package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.databind.JsonNode

abstract class BasePayloadMessage(val type: String, val action: String, val payload: Payload)

class Payload(val data: JsonNode?, val error: ErrorPayload? = null)

class ErrorPayload(val message: String)
