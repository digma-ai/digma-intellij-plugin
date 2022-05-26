package org.digma.intellij.plugin.analytics;

import java.io.IOException;

public final class UtilForTest {

    private UtilForTest() {
    }

    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";

    public static String loadTextFile(String path) {
        try (var inputStream = UtilForTest.class.getResourceAsStream(path)) {
            return new String(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
