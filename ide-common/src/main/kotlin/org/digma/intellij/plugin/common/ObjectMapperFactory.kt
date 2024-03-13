package org.digma.intellij.plugin.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat


/**
 * a factory for ObjectMapper to be used by components and services that need an ObjectMapper,
 * makes sure all will use the same configuration.
 */
fun createObjectMapper(): ObjectMapper {
    val objectMapper = ObjectMapper()
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    objectMapper.setDateFormat(StdDateFormat())
    return objectMapper
}

/**
 * ObjectMapper is fully thread safe and does not need to be created many times.
 * usually its more convenient to create a class member in classes that need to use an ObjectMapper
 * but not necessary, all the code can use the same instance.
 * a good reason to create a local ObjectMapper is if the common configuration in createObjectMapper
 * is not sufficient.
 */
val sharedObjectMapper = createObjectMapper()