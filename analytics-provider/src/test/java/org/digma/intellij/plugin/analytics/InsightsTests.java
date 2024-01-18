package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.gson.Gson;
import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.Duration;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError;
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight;
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight;
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects;
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsights;
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight;
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight;
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo;
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.insights.SpanInfo;
import org.digma.intellij.plugin.model.rest.version.VersionRequest;
import org.digma.intellij.plugin.model.rest.version.VersionResponse;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("resource")
class InsightsTests extends AbstractAnalyticsProviderTest {


    //run against running env just for local test
//    @Test
    public void actualGetInsightsTemp() {
        {
            List<String> ids = new ArrayList<>();
            ids.add("method:Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds");
            ids.add("span:MoneyTransferDomainService$_$Peristing balance transfer");
            ids.add("span:MoneyTransferDomainService$_$Creating record of transaction");
            InsightsRequest insightsRequest = new InsightsRequest("UNSET_ENV", ids);
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("http://localhost:5051");
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(insightsRequest);

            System.out.println(codeObjectInsights);
        }
    }


    //    @Test
    public void actualGetInsightsTempViaSsl() {
        {
            List<String> ids = new ArrayList<>();
            ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
            ids.add("span:TransferController$_$Process transfer");
            InsightsRequest insightsRequest = new InsightsRequest("UNSET_ENV", ids);
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051");
            List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(insightsRequest);

            System.out.println(codeObjectInsights);
        }
    }

    //@Test
    public void actualgetHtmlGraphForSpanPercentiles() {
        final SpanHistogramQuery query = new SpanHistogramQuery(
                "ARIK-LAPTOP[LOCAL]", "SampleInsights/Error", "OpenTelemetry.Instrumentation.AspNetCore", "dark", null);
        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051");

        String htmlBody = analyticsProvider.getHtmlGraphForSpanPercentiles(query);

        System.out.println("htmlBody:" + htmlBody);
    }

    //    @Test
    public void actualGetCodeObjectInsightStatus() {
        final InsightsOfMethodsRequest request = new InsightsOfMethodsRequest(
                "ARIKS-MACBOOK-PRO.LOCAL[LOCAL]",
                List.of(
                        new MethodWithCodeObjects("method:org.springframework.samples.petclinic.sample.SampleInsightsController$_$doWorkForBottleneck2", List.of(), List.of())
                )
        );
        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051");


        CodeObjectInsightsStatusResponse response = analyticsProvider.getCodeObjectInsightStatus(request);

        System.out.println("response:" + response);
    }

//    @Test
    public void actual_getVersions() {
        final VersionRequest request = new VersionRequest(
                "1.2.3",
                "IC",
                "4.5.6"
        );
        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051");

        VersionResponse response = analyticsProvider.getVersions(request);

        System.out.println("response:" + response);
    }

    @Test
    void getInsights() throws JsonProcessingException {

        final String ROUTE = "post transfer/transferfunds";
        final String SERVICE = "MyService";
        final String ENDPOINT_SPAN = "HTTP POST transfer/transferfunds";
        final String ENV_1 = "Env1";
        final String SCOPE_1 = "Scope1";
        final int IMPORTANCE_3 = 3;

        String codeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        String prefixedCodeObjectId = addPrefixToCodeObjectId(codeObjectId);
        Date actualStartTimeNow = new Date();
        Date customStartTimeFiveDaysBefore = Date.from(actualStartTimeNow.toInstant().minus(5, ChronoUnit.DAYS));
        List<CodeObjectInsight> expectedCodeObjectInsights = new ArrayList<>();

        HotspotInsight expectedHotspotInsight = new HotspotInsight(codeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null, actualStartTimeNow, customStartTimeFiveDaysBefore, prefixedCodeObjectId, false,null, 75,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedHotspotInsight);

        ErrorInsightNamedError namedError1 = new ErrorInsightNamedError("e0a4d03c-c609-11ec-a9d6-0242ac130006", "System.NullReferenceException", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        ErrorInsightNamedError namedError2 = new ErrorInsightNamedError("de63a938-c609-11ec-b388-0242ac130006", "System.Exception", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        List<ErrorInsightNamedError> namedErrors = new ArrayList<>();
        namedErrors.add(namedError1);
        namedErrors.add(namedError2);
        ErrorInsight expectedErrorInsight = new ErrorInsight(codeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null, actualStartTimeNow, customStartTimeFiveDaysBefore, prefixedCodeObjectId, false, null, 1, 0, 0, namedErrors,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedErrorInsight);

        String expectedNormalUsageInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        NormalUsageInsight expectedNormalUsageInsight = new NormalUsageInsight(expectedNormalUsageInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedNormalUsageInsightCodeObjectId), false, null,
                createSpanInfo(ENDPOINT_SPAN, expectedNormalUsageInsightCodeObjectId), ROUTE, SERVICE,
                40,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedNormalUsageInsight);

        String expectedLowUsageInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$Abc";
        LowUsageInsight expectedLowUsageInsight = new LowUsageInsight(expectedLowUsageInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedLowUsageInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedLowUsageInsightCodeObjectId), ROUTE, SERVICE,
                13,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedLowUsageInsight);

        String expectedHighUsageInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$Defg";
        HighUsageInsight expectedHighUsageInsight = new HighUsageInsight(expectedHighUsageInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedHighUsageInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedHighUsageInsightCodeObjectId), ROUTE, SERVICE,
                98,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedHighUsageInsight);

        SlowSpanInfo slowSpanInfo = new SlowSpanInfo(
                createSpanInfo("SomeSpan", "Sample.MoneyTransfer.API.MoneyTransferDomainService$_$Error"),
                0.10970134022722634, new Duration(3.44D, "ms", 3441700L), 0, null);

        String expectedSlowestSpansInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        SlowestSpansInsight expectedSlowestSpansInsight = new SlowestSpansInsight(expectedSlowestSpansInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedSlowestSpansInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedSlowestSpansInsightCodeObjectId), ROUTE, SERVICE,
                Collections.singletonList(slowSpanInfo),0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedSlowestSpansInsight);

        String expectedSlowEndpointInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        SlowEndpointInsight expectedSlowEndpointInsight = new SlowEndpointInsight(
                expectedSlowEndpointInsightCodeObjectId
                , ENV_1
                , SCOPE_1
                , IMPORTANCE_3
                , null
                , actualStartTimeNow
                , customStartTimeFiveDaysBefore
                , addPrefixToCodeObjectId(expectedSlowEndpointInsightCodeObjectId)
                , false
                , null
                , createSpanInfo(ENDPOINT_SPAN, expectedSlowEndpointInsightCodeObjectId)
                , ROUTE
                , SERVICE
                , new Duration(0.11D, "ms", 11000)
                , new Duration(0.12D, "ms", 12000)
                , new Duration(0.14D, "ms", 14000)
                , new Duration(0.18D, "ms", 18000)
                ,0,0,0, null, null, null, 0, null
        );
        expectedCodeObjectInsights.add(expectedSlowEndpointInsight);


        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedCodeObjectInsights))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList(addPrefixToCodeObjectId(codeObjectId))));

        assertEquals(7, codeObjectInsights.size(), "count of returned insights");

        assertEquals(HotspotInsight.class, codeObjectInsights.get(0).getClass());
        HotspotInsight hotspotInsight = (HotspotInsight) codeObjectInsights.get(0);
        AssertObjectsAreEqual(expectedHotspotInsight, hotspotInsight);

        assertEquals(ErrorInsight.class, codeObjectInsights.get(1).getClass());
        ErrorInsight errorInsight = (ErrorInsight) codeObjectInsights.get(1);
        AssertObjectsAreEqual(expectedErrorInsight, errorInsight);

        assertEquals(NormalUsageInsight.class, codeObjectInsights.get(2).getClass());
        NormalUsageInsight normalUsageInsight = (NormalUsageInsight) codeObjectInsights.get(2);
        AssertObjectsAreEqual(expectedNormalUsageInsight, normalUsageInsight);

        assertEquals(LowUsageInsight.class, codeObjectInsights.get(3).getClass());
        LowUsageInsight lowUsageInsight = (LowUsageInsight) codeObjectInsights.get(3);
        AssertObjectsAreEqual(expectedLowUsageInsight, lowUsageInsight);

        assertEquals(HighUsageInsight.class, codeObjectInsights.get(4).getClass());
        HighUsageInsight highUsageInsight = (HighUsageInsight) codeObjectInsights.get(4);
        AssertObjectsAreEqual(expectedHighUsageInsight, highUsageInsight);

        assertEquals(SlowestSpansInsight.class, codeObjectInsights.get(5).getClass());
        SlowestSpansInsight slowestSpansInsight = (SlowestSpansInsight) codeObjectInsights.get(5);
        AssertObjectsAreEqual(expectedSlowestSpansInsight, slowestSpansInsight);

        assertEquals(SlowEndpointInsight.class, codeObjectInsights.get(6).getClass());
        SlowEndpointInsight slowEndpointInsight = (SlowEndpointInsight) codeObjectInsights.get(6);
        AssertObjectsAreEqual(expectedSlowEndpointInsight, slowEndpointInsight);
    }

    private void AssertObjectsAreEqual(Object expected, Object actual) {
        String actualJson = new Gson().toJson(actual);
        String expectedJson = new Gson().toJson(expected);
        assertEquals(expectedJson, actualJson);
    }

    private SpanInfo createSpanInfo(String spanName, String methodCodeObjectId) {
        String instLib = "il";
        return new SpanInfo(instLib, spanName, "span:$instLib$_$" + spanName, "disp_" + spanName,
                methodCodeObjectId, "Internal");
    }

    @Test
    void getInsightsEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));

        assertEquals(0, codeObjectInsights.size(), "unexpected codeObjectInsights size");
    }

    @Test
    void getInsightsForMethodEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(null))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);

        List<MethodWithCodeObjects> methods = new ArrayList<>();
        methods.add(new MethodWithCodeObjects("1", Collections.singletonList("22"), Collections.singletonList("34")));

        InsightsOfMethodsResponse response = analyticsProvider.getInsightsOfMethods(
                new InsightsOfMethodsRequest("myenv", methods));

        assertNull(response);
    }

    @Test
    void getInsightsOfMethods() throws JsonProcessingException {
        final String ROUTE = "post transfer/transferfunds";
        final String SERVICE = "MyService";
        final String ENDPOINT_SPAN = "HTTP POST transfer/transferfunds";
        final String ENV_1 = "Env1";
        final String SCOPE_1 = "Scope1";
        final int IMPORTANCE_3 = 3;

        String codeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        String prefixedCodeObjectId = addPrefixToCodeObjectId(codeObjectId);
        Date actualStartTimeNow = new Date();
        Date customStartTimeFiveDaysBefore = Date.from(actualStartTimeNow.toInstant().minus(5, ChronoUnit.DAYS));
        List<CodeObjectInsight> expectedCodeObjectInsights = new ArrayList<>();

        HotspotInsight expectedHotspotInsight = new HotspotInsight(codeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null, actualStartTimeNow, customStartTimeFiveDaysBefore, prefixedCodeObjectId, false,null, 75,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedHotspotInsight);

        ErrorInsightNamedError namedError1 = new ErrorInsightNamedError("e0a4d03c-c609-11ec-a9d6-0242ac130006", "System.NullReferenceException", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        ErrorInsightNamedError namedError2 = new ErrorInsightNamedError("de63a938-c609-11ec-b388-0242ac130006", "System.Exception", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        List<ErrorInsightNamedError> namedErrors = new ArrayList<>();
        namedErrors.add(namedError1);
        namedErrors.add(namedError2);
        ErrorInsight expectedErrorInsight = new ErrorInsight(codeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null, actualStartTimeNow, customStartTimeFiveDaysBefore, prefixedCodeObjectId, false,null, 1, 0, 0, namedErrors,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedErrorInsight);

        String expectedNormalUsageInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        NormalUsageInsight expectedNormalUsageInsight = new NormalUsageInsight(expectedNormalUsageInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedNormalUsageInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedNormalUsageInsightCodeObjectId), ROUTE, SERVICE,
                40,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedNormalUsageInsight);

        String expectedLowUsageInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$Abc";
        LowUsageInsight expectedLowUsageInsight = new LowUsageInsight(expectedLowUsageInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedLowUsageInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedLowUsageInsightCodeObjectId), ROUTE, SERVICE,
                13,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedLowUsageInsight);

        String expectedHighUsageInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$Defg";
        HighUsageInsight expectedHighUsageInsight = new HighUsageInsight(expectedHighUsageInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedHighUsageInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedHighUsageInsightCodeObjectId), ROUTE, SERVICE,
                98,0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedHighUsageInsight);

        SlowSpanInfo slowSpanInfo = new SlowSpanInfo(
                createSpanInfo("SomeSpan", "Sample.MoneyTransfer.API.MoneyTransferDomainService$_$Error"),
                0.10970134022722634, new Duration(3.44D, "ms", 3441700L), 0, null);

        String expectedSlowestSpansInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        SlowestSpansInsight expectedSlowestSpansInsight = new SlowestSpansInsight(expectedSlowestSpansInsightCodeObjectId, ENV_1, SCOPE_1, IMPORTANCE_3, null,
                actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(expectedSlowestSpansInsightCodeObjectId), false,null,
                createSpanInfo(ENDPOINT_SPAN, expectedSlowestSpansInsightCodeObjectId), ROUTE, SERVICE,
                Collections.singletonList(slowSpanInfo),0,0,0, null, null, null, 0, null);
        expectedCodeObjectInsights.add(expectedSlowestSpansInsight);

        String expectedSlowEndpointInsightCodeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        SlowEndpointInsight expectedSlowEndpointInsight = new SlowEndpointInsight(
                expectedSlowEndpointInsightCodeObjectId
                , ENV_1
                , SCOPE_1
                , IMPORTANCE_3
                , null
                , actualStartTimeNow
                , customStartTimeFiveDaysBefore
                , addPrefixToCodeObjectId(expectedSlowEndpointInsightCodeObjectId)
                ,false
                , null
                , createSpanInfo(ENDPOINT_SPAN, expectedSlowEndpointInsightCodeObjectId)
                , ROUTE
                , SERVICE
                , new Duration(0.11D, "ms", 11000)
                , new Duration(0.12D, "ms", 12000)
                , new Duration(0.14D, "ms", 14000)
                , new Duration(0.18D, "ms", 18000)
                ,0,0,0, null, null, null, 0, null
        );
        expectedCodeObjectInsights.add(expectedSlowEndpointInsight);


        List<String> expectedRelatedSpansCodeObjectIds = new ArrayList<>();
        expectedRelatedSpansCodeObjectIds.add(codeObjectId);

        MethodWithCodeObjects expectedMethodWithCodeObjects = new MethodWithCodeObjects(addPrefixToCodeObjectId(codeObjectId), expectedRelatedSpansCodeObjectIds, Collections.emptyList());
        MethodWithInsights expectedMethodWithInsights = new MethodWithInsights(expectedMethodWithCodeObjects, expectedCodeObjectInsights);

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(
                        new InsightsOfMethodsResponse(ENV_1, Collections.singletonList(expectedMethodWithInsights))
                ))
                .addHeader("Content-Type", "application/json"));

        List<MethodWithCodeObjects> methodWithCodeObjects = new ArrayList<>();
        List<String> relatedSpansCodeObjectIds = new ArrayList<>();
        relatedSpansCodeObjectIds.add(codeObjectId);

        methodWithCodeObjects.add(new MethodWithCodeObjects(addPrefixToCodeObjectId(codeObjectId), relatedSpansCodeObjectIds, Collections.emptyList()));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        InsightsOfMethodsResponse insightsOfMethodsResponse = analyticsProvider.getInsightsOfMethods(
                new InsightsOfMethodsRequest(ENV_1, methodWithCodeObjects));
        List<MethodWithInsights> actualMethodsWithInsights = insightsOfMethodsResponse.getMethodsWithInsights();
        List<CodeObjectInsight> actualCodeObjectInsights = actualMethodsWithInsights.stream()
                .flatMap(methodWithInsights -> methodWithInsights.getInsights().stream())
                .toList();

        assertEquals(ENV_1, insightsOfMethodsResponse.getEnvironment(), "check if environment is correct");
        assertEquals(1, actualMethodsWithInsights.size(), "count of returned methods with insights");
        assertEquals(7, actualCodeObjectInsights.size(), "count of returned insights");

        assertEquals(HotspotInsight.class, actualCodeObjectInsights.get(0).getClass());
        HotspotInsight hotspotInsight = (HotspotInsight) actualCodeObjectInsights.get(0);
        AssertObjectsAreEqual(expectedHotspotInsight, hotspotInsight);

        assertEquals(ErrorInsight.class, actualCodeObjectInsights.get(1).getClass());
        ErrorInsight errorInsight = (ErrorInsight) actualCodeObjectInsights.get(1);
        AssertObjectsAreEqual(expectedErrorInsight, errorInsight);

        assertEquals(NormalUsageInsight.class, actualCodeObjectInsights.get(2).getClass());
        NormalUsageInsight normalUsageInsight = (NormalUsageInsight) actualCodeObjectInsights.get(2);
        AssertObjectsAreEqual(expectedNormalUsageInsight, normalUsageInsight);

        assertEquals(LowUsageInsight.class, actualCodeObjectInsights.get(3).getClass());
        LowUsageInsight lowUsageInsight = (LowUsageInsight) actualCodeObjectInsights.get(3);
        AssertObjectsAreEqual(expectedLowUsageInsight, lowUsageInsight);

        assertEquals(HighUsageInsight.class, actualCodeObjectInsights.get(4).getClass());
        HighUsageInsight highUsageInsight = (HighUsageInsight) actualCodeObjectInsights.get(4);
        AssertObjectsAreEqual(expectedHighUsageInsight, highUsageInsight);

        assertEquals(SlowestSpansInsight.class, actualCodeObjectInsights.get(5).getClass());
        SlowestSpansInsight slowestSpansInsight = (SlowestSpansInsight) actualCodeObjectInsights.get(5);
        AssertObjectsAreEqual(expectedSlowestSpansInsight, slowestSpansInsight);

        assertEquals(SlowEndpointInsight.class, actualCodeObjectInsights.get(6).getClass());
        SlowEndpointInsight slowEndpointInsight = (SlowEndpointInsight) actualCodeObjectInsights.get(6);
        AssertObjectsAreEqual(expectedSlowEndpointInsight, slowEndpointInsight);
    }

    @Test
    void getInsightsNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    @Test
    void getInsightsErrorResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(500, exception.getResponseCode());
    }


    @Test
    void getInsightsWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("mystring"))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    private String addPrefixToCodeObjectId(String codeObjectId) {
        return "method:" + codeObjectId;
    }

}
