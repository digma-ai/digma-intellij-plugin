package org.digma.intellij.plugin.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat(new StdDateFormat());
    }

    public static <T extends Record> T stringToJavaRecord(String jsonString, Class<T> toClass) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, toClass);
    }

    public static String javaRecordToJsonString(Record theRecord) throws JsonProcessingException {
        return objectMapper.writeValueAsString(theRecord);
    }

    public static String objectToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static JsonNode readTree(String tree) throws JsonProcessingException {
        return objectMapper.readTree(tree);
    }
}

