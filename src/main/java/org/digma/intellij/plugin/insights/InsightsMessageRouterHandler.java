package org.digma.intellij.plugin.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import kotlin.Pair;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsProviderException;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.insights.model.outgoing.CommitInfo;
import org.digma.intellij.plugin.insights.model.outgoing.InsightsPayload;
import org.digma.intellij.plugin.insights.model.outgoing.Method;
import org.digma.intellij.plugin.insights.model.outgoing.SetCodeLocationData;
import org.digma.intellij.plugin.insights.model.outgoing.SetCodeLocationMessage;
import org.digma.intellij.plugin.insights.model.outgoing.SetCommitInfoData;
import org.digma.intellij.plugin.insights.model.outgoing.SetCommitInfoMessage;
import org.digma.intellij.plugin.insights.model.outgoing.SetInsightsDataMessage;
import org.digma.intellij.plugin.insights.model.outgoing.SetLinkUnlinkResponseMessage;
import org.digma.intellij.plugin.insights.model.outgoing.SetSpanInsightData;
import org.digma.intellij.plugin.insights.model.outgoing.SetSpanInsightMessage;
import org.digma.intellij.plugin.insights.model.outgoing.Span;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse;
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation;
import org.digma.intellij.plugin.model.rest.navigation.SpanNavigationItem;
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.service.InsightsActionsService;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.jcef.RegistrationEventHandler;
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest;
import org.digma.intellij.plugin.ui.service.InsightsService;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.digma.intellij.plugin.vcs.VcsService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStart;
import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStop;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;


class InsightsMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private static final Logger LOGGER = Logger.getInstance(InsightsMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;


    private final ObjectMapper objectMapper;

    public InsightsMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat( new StdDateFormat());
    }


    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

        Backgroundable.executeOnPooledThread( () -> {
            try {

                var stopWatch = stopWatchStart();

                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();
                switch (action) {

                    case "INSIGHTS/INITIALIZE" -> {
                    }

                    case "INSIGHTS/GET_DATA" -> pushInsightsFromGetData();

                    case "INSIGHTS/GO_TO_ASSET" -> goToInsight(jsonNode);

                    case "INSIGHTS/OPEN_LIVE_VIEW" -> openLiveView(jsonNode);

                    case "INSIGHTS/OPEN_HISTOGRAM" -> openHistogram(jsonNode);

                    case "INSIGHTS/GO_TO_ERRORS" -> goToErrors();

                    case "INSIGHTS/GO_TO_ERROR" -> goToError(jsonNode);

                    case "INSIGHTS/GO_TO_METHOD" -> goToMethod(jsonNode);

                    case "INSIGHTS/RECALCULATE" -> recalculate(jsonNode);

                    case "INSIGHTS/REFRESH_ALL" -> refresh(jsonNode);

                    case "INSIGHTS/GO_TO_TRACE" -> goToTrace(jsonNode);

                    case "INSIGHTS/GO_TO_TRACE_COMPARISON" -> goToTraceComparison(jsonNode);

                    case "INSIGHTS/AUTOFIX_MISSING_DEPENDENCY" -> fixMissingDependencies(jsonNode);

                    case "INSIGHTS/ADD_ANNOTATION" -> addAnnotation(jsonNode);

                    case "INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED" -> markInsightsViewed(jsonNode);

                    case "INSIGHTS/LINK_TICKET" -> linkTicket(jsonNode);

                    case "INSIGHTS/UNLINK_TICKET" -> unlinkTicket(jsonNode);

                    case JCefMessagesUtils.GLOBAL_OPEN_TROUBLESHOOTING_GUIDE ->
                            EDT.ensureEDT(() -> MainToolWindowCardsController.getInstance(project).showTroubleshooting());

                    case JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        OpenInDefaultBrowserRequest openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInDefaultBrowserRequest.class);
                        if (openBrowserRequest != null && openBrowserRequest.getPayload() != null) {
                            BrowserUtil.browse(openBrowserRequest.getPayload().getUrl());
                        }
                    }

                    case JCefMessagesUtils.GLOBAL_SEND_TRACKING_EVENT -> {
                        SendTrackingEventRequest trackingRequest = JCefMessagesUtils.parseJsonToObject(request, SendTrackingEventRequest.class);
                        if (trackingRequest != null && trackingRequest.getPayload() != null) {
                            ActivityMonitor.getInstance(project).registerCustomEvent(trackingRequest.getPayload().getEventName(), trackingRequest.getPayload().getData());
                        }
                    }
                    case "INSIGHTS/GET_CODE_LOCATIONS" -> getCodeLocations(jsonNode);

                    case "INSIGHTS/GET_SPAN_INSIGHT" -> getInsight(jsonNode);

                    case "INSIGHTS/GET_COMMIT_INFO" -> getCommitInfo(jsonNode);

                    case JCefMessagesUtils.GLOBAL_REGISTER -> RegistrationEventHandler.getInstance(project).register(jsonNode);

                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

                stopWatchStop(stopWatch, time -> Log.log(LOGGER::trace, "action {} took {}",action, time));

            } catch (Exception e) {
                Log.debugWithException(LOGGER, e, "Exception in onQuery " + request);
                ErrorReporter.getInstance().reportError(project, "InsightsMessageRouterHandler.onQuery", e);
            }
        });

        callback.success("");

        return true;
    }

    private void getCommitInfo(JsonNode jsonNode) throws JsonProcessingException {

        var commits = (ArrayNode) objectMapper.readTree(jsonNode.get("payload").toString()).get("commits");

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
        serializeAndExecuteWindowPostMessageJavaScript(this.jbCefBrowser.getCefBrowser(), message);

    }


    private void markInsightsViewed(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED message");
        var insightsTypesJasonArray = (ArrayNode) objectMapper.readTree(jsonNode.get("payload").toString()).get("insightTypes");
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

    private void linkTicket(JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/LINK_TICKET message");
        var payload = objectMapper.readTree(jsonNode.get("payload").toString());
        var codeObjectId = payload.get("codeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        var ticketLink = payload.get("ticketLink").asText();
        var linkTicketResponse = AnalyticsService.getInstance(project).linkTicket(codeObjectId, insightType, ticketLink);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", linkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(this.jbCefBrowser.getCefBrowser(), message);
        ActivityMonitor.getInstance(project).registerUserActionEvent("link ticket", Map.of("insight", insightType));
    }

    private void unlinkTicket(JsonNode jsonNode) throws JsonProcessingException, AnalyticsServiceException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/UNLINK_TICKET message");
        var payload = objectMapper.readTree(jsonNode.get("payload").toString());
        var codeObjectId = payload.get("codeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        var unlinkTicketResponse = AnalyticsService.getInstance(project).unlinkTicket(codeObjectId, insightType);
        var message = new SetLinkUnlinkResponseMessage("digma", "INSIGHTS/SET_TICKET_LINK", unlinkTicketResponse);
        serializeAndExecuteWindowPostMessageJavaScript(this.jbCefBrowser.getCefBrowser(), message);
        ActivityMonitor.getInstance(project).registerUserActionEvent("unlink ticket", Map.of("insight", insightType));
    }

    private CodeObjectInsight getInsightBySpanTemporary(String spanCodeObjectId, String insightType) throws AnalyticsServiceException {
        InsightsOfSingleSpanResponse insightBySpan = AnalyticsService.getInstance(project).getInsightsForSingleSpan(spanCodeObjectId);
        return insightBySpan.getInsights().stream()
                .filter(o -> o.getType().name().equals(insightType)).findFirst().orElse(null);
    }
    private CodeObjectInsight getInsightBySpan(String spanCodeObjectId, String insightType) throws AnalyticsServiceException {
        try {
            return AnalyticsService.getInstance(project).getInsightBySpan(spanCodeObjectId, insightType);
        } catch (AnalyticsServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AnalyticsProviderException) {
                if(((AnalyticsProviderException)cause).getResponseCode() == 404)
                {
                    return getInsightBySpanTemporary(spanCodeObjectId, insightType);
                }
            }
            throw e;
        }
    }
    private void getInsight(JsonNode jsonNode) throws JsonProcessingException {

        Log.log(LOGGER::trace, project, "got INSIGHTS/GET_SPAN_INSIGHT message");
        JsonNode payload = objectMapper.readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var insightType = payload.get("insightType").asText();
        CodeObjectInsight insight = null;
        try {
            insight = getInsightBySpan(spanCodeObjectId, insightType);
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(LOGGER, project, e, "Error getInsight: {}", e.getMessage());
        }
        catch (Exception e){
            Log.warnWithException(LOGGER, project, e, "unhandled error while getInsight: {}", e.getMessage());
        }
        finally {
            var message = new SetSpanInsightMessage("digma", "INSIGHTS/SET_SPAN_INSIGHT", new SetSpanInsightData(insight));
            serializeAndExecuteWindowPostMessageJavaScript(this.jbCefBrowser.getCefBrowser(), message);
        }

    }

    private void getCodeLocations(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got INSIGHTS/GET_CODE_LOCATIONS message");
        JsonNode payload = objectMapper.readTree(jsonNode.get("payload").toString());
        var spanCodeObjectId = payload.get("spanCodeObjectId").asText();
        var methodCodeObjectIdNode = payload.get("methodCodeObjectId");
        var codeLocations = new ArrayList<String>();
        try {
            if(methodCodeObjectIdNode != null)
            {
                var methodCodeObjectId = methodCodeObjectIdNode.asText();
                if(methodCodeObjectId != null && methodCodeObjectId != ""){
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
            var distancedMap = new TreeMap<>(closestParentSpans.stream().collect(Collectors.groupingBy(o -> o.getDistance())));
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
        }
        catch (Exception e){
            Log.warnWithException(LOGGER, project, e, "unhandled error while getCodeLocations: {}", e.getMessage());
        }
        finally {
            setCodeLocations(codeLocations);
        }
    }

    private void setCodeLocations(List<String> codeLocations) {
        var message = new SetCodeLocationMessage("digma", "INSIGHTS/SET_CODE_LOCATIONS", new SetCodeLocationData(codeLocations));
        serializeAndExecuteWindowPostMessageJavaScript( this.jbCefBrowser.getCefBrowser(), message);
    }
    private String getMethodFQL(String methodCodeObjectId){
        var pair = CodeObjectsUtil.getMethodClassAndName(methodCodeObjectId);
        var classFqn = pair.getSecond();
        var methodName = pair.getFirst();
        return  String.format("%s.%s",classFqn,methodName);

    }
    private void goToInsight(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/GO_TO_ASSET message");
        var spanId = objectMapper.readTree(jsonNode.get("payload").toString()).get("spanCodeObjectId").asText();
        Log.log(LOGGER::debug, project, "got span id {}", spanId);
        InsightsService.getInstance(project).showInsight(spanId);
    }

    private void openHistogram(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_HISTOGRAM message");
        var instrumentationLibrary = objectMapper.readTree(jsonNode.get("payload").toString()).get("instrumentationLibrary").asText();
        var name = objectMapper.readTree(jsonNode.get("payload").toString()).get("name").asText();
        var insightType = objectMapper.readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).openHistogram(instrumentationLibrary, name, insightType);
    }

    private void openLiveView(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_LIVE_VIEW message");
        var prefixedCodeObjectId = objectMapper.readTree(jsonNode.get("payload").toString()).get("prefixedCodeObjectId").asText();
        Log.log(LOGGER::debug, project, "got prefixedCodeObjectId {}", prefixedCodeObjectId);
        InsightsService.getInstance(project).openLiveView(prefixedCodeObjectId);
    }

    private void goToErrors() {
        project.getService(InsightsActionsService.class).showErrorsTab();
        ActivityMonitor.getInstance(project).registerButtonClicked("expand-errors", InsightType.Errors);
    }

    private void goToError(JsonNode jsonNode) throws JsonProcessingException {
        ActivityMonitor.getInstance(project).registerCustomEvent("error-insight top-error-clicked", null);
        var errorUid = objectMapper.readTree(jsonNode.get("payload").toString()).get("errorId").asText();
        project.getService(ErrorsViewOrchestrator.class).showErrorDetails(errorUid);
    }

    private void goToMethod(JsonNode jsonNode) throws JsonProcessingException {
        var methodId = objectMapper.readTree(jsonNode.get("payload").toString()).get("id").asText();
        EDT.ensureEDT(() -> project.getService(InsightsActionsService.class).navigateToMethodFromFunctionsListPanel(methodId));
    }

    private void recalculate(JsonNode jsonNode) throws JsonProcessingException {
        var prefixedCodeObjectId = objectMapper.readTree(jsonNode.get("payload").toString()).get("prefixedCodeObjectId").asText();
        var insightType = objectMapper.readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).recalculate(prefixedCodeObjectId, insightType);
    }

    private void refresh(JsonNode jsonNode) throws JsonProcessingException {
        var insightType = objectMapper.readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).refresh(InsightType.valueOf(insightType));
    }

    private void goToTrace(JsonNode jsonNode) throws JsonProcessingException {

        var traceId = objectMapper.readTree(jsonNode.get("payload").toString()).get("trace").get("id").asText();
        var traceName = objectMapper.readTree(jsonNode.get("payload").toString()).get("trace").get("name").asText();
        var insightType = objectMapper.readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        var spanCodeObjectId = objectMapper.readTree(jsonNode.get("payload").toString()).get("spanCodeObjectId").asText();
        InsightsService.getInstance(project).goToTrace(traceId, traceName, InsightType.valueOf(insightType), spanCodeObjectId);
    }

    private void goToTraceComparison(JsonNode jsonNode) throws JsonProcessingException {

        var traces = objectMapper.readTree(jsonNode.get("payload").toString()).get("traces");
        var traceId1 = traces.get(0).get("id").asText();
        var traceName1 = traces.get(0).get("name").asText();
        var traceId2 = traces.get(1).get("id").asText();
        var traceName2 = traces.get(1).get("name").asText();
        var insightType = objectMapper.readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).goToTraceComparison(traceId1, traceName1, traceId2, traceName2, InsightType.valueOf(insightType));
    }


    private void addAnnotation(JsonNode jsonNode) throws JsonProcessingException {
        var methodId = objectMapper.readTree(jsonNode.get("payload").toString()).get("methodId").asText();
        InsightsService.getInstance(project).addAnnotation(methodId);
    }

    private void fixMissingDependencies(JsonNode jsonNode) throws JsonProcessingException {
        var methodId = objectMapper.readTree(jsonNode.get("payload").toString()).get("methodId").asText();
        InsightsService.getInstance(project).fixMissingDependencies(methodId);
    }


    private void pushInsightsFromGetData() {
        Log.log(LOGGER::debug, project, "got INSIGHTS/GET_DATA message");
        InsightsService.getInstance(project).refreshInsights();
    }


    void sendRequestToChangeUiTheme(@NotNull Theme theme) {
        JCefBrowserUtil.sendRequestToChangeUiTheme(theme, jbCefBrowser);
    }

    void sendRequestToChangeFont(String fontName) {
        JCefBrowserUtil.sendRequestToChangeFont(fontName, jbCefBrowser);
    }

    void sendRequestToChangeCodeFont(String fontName) {
        JCefBrowserUtil.sendRequestToChangeCodeFont(fontName, jbCefBrowser);
    }


    void pushInsights(List<CodeObjectInsight> insights, List<Span> spans, String assetId,
                      String serviceName, String environment, String uiInsightsStatus, String viewMode,
                      List<Method> methods,
                      boolean hasMissingDependency,
                      boolean canInstrumentMethod,
                      boolean needsObservabilityFix) {


        var payload = new InsightsPayload(insights, spans, assetId, serviceName, environment, uiInsightsStatus, viewMode, methods, hasMissingDependency, canInstrumentMethod, needsObservabilityFix);


        var message = new SetInsightsDataMessage("digma", "INSIGHTS/SET_DATA", payload);
        Log.log(LOGGER::debug, project, "sending INSIGHTS/SET_DATA message");
        try {
            jbCefBrowser.getCefBrowser().executeJavaScript(
                    "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                    jbCefBrowser.getCefBrowser().getURL(),
                    0);
        } catch (JsonProcessingException e) {
            Log.warnWithException(LOGGER, project, e, "Error sending message to webview");
        }
    }


    void emptyInsights() {

        var message = new SetInsightsDataMessage("digma", "INSIGHTS/SET_DATA", InsightsPayload.EMPTY_INSIGHTS);
        try {
            jbCefBrowser.getCefBrowser().executeJavaScript(
                    "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                    jbCefBrowser.getCefBrowser().getURL(),
                    0);
        } catch (JsonProcessingException e) {
            Log.warnWithException(LOGGER, project, e, "Error sending message to webview");
        }
    }

    void emptyPreview() {

        var message = new SetInsightsDataMessage("digma", "INSIGHTS/SET_DATA", InsightsPayload.EMPTY_PREVIEW);
        try {
            jbCefBrowser.getCefBrowser().executeJavaScript(
                    "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                    jbCefBrowser.getCefBrowser().getURL(),
                    0);
        } catch (JsonProcessingException e) {
            Log.warnWithException(LOGGER, project, e, "Error sending message to webview");
        }
    }
}
