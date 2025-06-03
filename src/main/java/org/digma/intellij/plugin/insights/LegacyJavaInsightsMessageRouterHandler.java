package org.digma.intellij.plugin.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.insights.issues.GetIssuesRequestPayload;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.insights.*;
import org.digma.intellij.plugin.ui.insights.model.*;
import org.digma.intellij.plugin.vcs.VcsService;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.digma.intellij.plugin.ui.jcef.JCEFUtilsKt.getQueryMapFromPayload;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;

//todo: convert to kotlin move all code to org.digma.intellij.plugin.ui.insights.InsightsMessageRouterHandler.
// if any method needs to call suspending code it must be moved to InsightsMessageRouterHandler.
// it is currently a strange class that uses InsightsMessageRouterHandler.
public class LegacyJavaInsightsMessageRouterHandler {

    private final Logger LOGGER = Logger.getInstance(getClass());
    private final Project project;
    private final InsightsMessageRouterHandler myMessageRouterHandler;


    public LegacyJavaInsightsMessageRouterHandler(Project project, InsightsMessageRouterHandler myMessageRouterHandler) {
        this.project = project;
        this.myMessageRouterHandler = myMessageRouterHandler;
    }

    public boolean doOnQueryJavaLegacy(@NotNull CefBrowser browser, @NotNull JsonNode requestJsonNode, @NotNull String action) throws Exception {

        switch (action) {

            case "INSIGHTS/OPEN_LIVE_VIEW" -> openLiveView(requestJsonNode);

            case "INSIGHTS/OPEN_HISTOGRAM" -> openHistogram(requestJsonNode);

            case "INSIGHTS/RECALCULATE" -> recalculate(browser, requestJsonNode);

            case "INSIGHTS/GO_TO_TRACE" -> goToTrace(requestJsonNode);

            case "INSIGHTS/GO_TO_TRACE_COMPARISON" -> goToTraceComparison(requestJsonNode);

            case "INSIGHTS/LINK_TICKET" -> linkTicket(browser, requestJsonNode);

            case "INSIGHTS/UNLINK_TICKET" -> unlinkTicket(browser, requestJsonNode);

            case "INSIGHTS/GET_SPAN_INSIGHT" -> getInsight(browser, requestJsonNode);

            case "INSIGHTS/GET_COMMIT_INFO" -> getCommitInfo(browser, requestJsonNode);

            case "INSIGHTS/GET_DATA_LIST" -> pushInsightsListData(requestJsonNode);

            case "INSIGHTS/DISMISS" -> dismissInsight(requestJsonNode);

            case "INSIGHTS/UNDISMISS" -> undismissInsight(requestJsonNode);

            case "ISSUES/GET_DATA_LIST" -> pushIssuesListData(requestJsonNode);

            case "ISSUES/GET_FILTERS" -> pushIssuesFiltersData(requestJsonNode);

            default -> {
                return false;
            }
        }

        return true;
    }

    private void pushInsightsListData(JsonNode jsonNode) {
        Log.log(LOGGER::debug, project, "got INSIGHTS/GET_DATA_LIST message");
        Map<String, Object> backendQueryParams = getQueryMapFromPayload(jsonNode, myMessageRouterHandler.getObjectMapper());
        InsightsService.getInstance(project).refreshInsightsList(backendQueryParams);
    }

    private void pushIssuesListData(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got ISSUES/GET_DATA_LIST message");
        var payload = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString());
        var query = payload.get("query");
        var request = new GetIssuesRequestPayload(
                query.at("/environment").asText(""),
                query.at("/scopedSpanCodeObjectId").asText(""),
                query.at("/displayName").asText(null),
                query.at("/showDismissed").asBoolean(false),
                query.at("/filters").toString(),
                query.at("/sorting/criterion").asText(null),
                query.at("/sorting/order").asText(null),
                query.at("/insightTypes").toString(),
                query.at("/services").toString(),
                query.at("/criticalityFilter").toString(),
                query.at("/page").asInt(0));

        InsightsService.getInstance(project).refreshIssuesList(request);
    }

    private void pushIssuesFiltersData(JsonNode jsonNode) {
        Log.log(LOGGER::debug, project, "got ISSUES/GET_FILTERS message");
        Map<String, Object> backendQueryParams = getQueryMapFromPayload(jsonNode, myMessageRouterHandler.getObjectMapper());
        InsightsService.getInstance(project).refreshIssuesFilters(backendQueryParams);
    }

    private void dismissInsight(JsonNode jsonNode) {
        Log.log(LOGGER::debug, project, "got INSIGHTS/DISMISS message");
        var insightId = myMessageRouterHandler.getPayloadFromRequestNonNull(jsonNode).get("insightId").asText();
        InsightsService.getInstance(project).dismissInsight(insightId);
    }

    private void undismissInsight(JsonNode jsonNode) {
        Log.log(LOGGER::debug, project, "got INSIGHTS/UNDISMISS message");
        var insightId = myMessageRouterHandler.getPayloadFromRequestNonNull(jsonNode).get("insightId").asText();
        InsightsService.getInstance(project).undismissInsight(insightId);
    }

    private void getCommitInfo(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {

        var commits = (ArrayNode) myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString()).get("commits");

        if (commits == null || commits.isEmpty()) return;

        var commitInfos = new HashMap<String, CommitInfo>();
        commits.forEach(commit -> {
            var commitStr = commit.asText();
            var url = VcsService.getInstance(project).buildRemoteLinkToCommit(commitStr);
            if (url != null) {
                commitInfos.put(commitStr, new CommitInfo(commitStr, url));
            }
        });

        var message = new SetCommitInfoMessage("digma", "INSIGHTS/SET_COMMIT_INFO", new SetCommitInfoData(commitInfos));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);

    }

    private void linkTicket(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/LINK_TICKET message");
        var payload = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString());
        var insightId = payload.get("insightId").asText();
        var insightType = payload.get("insightType").asText();
        var ticketLink = payload.get("ticketLink").asText();
        var linkTicketResponse = AnalyticsService.getInstance(project).linkTicket(insightId, ticketLink);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", linkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        ActivityMonitor.getInstance(project).registerUserAction("link ticket", Map.of("insight", insightType));
    }

    private void unlinkTicket(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/UNLINK_TICKET message");
        var payload = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString());
        var insightId = payload.get("insightId").asText();
        var insightType = payload.get("insightType").asText();
        var unlinkTicketResponse = AnalyticsService.getInstance(project).unlinkTicket(insightId);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", unlinkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        ActivityMonitor.getInstance(project).registerUserAction("unlink ticket", Map.of("insight", insightType));
    }

    @Nullable
    private String getInsightBySpan(String spanCodeObjectId, String insightType) throws AnalyticsServiceException {
        try {
            return AnalyticsService.getInstance(project).getInsightBySpan(spanCodeObjectId, insightType);
        } catch (AnalyticsServiceException e) {
            return null;
        }
    }

    private void getInsight(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/GET_SPAN_INSIGHT message");
        JsonNode payload = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        JsonNode insight = null;
        try {
            var insightsString = getInsightBySpan(spanCodeObjectId, insightType);
            if (insightsString != null) {
                insight = myMessageRouterHandler.getObjectMapper().readTree(insightsString);
            }
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(LOGGER, project, e, "Error getInsight: {}", e.getMessage());
        }

        var message = new SetSpanInsightMessage("digma", "INSIGHTS/SET_SPAN_INSIGHT", new SetSpanInsightData(insight));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
    }


    private void openHistogram(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_HISTOGRAM message");
        var payload = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        var displayName = payload.has("displayName") ? payload.get("displayName").asText() : null;
        org.digma.intellij.plugin.ui.insights.InsightsService.getInstance(project).openHistogram(spanCodeObjectId, insightType, displayName);
    }

    private void openLiveView(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_LIVE_VIEW message");
        var codeObjectId = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString()).get("codeObjectId").asText();
        Log.log(LOGGER::debug, project, "got codeObjectId {}", codeObjectId);
        InsightsService.getInstance(project).openLiveView(codeObjectId);
    }

    private void recalculate(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {
        var insightId = myMessageRouterHandler.getObjectMapper().readTree(jsonNode.get("payload").toString()).get("id").asText();
        InsightsService.getInstance(project).recalculate(insightId);

        var message = new SetInsightRecalculatedMessage(new SetInsightRecalculated(insightId));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
    }

    private void goToTrace(JsonNode jsonNode) {
        var payload = myMessageRouterHandler.getPayloadFromRequest(jsonNode);
        if (payload != null) {
            var traceId = payload.get("trace").get("id").asText();
            var traceName = payload.get("trace").get("name").asText();
            var insightType = payload.get("insightType").asText();
            String spanCodeObjectId = payload.has("spanCodeObjectId") ? payload.get("spanCodeObjectId").asText() : null;
            InsightsService.getInstance(project).goToTrace(traceId, traceName, insightType, spanCodeObjectId);
        }
    }

    private void goToTraceComparison(JsonNode jsonNode) {
        var payload = myMessageRouterHandler.getPayloadFromRequest(jsonNode);
        if (payload != null) {
            var traces = payload.get("traces");
            var traceId1 = traces.get(0).get("id").asText();
            var traceName1 = traces.get(0).get("name").asText();
            var traceId2 = traces.get(1).get("id").asText();
            var traceName2 = traces.get(1).get("name").asText();
            var insightType = payload.get("insightType").asText();
            InsightsService.getInstance(project).goToTraceComparison(traceId1, traceName1, traceId2, traceName2, insightType);
        }
    }
}

