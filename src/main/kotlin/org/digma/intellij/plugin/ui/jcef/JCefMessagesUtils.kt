package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper


/**
 * try to find a field in the payload without throwing NPE.
 * requestJsonNode is the full request as json node
 */
fun tryGetFieldFromPayload(objectMapper: ObjectMapper, requestJsonNode: JsonNode, fieldName: String): String? {
    try {
        return objectMapper.readTree(requestJsonNode.get("payload").toString()).get(fieldName).asText()
    } catch (e: NullPointerException) {
        return null
    }
}