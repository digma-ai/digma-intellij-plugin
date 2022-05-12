package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentTests extends AbstractAnalyticsProviderTest {

    @Test
    public void getEnvironmentsTest() throws JsonProcessingException {

        List<String> expectedEnvs = new ArrayList<>();
        expectedEnvs.add("env1");
        expectedEnvs.add("env2");
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedEnvs))
                .addHeader("Content-Type", "application/json"));


        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<String> envsResult = restAnalyticsProvider.getEnvironments();

        assertEquals(expectedEnvs.size(), envsResult.size(), "unexpected environments size");
        assertIterableEquals(expectedEnvs, envsResult, "unexpected environments result");
    }


    @Test
    public void getEnvironmentsEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<String> envsResult = restAnalyticsProvider.getEnvironments();

        assertEquals(0, envsResult.size(), "unexpected environments size");
        assertIterableEquals(Collections.emptyList(), envsResult, "unexpected environments result");
    }

    @Test
    public void getEnvironmentsNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getEnvironments();
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    @Test
    public void getEnvironmentsErrorResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getEnvironments();
        });

        assertEquals(500, exception.getResponseCode());
    }


    @Test
    public void getEnvironmentsWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("MYENV"))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getEnvironments();
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }


}
