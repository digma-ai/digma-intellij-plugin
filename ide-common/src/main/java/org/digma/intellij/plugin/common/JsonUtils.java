package org.digma.intellij.plugin.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T extends Record> T stringToJavaRecord(String jsonString, Class<T> toClass) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, toClass);
    }

    public static String javaRecordToJsonString(Record theRecord) throws JsonProcessingException {
        return objectMapper.writeValueAsString(theRecord);
    }

    public static String objectToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

}
