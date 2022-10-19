package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                "ARIK-LAPTOP[LOCAL]", "SampleInsights/Error", "OpenTelemetry.Instrumentation.AspNetCore", "", "dark", null);
        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051");

        String htmlBody = analyticsProvider.getHtmlGraphForSpanPercentiles(query);

        System.out.println("htmlBody:" + htmlBody);
    }

    @Test
    void getInsights() throws JsonProcessingException {

        String codeObjectId = "Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds";
        List<CodeObjectInsight> expectedCodeObjectInsights = new ArrayList<>();

        HotspotInsight expectedHotspotInsight = new HotspotInsight(codeObjectId, 75);
        expectedCodeObjectInsights.add(expectedHotspotInsight);

        ErrorInsightNamedError namedError1 = new ErrorInsightNamedError("e0a4d03c-c609-11ec-a9d6-0242ac130006", "System.NullReferenceException", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        ErrorInsightNamedError namedError2 = new ErrorInsightNamedError("de63a938-c609-11ec-b388-0242ac130006", "System.Exception", codeObjectId, "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        List<ErrorInsightNamedError> namedErrors = new ArrayList<>();
        namedErrors.add(namedError1);
        namedErrors.add(namedError2);
        ErrorInsight expectedErrorInsight = new ErrorInsight(codeObjectId, 1, 0, 0, namedErrors);
        expectedCodeObjectInsights.add(expectedErrorInsight);

        NormalUsageInsight expectedNormalUsageInsight = new NormalUsageInsight("Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds",
                "post transfer/transferfunds", 40);
        expectedCodeObjectInsights.add(expectedNormalUsageInsight);

        LowUsageInsight expectedLowUsageInsight = new LowUsageInsight("Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$Abc",
                "post transfer/transferfunds", 13);
        expectedCodeObjectInsights.add(expectedLowUsageInsight);

        HighUsageInsight expectedHighUsageInsight = new HighUsageInsight("Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$Defg",
                "post transfer/transferfunds", 98);
        expectedCodeObjectInsights.add(expectedHighUsageInsight);

        SpanInfo spanInfo = new SpanInfo("Retrieving account", "Retrieving account", "MoneyTransferDomainService", "Sample.MoneyTransfer.API","Sample.MoneyTransfer.API.MoneyTransferDomainService$_$Error");
        SlowSpanInfo slowSpanInfo = new SlowSpanInfo(spanInfo,
                new Percentile(0.10970134022722634D,new Duration(3.44D,"ms",3441700L)),
                new Percentile(0.2566821090980162D,new Duration(3.44D,"ms",3441700L)),
                new Percentile(0.4407383382867023D,new Duration(5.64D,"ms",5643900L)));

        SlowestSpansInsight expectedSlowestSpansInsight = new SlowestSpansInsight("Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds",
                "post transfer/transferfunds",Collections.singletonList(slowSpanInfo));
        expectedCodeObjectInsights.add(expectedSlowestSpansInsight);

        SlowEndpointInsight expectedSlowEndpointInsight = new SlowEndpointInsight("Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService$_$TransferFunds", "post transfer/transferfunds"
                , new Duration(0.11D, "ms", 11000)
                , new Duration(0.12D, "ms", 12000)
                , new Duration(0.13D, "ms", 13000)
                , new Duration(0.14D, "ms", 14000)
                , new Duration(0.15D, "ms", 15000)
                , new Duration(0.16D, "ms", 16000)
                , new Duration(0.17D, "ms", 17000)
                , new Duration(0.18D, "ms", 18000)
                , new Duration(0.19D, "ms", 19000)
                , new Duration(0.21D, "ms", 21000)
                , new Duration(0.22D, "ms", 22000)
        );
        expectedCodeObjectInsights.add(expectedSlowEndpointInsight);


        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedCodeObjectInsights))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:" + codeObjectId)));

        assertEquals(7, codeObjectInsights.size(), "count of returned insights");

        assertEquals(HotspotInsight.class, codeObjectInsights.get(0).getClass());
        HotspotInsight hotspotInsight = (HotspotInsight) codeObjectInsights.get(0);
        assertEquals(expectedHotspotInsight, hotspotInsight);

        assertEquals(ErrorInsight.class, codeObjectInsights.get(1).getClass());
        ErrorInsight errorInsight = (ErrorInsight) codeObjectInsights.get(1);
        assertEquals(expectedErrorInsight, errorInsight);

        assertEquals(NormalUsageInsight.class, codeObjectInsights.get(2).getClass());
        NormalUsageInsight normalUsageInsight = (NormalUsageInsight) codeObjectInsights.get(2);
        assertEquals(expectedNormalUsageInsight, normalUsageInsight);

        assertEquals(LowUsageInsight.class, codeObjectInsights.get(3).getClass());
        LowUsageInsight lowUsageInsight = (LowUsageInsight) codeObjectInsights.get(3);
        assertEquals(expectedLowUsageInsight, lowUsageInsight);

        assertEquals(HighUsageInsight.class, codeObjectInsights.get(4).getClass());
        HighUsageInsight highUsageInsight = (HighUsageInsight) codeObjectInsights.get(4);
        assertEquals(expectedHighUsageInsight, highUsageInsight);

        assertEquals(SlowestSpansInsight.class, codeObjectInsights.get(5).getClass());
        SlowestSpansInsight slowestSpansInsight = (SlowestSpansInsight) codeObjectInsights.get(5);
        assertEquals(expectedSlowestSpansInsight, slowestSpansInsight);

        assertEquals(SlowEndpointInsight.class, codeObjectInsights.get(6).getClass());
        SlowEndpointInsight slowEndpointInsight = (SlowEndpointInsight) codeObjectInsights.get(6);
        assertEquals(expectedSlowEndpointInsight, slowEndpointInsight);

    }


    @Test
    void getInsightsEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));

        assertEquals(0, codeObjectInsights.size(), "unexpected summaries size");
    }

    @Test
    void getSummariesNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    @Test
    void getSummariesErrorResultTest() {

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
    void getSummariesWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("mystring"))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
            analyticsProvider.getInsights(new InsightsRequest("myenv", Collections.singletonList("method:aaaa")));
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }


}
