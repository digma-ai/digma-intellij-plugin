package org.digma.intellij.plugin.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.CodeObjectsUtil;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.insights.InsightType;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.insights.InsightsService;
import org.digma.intellij.plugin.ui.insights.model.*;
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler;
import org.digma.intellij.plugin.vcs.VcsService;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.ui.jcef.JCEFUtilsKt.getQueryMapFromPayload;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;

//todo: convert to kotlin and move to org.digma.intellij.plugin.ui.insights
public class InsightsMessageRouterHandler extends BaseMessageRouterHandler {

    private final Logger LOGGER = Logger.getInstance(getClass());

    private final Project project;

    public InsightsMessageRouterHandler(Project project) {
        super(project);
        this.project = project;
    }


    @NotNull
    @Override
    public String getOriginForTroubleshootingEvent() {
        return "insights";
    }


    @Override
    public void doOnQuery(@NotNull Project project, @NotNull CefBrowser browser, @NotNull JsonNode requestJsonNode, @NotNull String rawRequest, @NotNull String action) throws Exception {

        var jsonNode = getObjectMapper().readTree(rawRequest);
        switch (action) {

            case "INSIGHTS/INITIALIZE" -> onInitialize(browser);

            case "INSIGHTS/GO_TO_ASSET" -> goToInsight(jsonNode);

            case "INSIGHTS/OPEN_LIVE_VIEW" -> openLiveView(jsonNode);

            case "INSIGHTS/OPEN_HISTOGRAM" -> openHistogram(jsonNode);

            case "INSIGHTS/RECALCULATE" -> recalculate(jsonNode);

            case "INSIGHTS/GO_TO_TRACE" -> goToTrace(jsonNode);

            case "INSIGHTS/GO_TO_TRACE_COMPARISON" -> goToTraceComparison(jsonNode);

            case "INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED" -> markInsightsViewed(jsonNode);

            case "INSIGHTS/LINK_TICKET" -> linkTicket(browser, jsonNode);

            case "INSIGHTS/UNLINK_TICKET" -> unlinkTicket(browser, jsonNode);

            case "INSIGHTS/GET_CODE_LOCATIONS" -> getCodeLocations(browser, jsonNode);

            case "INSIGHTS/GET_SPAN_INSIGHT" -> getInsight(browser, jsonNode);

            case "INSIGHTS/GET_COMMIT_INFO" -> getCommitInfo(browser, jsonNode);

            case "INSIGHTS/GET_DATA_LIST" -> pushInsightsListData(jsonNode);


            default -> Log.log(LOGGER::warn, "got unexpected action='{}'", action);
        }
    }

    private void pushInsightsListData(JsonNode jsonNode) {
        Log.log(LOGGER::debug, project, "got INSIGHTS/GET_DATA_LIST message");
        Map<String, Object> backendQueryParams = getQueryMapFromPayload(jsonNode, getObjectMapper());
        InsightsService.getInstance(project).refreshInsightsList(backendQueryParams);
    }

    private void getCommitInfo(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {

        var commits = (ArrayNode) getObjectMapper().readTree(jsonNode.get("payload").toString()).get("commits");

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


    private void markInsightsViewed(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED message");
        var insightsTypesJasonArray = (ArrayNode) getObjectMapper().readTree(jsonNode.get("payload").toString()).get("insightTypes");
        List<Pair<InsightType, Integer>> insightTypeList = new ArrayList<>();
        insightsTypesJasonArray.forEach(insightType -> {
            var type = InsightType.valueOf(insightType.get("type").asText());
            var reopenCountObject = insightType.get("reopenCount");
            var reopensCount = (reopenCountObject != null) ? reopenCountObject.asInt() : 0;
            Pair<InsightType, Integer> insightOpensCount = new Pair<>(type, reopensCount);
            insightTypeList.add(insightOpensCount);
        });
        Log.log(LOGGER::trace, project, "got insights types {}", insightTypeList);
        ActivityMonitor.getInstance(project).registerInsightsViewed(insightTypeList);
    }

    private void linkTicket(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/LINK_TICKET message");
        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var codeObjectId = payload.get("codeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        var ticketLink = payload.get("ticketLink").asText();
        var linkTicketResponse = AnalyticsService.getInstance(project).linkTicket(codeObjectId, insightType, ticketLink);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", linkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        ActivityMonitor.getInstance(project).registerUserActionEvent("link ticket", Map.of("insight", insightType));
    }

    private void unlinkTicket(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/UNLINK_TICKET message");
        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var codeObjectId = payload.get("codeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        var unlinkTicketResponse = AnalyticsService.getInstance(project).unlinkTicket(codeObjectId, insightType);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", unlinkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        ActivityMonitor.getInstance(project).registerUserActionEvent("unlink ticket", Map.of("insight", insightType));
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
            Log.warnWithException(LOGGER, project, e, "Error getInsight: {}", e.getMessage());
        }

        var message = new SetSpanInsightMessage("digma", "INSIGHTS/SET_SPAN_INSIGHT", new SetSpanInsightData(insight));
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
    }

    private void getCodeLocations(@NotNull CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/GET_CODE_LOCATIONS message");
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

            var codeNavigator = CodeNavigator.getInstance(project);
            var methodCodeObjectId = codeNavigator.findMethodCodeObjectId(spanCodeObjectId);
            if (methodCodeObjectId != null) {
                codeLocations.add(getMethodFQL(methodCodeObjectId));
                return;
            }


            CodeObjectNavigation codeObjectNavigation = AnalyticsService.getInstance(project).getCodeObjectNavigation(spanCodeObjectId);
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
            Log.warnWithException(LOGGER, project, e, "Error getCodeLocations: {}", e.getMessage());
        } catch (Exception e) {
            Log.warnWithException(LOGGER, project, e, "unhandled error while getCodeLocations: {}", e.getMessage());
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
        Log.log(LOGGER::debug, project, "got INSIGHTS/GO_TO_ASSET message");
        var spanId = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("spanCodeObjectId").asText();
        Log.log(LOGGER::debug, project, "got span id {}", spanId);
        InsightsService.getInstance(project).showInsight(spanId);
    }

    private void openHistogram(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_HISTOGRAM message");
        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var instrumentationLibrary = payload.get("instrumentationLibrary").asText();
        var name = payload.get("name").asText();
        var insightType = payload.get("insightType").asText();
        var displayName = payload.has("displayName") ? payload.get("displayName").asText() : null;
        org.digma.intellij.plugin.ui.insights.InsightsService.getInstance(project).openHistogram(instrumentationLibrary, name, insightType, displayName);
    }

    private void openLiveView(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_LIVE_VIEW message");
        var prefixedCodeObjectId = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("prefixedCodeObjectId").asText();
        Log.log(LOGGER::debug, project, "got prefixedCodeObjectId {}", prefixedCodeObjectId);
        InsightsService.getInstance(project).openLiveView(prefixedCodeObjectId);
    }


    private void recalculate(JsonNode jsonNode) throws JsonProcessingException {
        var prefixedCodeObjectId = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("prefixedCodeObjectId").asText();
        var insightType = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).recalculate(prefixedCodeObjectId, insightType);
    }

    private void goToTrace(JsonNode jsonNode) throws JsonProcessingException {

        var payload = getObjectMapper().readTree(jsonNode.get("payload").toString());
        var traceId = payload.get("trace").get("id").asText();
        var traceName = payload.get("trace").get("name").asText();
        var insightType = payload.get("insightType").asText();
        String spanCodeObjectId = payload.has("spanCodeObjectId") ? payload.get("spanCodeObjectId").asText() : null;

        InsightsService.getInstance(project).goToTrace(traceId, traceName, InsightType.valueOf(insightType), spanCodeObjectId);
    }

    private void goToTraceComparison(JsonNode jsonNode) throws JsonProcessingException {

        var traces = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("traces");
        var traceId1 = traces.get(0).get("id").asText();
        var traceName1 = traces.get(0).get("name").asText();
        var traceId2 = traces.get(1).get("id").asText();
        var traceName2 = traces.get(1).get("name").asText();
        var insightType = getObjectMapper().readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).goToTraceComparison(traceId1, traceName1, traceId2, traceName2, InsightType.valueOf(insightType));
    }


    private void onInitialize(CefBrowser browser) {
        doCommonInitialize(browser);
    }

}
