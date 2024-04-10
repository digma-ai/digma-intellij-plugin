package org.digma.intellij.plugin.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.CodeObjectsUtil;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.insights.InsightsService;
import org.digma.intellij.plugin.ui.insights.model.*;
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler;
import org.digma.intellij.plugin.vcs.VcsService;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.ui.jcef.JCEFUtilsKt.getQueryMapFromPayload;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;

//todo: convert to kotlin move all code to org.digma.intellij.plugin.ui.insights.InsightsMessageRouterHandler
public abstract class AbstractInsightsMessageRouterHandler extends BaseCommonMessageRouterHandler {

    protected final Logger LOGGER = Logger.getInstance(getClass());


    public AbstractInsightsMessageRouterHandler(Project project) {
        super(project);
    }


    @Override
    public boolean doOnQuery(@NotNull Project project, @NotNull CefBrowser browser, @NotNull JsonNode requestJsonNode, @NotNull String rawRequest, @NotNull String action) throws Exception {

        switch (action) {

            case "INSIGHTS/GO_TO_ASSET" -> goToInsight(requestJsonNode);

            case "INSIGHTS/OPEN_LIVE_VIEW" -> openLiveView(requestJsonNode);

            case "INSIGHTS/OPEN_HISTOGRAM" -> openHistogram(requestJsonNode);

            case "INSIGHTS/RECALCULATE" -> recalculate(requestJsonNode);

            case "INSIGHTS/GO_TO_TRACE" -> goToTrace(requestJsonNode);

            case "INSIGHTS/GO_TO_TRACE_COMPARISON" -> goToTraceComparison(requestJsonNode);

            case "INSIGHTS/LINK_TICKET" -> linkTicket(browser, requestJsonNode);

            case "INSIGHTS/UNLINK_TICKET" -> unlinkTicket(browser, requestJsonNode);

            case "INSIGHTS/GET_CODE_LOCATIONS" -> getCodeLocations(browser, requestJsonNode);

            case "INSIGHTS/GET_SPAN_INSIGHT" -> getInsight(browser, requestJsonNode);

            case "INSIGHTS/GET_COMMIT_INFO" -> getCommitInfo(browser, requestJsonNode);

            case "INSIGHTS/GET_DATA_LIST" -> pushInsightsListData(requestJsonNode);

            case "INSIGHTS/DISMISS" -> dismissInsight(requestJsonNode);

            case "INSIGHTS/UNDISMISS" -> undismissInsight(requestJsonNode);

            default -> {
                return false;
            }
        }

        return true;
    }

    private void pushInsightsListData(JsonNode jsonNode) {
        Log.log(LOGGER::debug, getProject(), "got INSIGHTS/GET_DATA_LIST message");
        Map<String, Object> backendQueryParams = getQueryMapFromPayload(jsonNode, getObjectMapper());
        InsightsService.getInstance(getProject()).refreshInsightsList(backendQueryParams);
    }

    private void dismissInsight(JsonNode jsonNode) {
        Log.log(LOGGER::debug, getProject(), "got INSIGHTS/DISMISS message");
        var insightId = getPayloadFromRequestNonNull(jsonNode).get("insightId").asText();
        InsightsService.getInstance(getProject()).dismissInsight(insightId);
    }

    private void undismissInsight(JsonNode jsonNode) {
        Log.log(LOGGER::debug, getProject(), "got INSIGHTS/UNDISMISS message");
        var insightId = getPayloadFromRequestNonNull(jsonNode).get("insightId").asText();
        InsightsService.getInstance(getProject()).undismissInsight(insightId);
    }

    private void getCommitInfo(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {

        var commits = (ArrayNode) getObjectMapper().readTree(jsonNode.get("payload").toString()).get("commits");

        if (commits == null || commits.isEmpty()) return;

        var commitInfos = new HashMap<String, CommitInfo>();
        commits.forEach(commit -> {
            var commitStr = commit.asText();
            var url = VcsService.getInstance(getProject()).buildRemoteLinkToCommit(commitStr);
            if (url != null) {
                commitInfos.put(commitStr, new CommitInfo(commitStr, url));
            }
        });


        var message = new SetCommitInfoMessage("digma", "INSIGHTS/SET_COMMIT_INFO", new SetCommitInfoData(commitInfos));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);

    }




    private void linkTicket(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, getProject(), "got INSIGHTS/LINK_TICKET message");
        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var insightId = payload.get("insightId").asText();
        var insightType = payload.get("insightType").asText();
        var ticketLink = payload.get("ticketLink").asText();
        var linkTicketResponse = AnalyticsService.getInstance(getProject()).linkTicket(insightId, ticketLink);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", linkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        ActivityMonitor.getInstance(getProject()).registerUserActionEvent("link ticket", Map.of("insight", insightType));
    }

    private void unlinkTicket(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, getProject(), "got INSIGHTS/UNLINK_TICKET message");
        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var insightId = payload.get("insightId").asText();
        var insightType = payload.get("insightType").asText();
        var unlinkTicketResponse = AnalyticsService.getInstance(getProject()).unlinkTicket(insightId);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", unlinkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        ActivityMonitor.getInstance(getProject()).registerUserActionEvent("unlink ticket", Map.of("insight", insightType));
    }

    @Nullable
    private String getInsightBySpan(String spanCodeObjectId, String insightType) throws AnalyticsServiceException {
        try {
            return AnalyticsService.getInstance(getProject()).getInsightBySpan(spanCodeObjectId, insightType);
        } catch (AnalyticsServiceException e) {
            return null;
        }
    }

    private void getInsight(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, getProject(), "got INSIGHTS/GET_SPAN_INSIGHT message");
        JsonNode payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        JsonNode insight = null;
        try {
            var insightsString = getInsightBySpan(spanCodeObjectId, insightType);
            if (insightsString != null) {
                insight = getObjectMapper().readTree(insightsString);
            }
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(LOGGER, getProject(), e, "Error getInsight: {}", e.getMessage());
        }

        var message = new SetSpanInsightMessage("digma", "INSIGHTS/SET_SPAN_INSIGHT", new SetSpanInsightData(insight));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
    }

    private void getCodeLocations(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, getProject(), "got INSIGHTS/GET_CODE_LOCATIONS message");
        JsonNode payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var methodCodeObjectIdNode = payload.get("methodCodeObjectId");
        var codeLocations = new ArrayList<String>();
        try {
            if (methodCodeObjectIdNode != null) {
                var methodCodeObjectId = methodCodeObjectIdNode.asText();
                if (methodCodeObjectId != null && !methodCodeObjectId.isEmpty()) {
                    codeLocations.add(getMethodFQL(methodCodeObjectId));
                    return;
                }
            }

            var codeNavigator = CodeNavigator.getInstance(getProject());
            var methodCodeObjectId = codeNavigator.findMethodCodeObjectId(spanCodeObjectId);
            if (methodCodeObjectId != null) {
                codeLocations.add(getMethodFQL(methodCodeObjectId));
                return;
            }


            CodeObjectNavigation codeObjectNavigation = AnalyticsService.getInstance(getProject()).getCodeObjectNavigation(spanCodeObjectId);
            List<SpanNavigationItem> closestParentSpans = codeObjectNavigation.getNavigationEntry().getClosestParentSpans();
            var distancedMap = new TreeMap<>(closestParentSpans.stream().collect(Collectors.groupingBy(SpanNavigationItem::getDistance)));
            for (var entry : distancedMap.entrySet()) {//exit when code location found sorted by distance.
                List<SpanNavigationItem> navigationItems = entry.getValue();
                for (var navigationItem : navigationItems) {
                    methodCodeObjectId = navigationItem.getMethodCodeObjectId();
                    if (methodCodeObjectId == null) {//no method code object attached to span, try using client side discovery
                        methodCodeObjectId = codeNavigator.findMethodCodeObjectId(navigationItem.getSpanCodeObjectId());
                    }
                    if (methodCodeObjectId != null) {
                        codeLocations.add(getMethodFQL(methodCodeObjectId));
                    }
                }
                if (!codeLocations.isEmpty()) {
                    return;
                }
            }
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(LOGGER, getProject(), e, "Error getCodeLocations: {}", e.getMessage());
        } catch (Exception e) {
            Log.warnWithException(LOGGER, getProject(), e, "unhandled error while getCodeLocations: {}", e.getMessage());
        } finally {
            setCodeLocations(browser, codeLocations);
        }
    }

    private void setCodeLocations(@NotNull CefBrowser browser, List<String> codeLocations) {
        var message = new SetCodeLocationMessage("digma", "INSIGHTS/SET_CODE_LOCATIONS", new SetCodeLocationData(codeLocations));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
    }

    private String getMethodFQL(String methodCodeObjectId) {
        var pair = CodeObjectsUtil.getMethodClassAndName(methodCodeObjectId);
        var classFqn = pair.getFirst();
        var methodName = pair.getSecond();
        return String.format("%s.%s", classFqn, methodName);

    }

    private void goToInsight(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, getProject(), "got INSIGHTS/GO_TO_ASSET message");
        var spanId = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("spanCodeObjectId").asText();
        Log.log(LOGGER::debug, getProject(), "got span id {}", spanId);
        InsightsService.getInstance(getProject()).showInsight(spanId);
    }

    private void openHistogram(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, getProject(), "got INSIGHTS/OPEN_HISTOGRAM message");
        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        var displayName = payload.has("displayName") ? payload.get("displayName").asText() : null;
        org.digma.intellij.plugin.ui.insights.InsightsService.getInstance(getProject()).openHistogram(spanCodeObjectId, insightType, displayName);
    }

    private void openLiveView(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, getProject(), "got INSIGHTS/OPEN_LIVE_VIEW message");
        var codeObjectId = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("codeObjectId").asText();
        Log.log(LOGGER::debug, getProject(), "got codeObjectId {}", codeObjectId);
        InsightsService.getInstance(getProject()).openLiveView(codeObjectId);
    }


    private void recalculate(JsonNode jsonNode) throws JsonProcessingException {
        var insightId = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("id").asText();
        InsightsService.getInstance(getProject()).recalculate(insightId);
    }

    private void goToTrace(JsonNode jsonNode) {
        var payload = getPayloadFromRequest(jsonNode);
        if (payload != null) {
            var traceId = payload.get("trace").get("id").asText();
            var traceName = payload.get("trace").get("name").asText();
            var insightType = payload.get("insightType").asText();
            String spanCodeObjectId = payload.has("spanCodeObjectId") ? payload.get("spanCodeObjectId").asText() : null;
            InsightsService.getInstance(getProject()).goToTrace(traceId, traceName, insightType, spanCodeObjectId);
        }
    }

    private void goToTraceComparison(JsonNode jsonNode) {
        var payload = getPayloadFromRequest(jsonNode);
        if (payload != null) {
            var traces = payload.get("traces");
            var traceId1 = traces.get(0).get("id").asText();
            var traceName1 = traces.get(0).get("name").asText();
            var traceId2 = traces.get(1).get("id").asText();
            var traceName2 = traces.get(1).get("name").asText();
            var insightType = payload.get("insightType").asText();
            InsightsService.getInstance(getProject()).goToTraceComparison(traceId1, traceName1, traceId2, traceName2, insightType);
        }
    }


}
