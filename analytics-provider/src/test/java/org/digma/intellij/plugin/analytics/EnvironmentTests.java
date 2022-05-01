package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ProcessingException;
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

        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<String> envsResult = restAnalyticsProvider.getEnvironments();

        assertNull(envsResult, "environments result should be null");
    }

    @Test
    public void getEnvironmentsErrorResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        InternalServerErrorException exception = assertThrows(InternalServerErrorException.class, () -> {
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getEnvironments();
        });

        assertEquals(500, exception.getResponse().getStatus());
    }


    @Test
    public void getEnvironmentsWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("MYENV"))
                .addHeader("Content-Type", "application/json"));

        ProcessingException exception = assertThrows(ProcessingException.class, () -> {
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getEnvironments();
        });

        assertTrue(exception.getMessage().contains("jakarta.ws.rs.ProcessingException: com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot construct instance of `java.util.ArrayList` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value ('MYENV')"));
    }


}
