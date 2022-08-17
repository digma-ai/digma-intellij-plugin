package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.CodeObjectSummaryType;
import org.digma.intellij.plugin.model.rest.summary.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("resource")
class SummariesTests extends AbstractAnalyticsProviderTest {


    //run against running env just for local test
//    @Test
    public void getSummariesTemp() {
        List<String> ids = new ArrayList<>();
        ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
        ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$DepositFunds");
        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("UNSET_ENV", ids);
        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051"); // HTTPS => SSL
        List<CodeObjectSummary> summaries = analyticsProvider.getSummaries(summaryRequest);
        System.out.println(summaries);
    }


    @Test
    void getSummaries() throws JsonProcessingException {

        String codeObjectId = "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds";
        List<ExecutedCodeSummary> executedCodeSummaries = new ArrayList<>();
        executedCodeSummaries.add(new ExecutedCodeSummary("", "System.Exception", "Insufficient funds", false, false, 48));
        executedCodeSummaries.add(new ExecutedCodeSummary("", "System.NullReferenceException", "Object reference not set to an instance of an object.", false, false, 48));
        MethodCodeObjectSummary expectedCodeObjectSummary = new MethodCodeObjectSummary(codeObjectId, 1, 1, 20, executedCodeSummaries);

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.singletonList(expectedCodeObjectSummary)))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("method:" + codeObjectId));
        List<CodeObjectSummary> summariesResult = analyticsProvider.getSummaries(summaryRequest);

        assertEquals(1, summariesResult.size());
        assertEquals(MethodCodeObjectSummary.class, summariesResult.get(0).getClass());
        MethodCodeObjectSummary methodCodeObjectSummary = (MethodCodeObjectSummary) summariesResult.get(0);
        assertEquals(CodeObjectSummaryType.MethodSummary, methodCodeObjectSummary.getType());
        assertEquals(expectedCodeObjectSummary, methodCodeObjectSummary);
    }

    @Test
    void getMultipleSummaries() throws JsonProcessingException {

        String codeObjectId = "Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds";
        List<ExecutedCodeSummary> executedCodeSummaries = new ArrayList<>();
        executedCodeSummaries.add(new ExecutedCodeSummary("", "System.Exception", "Insufficient funds", false, false, 48));
        executedCodeSummaries.add(new ExecutedCodeSummary("", "System.NullReferenceException", "Object reference not set to an instance of an object.", false, false, 48));
        MethodCodeObjectSummary expectedMethodCodeObjectSummary = new MethodCodeObjectSummary(codeObjectId, 1, 1, 20, executedCodeSummaries);
        SpanCodeObjectSummary expectedSpanCodeObjectSummary = new SpanCodeObjectSummary(codeObjectId, 1, 1);

        List<CodeObjectSummary> expectedSummaries = new ArrayList<>();
        expectedSummaries.add(expectedMethodCodeObjectSummary);
        expectedSummaries.add(expectedSpanCodeObjectSummary);

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedSummaries))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider(baseUrl);
        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("method:" + codeObjectId));
        List<CodeObjectSummary> summariesResult = analyticsProvider.getSummaries(summaryRequest);

        assertEquals(2, summariesResult.size());
        assertEquals(MethodCodeObjectSummary.class, summariesResult.get(0).getClass());
        assertEquals(SpanCodeObjectSummary.class, summariesResult.get(1).getClass());
        MethodCodeObjectSummary methodCodeObjectSummary = (MethodCodeObjectSummary) summariesResult.get(0);
        assertEquals(CodeObjectSummaryType.MethodSummary, methodCodeObjectSummary.getType());
        assertEquals(expectedMethodCodeObjectSummary, methodCodeObjectSummary);

        SpanCodeObjectSummary spanCodeObjectSummary = (SpanCodeObjectSummary) summariesResult.get(1);
        assertEquals(CodeObjectSummaryType.SpanSummary, spanCodeObjectSummary.getType());
        assertEquals(spanCodeObjectSummary, expectedSpanCodeObjectSummary);
    }


    @Test
    void getSummariesEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectSummary> summariesResult = restAnalyticsProvider.getSummaries(summaryRequest);

        assertEquals(0, summariesResult.size(), "unexpected summaries size");
    }

    @Test
    void getSummariesNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getSummaries(summaryRequest);
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }

    @Test
    void getSummariesErrorResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getSummaries(summaryRequest);
        });

        assertEquals(500, exception.getResponseCode());
    }


    @Test
    void getSummariesWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("mystring"))
                .addHeader("Content-Type", "application/json"));

        AnalyticsProviderException exception = assertThrows(AnalyticsProviderException.class, () -> {
            CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getSummaries(summaryRequest);
        });

        assertEquals(MismatchedInputException.class, exception.getCause().getClass());
    }


}
