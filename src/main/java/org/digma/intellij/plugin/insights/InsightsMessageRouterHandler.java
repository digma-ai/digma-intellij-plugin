package org.digma.intellij.plugin.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.insights.model.outgoing.InsightsPayload;
import org.digma.intellij.plugin.insights.model.outgoing.Method;
import org.digma.intellij.plugin.insights.model.outgoing.SetInsightsDataMessage;
import org.digma.intellij.plugin.insights.model.outgoing.Span;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.jcef.common.OpenInBrowserRequest;
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.service.InsightsService;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class InsightsMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private static final Logger LOGGER = Logger.getInstance(InsightsMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;


    private final ObjectMapper objectMapper = new ObjectMapper();

    public InsightsMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;


    }


    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

        Backgroundable.runInNewBackgroundThread(project, "Processing Insights message", () -> {
            try {

                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();
                switch (action) {
                    case "INSIGHTS/GET_DATA" -> pushInsightsFromGetData();

                    case "INSIGHTS/GO_TO_ASSET" -> goToInsight(jsonNode);

                    case "INSIGHTS/OPEN_LIVE_VIEW" -> openLiveView(jsonNode);

                    case "INSIGHTS/OPEN_HISTOGRAM" -> openHistogram(jsonNode);



                    case JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        OpenInBrowserRequest openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInBrowserRequest.class);
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

                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

            } catch (Exception e) {
                Log.debugWithException(LOGGER, e, "Exception in onQuery " + request);
            }
        });

        callback.success("");

        return true;
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
                      boolean hasMissingDependency) {


        var payload = new InsightsPayload(insights, spans, assetId, serviceName, environment, uiInsightsStatus, viewMode, methods, hasMissingDependency);


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

        var message = new SetInsightsDataMessage("digma", "INSIGHTS/SET_DATA", InsightsPayload.EMPTY);
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
