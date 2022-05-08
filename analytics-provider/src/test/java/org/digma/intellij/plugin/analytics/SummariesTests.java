package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ProcessingException;
import okhttp3.mockwebserver.MockResponse;
import org.digma.intellij.plugin.model.CodeObjectSummaryType;
import org.digma.intellij.plugin.model.rest.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SummariesTests extends AbstractAnalyticsProviderTest {


//    @Test
//    public void getSummariesTemp(){
//        List<String> ids = new ArrayList<>();
//        ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds");
//        ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$DepositFunds");
//        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("UNSET_ENV", ids);
//        AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("http://localhost:5051");
//        List<CodeObjectSummary> summaries = analyticsProvider.getSummaries(summaryRequest);
//        System.out.println(summaries);
//    }


    @Test
    public void getSummaries() throws JsonProcessingException {

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
        assertEquals(methodCodeObjectSummary.getType(), CodeObjectSummaryType.MethodSummary);
        assertTrue(expectedCodeObjectSummary.equals(methodCodeObjectSummary));
    }

    @Test
    public void getMultipleSummaries() throws JsonProcessingException {

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
        assertEquals(methodCodeObjectSummary.getType(), CodeObjectSummaryType.MethodSummary);
        assertTrue(expectedMethodCodeObjectSummary.equals(methodCodeObjectSummary));

        SpanCodeObjectSummary spanCodeObjectSummary = (SpanCodeObjectSummary) summariesResult.get(1);
        assertEquals(spanCodeObjectSummary.getType(), CodeObjectSummaryType.SpanSummary);
        assertTrue(spanCodeObjectSummary.equals(expectedSpanCodeObjectSummary));
    }


    @Test
    public void getSummariesEmptyResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.emptyList()))
                .addHeader("Content-Type", "application/json"));

        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectSummary> summariesResult = restAnalyticsProvider.getSummaries(summaryRequest);

        assertEquals(0, summariesResult.size(), "unexpected summaries size");
    }

    @Test
    public void getSummariesNullResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json"));

        CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
        List<CodeObjectSummary> summariesResult = restAnalyticsProvider.getSummaries(summaryRequest);

        assertNull(summariesResult, "summaries result should be null");
    }

    @Test
    public void getSummariesErrorResultTest() {

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        InternalServerErrorException exception = assertThrows(InternalServerErrorException.class, () -> {
            CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getSummaries(summaryRequest);
        });

        assertEquals(500, exception.getResponse().getStatus());
    }


    @Test
    public void getSummariesWrongResultTest() throws JsonProcessingException {

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString("mystring"))
                .addHeader("Content-Type", "application/json"));

        ProcessingException exception = assertThrows(ProcessingException.class, () -> {
            CodeObjectSummaryRequest summaryRequest = new CodeObjectSummaryRequest("myenv", Collections.singletonList("nonexistingid"));
            AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(baseUrl);
            restAnalyticsProvider.getSummaries(summaryRequest);
        });

        assertTrue(exception.getMessage().contains("com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize value of type `java.util.ArrayList<org.digma.intellij.plugin.model.rest.CodeObjectSummary>` from String value (token `JsonToken.VALUE_STRING`)"));
    }


}
