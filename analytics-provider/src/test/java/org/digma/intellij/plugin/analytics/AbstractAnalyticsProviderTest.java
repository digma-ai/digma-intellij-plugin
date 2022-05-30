package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

public abstract class AbstractAnalyticsProviderTest {


    protected static MockWebServer mockBackEnd;
    protected ObjectMapper objectMapper = new ObjectMapper();

    protected String baseUrl;


    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @BeforeEach
    void initialize() {
        baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());
    }

    protected RestAnalyticsProvider instanceOfRestAnalyticsProvider() {
        return new RestAnalyticsProvider(baseUrl);
    }
}
