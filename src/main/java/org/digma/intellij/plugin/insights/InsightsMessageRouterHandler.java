package org.digma.intellij.plugin.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.messages.MessageBusConnection;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.insights.model.outgoing.InsightsPayload;
import org.digma.intellij.plugin.insights.model.outgoing.SetInsightsDataMessage;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.jcef.common.OpenInBrowserRequest;
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class InsightsMessageRouterHandler extends CefMessageRouterHandlerAdapter implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(InsightsMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    private final MessageBusConnection messageBusConnection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InsightsMessageRouterHandler(Project project, Disposable parentDisposable, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;

        messageBusConnection = project.getMessageBus().connect(parentDisposable);
        messageBusConnection.subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, new EnvironmentChanged() {
            @Override
            public void environmentChanged(String newEnv, boolean refreshInsightsView) {
                try {
                    pushInsightsOnEnvironmentChange(jbCefBrowser.getCefBrowser(), objectMapper);
                } catch (JsonProcessingException e) {
                    Log.debugWithException(LOGGER, e, "Exception in pushInsights");
                }
            }

            @Override
            public void environmentsListChanged(List<String> newEnvironments) {
                //nothing to do
            }
        });
    }


    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

        Backgroundable.runInNewBackgroundThread(project, "Processing Insights message", () -> {
            try {

                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();
                switch (action) {
//                    case "INSIGHTS/GET_DATA" -> pushInsightsFromGetData(browser, objectMapper);

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
        Log.log(LOGGER::debug, project, "got span id {}",spanId);
        InsightsService.getInstance(project).showInsight(spanId);
    }

    private void openHistogram(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_HISTOGRAM message");
        var spanCodeObjectId = objectMapper.readTree(jsonNode.get("payload").toString()).get("spanCodeObjectId").asText();
        var insightType = objectMapper.readTree(jsonNode.get("payload").toString()).get("insightType").asText();
        InsightsService.getInstance(project).openHistogram(spanCodeObjectId,insightType);
    }

    private void openLiveView(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/OPEN_LIVE_VIEW message");
        var prefixedCodeObjectId = objectMapper.readTree(jsonNode.get("payload").toString()).get("prefixedCodeObjectId").asText();
        Log.log(LOGGER::debug, project, "got prefixedCodeObjectId {}",prefixedCodeObjectId);
        InsightsService.getInstance(project).openLiveView(prefixedCodeObjectId);
    }


    private synchronized void pushInsights(CefBrowser browser, ObjectMapper objectMapper) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "pushAssets called");
        var insights = InsightsService.getInstance(project).getInsights();
        var payload = new InsightsPayload(insights);
        var message = new SetInsightsDataMessage("digma", "INSIGHTS/SET_DATA", payload);
        Log.log(LOGGER::debug, project, "sending INSIGHTS/SET_DATA message");
        browser.executeJavaScript(
                "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0);
    }


    private void pushInsightsFromGetData(CefBrowser browser, ObjectMapper objectMapper) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got INSIGHTS/GET_DATA message");
        pushInsights(browser, objectMapper);
    }

    private void pushInsightsOnEnvironmentChange(CefBrowser cefBrowser, ObjectMapper objectMapper) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "pushInsightsOnEnvironmentChange called");
        pushInsights(cefBrowser, objectMapper);
    }

    public void pushInsightsFromEvent() {
        try {
            pushInsights(jbCefBrowser.getCefBrowser(),objectMapper);
        } catch (JsonProcessingException e) {
            Log.debugWithException(LOGGER, e, "Exception in pushInsightsFromEvent");
        }
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


    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(LOGGER::debug, "jcef query canceled");
    }

    @Override
    public void dispose() {
        messageBusConnection.dispose();
    }


}
