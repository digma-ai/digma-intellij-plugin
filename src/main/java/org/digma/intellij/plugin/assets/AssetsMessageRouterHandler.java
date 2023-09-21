package org.digma.intellij.plugin.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.assets.model.outgoing.SetAssetsDataMessage;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest;
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStart;
import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStop;

class AssetsMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private static final Logger LOGGER = Logger.getInstance(AssetsMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    private final ObjectMapper objectMapper;

    public AssetsMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat( new StdDateFormat());
    }


    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

        Backgroundable.executeOnPooledThread(() -> {

            try {
                var stopWatch = stopWatchStart();

                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();

                Log.log(LOGGER::trace, "executing action {}", action);

                switch (action) {
                    case "ASSETS/INITIALIZE" -> {
                    }

                    case "ASSETS/GET_DATA" -> pushAssetsFromGetData(browser, objectMapper);

                    case "ASSETS/GO_TO_ASSET" -> goToAsset(jsonNode);

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

                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

                stopWatchStop(stopWatch, time -> Log.log(LOGGER::trace, "action {} took {}",action, time));

            } catch (Exception e) {
                Log.debugWithException(LOGGER, e, "Exception in onQuery " + request);
            }
        });

        callback.success("");

        return true;
    }


    private void goToAsset(JsonNode jsonNode) throws JsonProcessingException {

        EDT.assertNonDispatchThread();

        Log.log(LOGGER::trace, project, "got ASSETS/GO_TO_ASSET message");
        var spanId = objectMapper.readTree(jsonNode.get("payload").toString()).get("entry").get("span").get("spanCodeObjectId").asText();
        Log.log(LOGGER::trace, project, "got span id {}", spanId);
        AssetsService.getInstance(project).showAsset(spanId);
    }


    private synchronized void pushAssets(CefBrowser browser, ObjectMapper objectMapper) throws JsonProcessingException {

        EDT.assertNonDispatchThread();

        Log.log(LOGGER::trace, project, "pushAssets called");
        var payload = objectMapper.readTree(AssetsService.getInstance(project).getAssets());
        var message = new SetAssetsDataMessage("digma", "ASSETS/SET_DATA", payload);
        Log.log(LOGGER::trace, project, "sending ASSETS/SET_DATA message");
        browser.executeJavaScript(
                "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0);
    }


    private void pushAssetsFromGetData(CefBrowser browser, ObjectMapper objectMapper) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got ASSETS/GET_DATA message");
        pushAssets(browser, objectMapper);
    }

    void pushAssetsOnEnvironmentChange() throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "pushAssetsOnEnvironmentChange called");
        pushAssets(jbCefBrowser.getCefBrowser(), objectMapper);
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
}
