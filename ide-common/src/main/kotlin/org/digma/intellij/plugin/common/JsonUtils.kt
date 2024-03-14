package org.digma.intellij.plugin.common

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode


@Throws(JsonProcessingException::class)
fun objectToJson(value: Any): String {
    return sharedObjectMapper.writeValueAsString(value)
}

@Throws(JsonProcessingException::class)
fun readTree(tree: String): JsonNode {
    return sharedObjectMapper.readTree(tree)
}

fun objectNodeToMap(objectNode: ObjectNode): Map<String, Any> {
    val ref = object : TypeReference<Map<String, Any>>() {}
    return sharedObjectMapper.convertValue(objectNode, ref)

//    val result = mutableMapOf<String,String>()
//    objectNode.fields().forEachRemaining {
//        result[it.key] = it.value?.asText() ?: ""
//    }
//    return result
}

@Throws(NullPointerException::class)
fun getStringValueFromNode(jsonNode: JsonNode, name: String): String {
    return jsonNode.get(name).asText()
}

fun getStringValueFromNodeOrNull(jsonNode: JsonNode, name: String): String? {
    return jsonNode.get(name)?.asText()
}