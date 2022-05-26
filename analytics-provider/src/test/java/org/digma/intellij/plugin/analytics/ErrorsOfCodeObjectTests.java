package org.digma.intellij.plugin.analytics;

import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.digma.intellij.plugin.analytics.UtilForTest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorsOfCodeObjectTests extends AbstractAnalyticsProviderTest {

    @Test
    public void ableToParse2Items() {
        final var jsonContent = loadTextFile("/jsons/errors_of_code_object_2_items.json");
        mockBackEnd.enqueue(new MockResponse()
                .setBody(jsonContent)
                .addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));

        RestAnalyticsProvider analyticsProvider = instanceOfRestAnalyticsProvider();
        List<CodeObjectError> errors = analyticsProvider.getErrorsOfCodeObject("a", "b");

        assertEquals(2, errors.size(), "count of returned errors");
    }

}
