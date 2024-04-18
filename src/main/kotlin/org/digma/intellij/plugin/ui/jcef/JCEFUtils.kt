package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode


fun getQueryMapFromPayload(requestJsonNode: JsonNode, objectMapper: ObjectMapper): MutableMap<String, Any> {

    val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
    val payloadQuery: JsonNode = objectMapper.readTree(payloadNode.get("query").toString())

    return getMapFromNode(payloadQuery, objectMapper)
}

fun getMapFromNode(requestJsonNode: JsonNode, objectMapper: ObjectMapper): MutableMap<String, Any> {

    val backendQueryParams = mutableMapOf<String, Any>()

    if (requestJsonNode is ObjectNode) {

        val payloadQueryAsMap = objectMapper.convertValue(requestJsonNode, Map::class.java)

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

    return backendQueryParams
}