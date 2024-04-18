package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.rest.environment.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
class EnvironmentTests extends AbstractAnalyticsProviderTest {

    @Test
    void getEnvironmentsTest() throws JsonProcessingException {

        List<Env> expectedEnvs = new ArrayList<>();
        expectedEnvs.add(new Env("myid1", "myname1", EnvType.Public));
        expectedEnvs.add(new Env("myid2", "myname2", EnvType.Private));
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedEnvs))
                .addHeader("Content-Type", "application/json"));


        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<Env> envsResult = restAnalyticsProvider.getEnvironments();

        assertEquals(expectedEnvs.size(), envsResult.size(), "unexpected environments size");
        assertIterableEquals(expectedEnvs, envsResult, "unexpected environments result");
    }


    @Test
    void getEnvironmentsEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<Env> envsResult = restAnalyticsProvider.getEnvironments();

        assertEquals(0, envsResult.size(), "unexpected environments size");
        assertIterableEquals(Collections.emptyList(), envsResult, "unexpected environments result");
    }

    @Test
    void getEnvironmentsNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getEnvironments();
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    @Test
    void getEnvironmentsErrorResultTest() {

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
    void getEnvironmentsWrongResultTest() throws JsonProcessingException {

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
