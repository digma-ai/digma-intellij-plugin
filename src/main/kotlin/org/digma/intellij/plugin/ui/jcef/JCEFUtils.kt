package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.digma.intellij.plugin.persistence.PersistenceService


fun getQueryMapFromPayload(requestJsonNode: JsonNode): Map<String, Any> {

    val objectMapper: ObjectMapper = createObjectMapper()
    val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
    val payloadQuery: JsonNode = objectMapper.readTree(payloadNode.get("query").toString())

    val backendQueryParams = mutableMapOf<String, Any>()

    if (payloadQuery is ObjectNode) {

        val payloadQueryAsMap = objectMapper.convertValue(payloadQuery, Map::class.java)

        payloadQueryAsMap.forEach { entry: Map.Entry<Any?, Any?> ->
            entry.key?.let {
                val value = entry.value
                if (value is List<*>) {
                    backendQueryParams[it.toString()] = value.joinToString(",")
                } else {
                    if (value != null) {
                        backendQueryParams[it.toString()] = value.toString()
                    }
                }
            }
        }
    }

    backendQueryParams["environment"] = PersistenceService.getInstance().getCurrentEnv() ?: ""
    return backendQueryParams
}