package org.digma.intellij.plugin.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule


/**
 * a factory for ObjectMapper to be used by components and services that need an ObjectMapper,
 * makes sure all will use the same configuration.
 */
fun createObjectMapper(): ObjectMapper {
    //Note that it is risky to change configuration or install modules.
    // because there are components that already use this object mapper as is
    // and changes to configuration may cause issues. its probably mainly dates that were
    // serialized to persistence.
    //but, probably installing the JavaTimeModule should be OK because it's backwards compatible.
    //users of this factory may change configuration on the instance they create.
    val objectMapper = ObjectMapper()
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    objectMapper.setDateFormat(StdDateFormat())
    return objectMapper
}


fun createObjectMapperWithJavaTimeModule(): ObjectMapper {
    //install the JavaTimeModule for better serialization of dates
    val objectMapper = ObjectMapper()
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    objectMapper.registerModules(JavaTimeModule())
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