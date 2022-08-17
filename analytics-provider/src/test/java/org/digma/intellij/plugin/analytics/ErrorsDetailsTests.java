package org.digma.intellij.plugin.analytics;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.digma.intellij.plugin.analytics.UtilForTest.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ErrorsDetailsTests extends AbstractAnalyticsProviderTest {


//    @Test
//    public void testWithRealEnv(){
//        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("http://localhost:5051");
//        var errorDetails = analyticsProvider.getCodeObjectErrorDetails("cb53eab4-d442-11ec-baea-0242ac140003");
//        assertNotNull(errorDetails, "error details is null");
//    }


    @Test
    void testErrorDetails() {
        final var jsonContent = loadTextFile("/jsons/error-details.json");
        mockBackEnd.enqueue(new MockResponse()
                .setBody(jsonContent)
                .addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));


        try (RestAnalyticsProvider analyticsProvider = instanceOfRestAnalyticsProvider()) {

            var errorDetails = analyticsProvider.getCodeObjectErrorDetails("");

            assertNotNull(errorDetails, "error details is null");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
