package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat


/**
 * a factory for ObjectMapper to be used by jcef components and services that need an ObjectMapper,
 * makes sure all will use the same configuration
 */
fun createObjectMapper(): ObjectMapper {
    val objectMapper = ObjectMapper()
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    objectMapper.setDateFormat(StdDateFormat())
    return objectMapper
}