package org.digma.intellij.plugin.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.assets.model.outgoing.SetAssetsDataMessage;
import org.digma.intellij.plugin.assets.model.outgoing.SetCategoriesDataMessage;
import org.digma.intellij.plugin.assets.model.outgoing.SetServicesDataMessage;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.dashboard.outgoing.BackendInfoMessage;
import org.digma.intellij.plugin.emvironment.model.outgoing.EnvironmentChangedMessage;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest;
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStart;
import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStop;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;

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
                    case "ASSETS/INITIALIZE" -> onInitialize(browser);

                    case "ASSETS/GET_CATEGORIES_DATA" -> pushAssetCategories(browser, objectMapper, jsonNode);

                    case "ASSETS/GET_DATA" -> pushAssetsFromGetData(browser, jsonNode);

                    case "ASSETS/GO_TO_ASSET" -> goToAsset(jsonNode);

                    case "ASSETS/GET_SERVICES" -> pushServices(browser);

                    case "ASSETS/SET_SELECTED_SERVICES" -> {
                        var services = getServices(objectMapper, jsonNode);
                        PersistenceService.getInstance().setSelectedServices(project.getName(), services);
                    }

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
        var spanId = objectMapper.readTree(jsonNode.get("payload").toString()).get("spanCodeObjectId").asText();
        Log.log(LOGGER::trace, project, "got span id {}", spanId);
        AssetsService.getInstance(project).showAsset(spanId);
    }

    private synchronized void pushAssetCategories(CefBrowser browser, ObjectMapper objectMapper, JsonNode jsonNode) throws JsonProcessingException {

        EDT.assertNonDispatchThread();

        Log.log(LOGGER::trace, project, "pushCategories called");

        String[] services = getServices(objectMapper, jsonNode);

        var payload = objectMapper.readTree(AssetsService.getInstance(project).getAssetCategories(services));
        var message = new SetCategoriesDataMessage("digma", "ASSETS/SET_CATEGORIES_DATA", payload);
        Log.log(LOGGER::trace, project, "sending ASSETS/SET_CATEGORIES_DATA message");
        browser.executeJavaScript(
                "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0);
    }

    @Nullable
    private static String[] getServices(ObjectMapper objectMapper, JsonNode jsonNode) {
        String[] services = null;
        JsonNode payloadNode = jsonNode.get("payload");

        if (payloadNode != null) {
            var node = payloadNode.get("services");

            if (node == null) {
                node = payloadNode.get("query").get("services");
            }

            if (node.isArray() && node.elements().hasNext()) {
                services = objectMapper.convertValue(node, String[].class);
            }
        }

        return services;
    }

    private synchronized void pushAssets(CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {

        EDT.assertNonDispatchThread();

        Map<String, Object> mapRequest = objectMapper.convertValue(jsonNode, Map.class);
        Map<String, Object> requestPayload = (Map<String, Object>) mapRequest.get("payload");

        Map<String,String> backendQueryParams = new HashMap<>();
        // query parameters
        Map<String, Object> payloadQueryParams = (Map<String, Object>) requestPayload.get("query");
        payloadQueryParams.forEach((paramKey, paramValue) -> {
            if(!Objects.equals(paramKey, "services")) {
                backendQueryParams.put(paramKey, paramValue.toString());
            }
        });

        backendQueryParams.put("environment",PersistenceService.getInstance().getState().getCurrentEnv());
        var services = getServices(objectMapper, jsonNode);

        Log.log(LOGGER::trace, project, "pushAssets called");
        var payload = objectMapper.readTree(AssetsService.getInstance(project).getAssets(backendQueryParams, services));
        var message = new SetAssetsDataMessage("digma", "ASSETS/SET_DATA", payload);
        Log.log(LOGGER::trace, project, "sending ASSETS/SET_DATA message");
        browser.executeJavaScript(
                "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0);
    }

    private void pushAssetsFromGetData(CefBrowser browser, JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::trace, project, "got ASSETS/GET_DATA message");

        if (jsonNode.isMissingNode() || jsonNode.get("payload") == null)
            return;

        pushAssets(browser, jsonNode);
    }

    private void pushServices(CefBrowser browser) throws JsonProcessingException {
        var servicesJsonString = AssetsService.getInstance(project).getServices();
        JsonNode services;

        if (servicesJsonString != null) {
            services = objectMapper.readTree(servicesJsonString);
        } else {
            services = objectMapper.createArrayNode();
        }

        ObjectNode jNode = objectMapper.createObjectNode();
        jNode.set("services", services);
        var message = new SetServicesDataMessage("digma", "ASSETS/SET_SERVICES", jNode);
        Log.log(LOGGER::trace, project, "sending ASSETS/SET_SERVICES message");
        browser.executeJavaScript(
                "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0);
    }

    void pushGlobalEnvironmentChange() throws JsonProcessingException {
        EDT.assertNonDispatchThread();

        var curEnv = PersistenceService.getInstance().getState().getCurrentEnv();
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();
        ((ObjectNode) jsonNode).put("environment", curEnv);
        var message = new EnvironmentChangedMessage(
                JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
                JCefMessagesUtils.GLOBAL_SET_ENVIRONMENT,
                jsonNode);

        serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.getCefBrowser(), message);
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

    private void onInitialize(CefBrowser browser) {
        try {
            AboutResult about = AnalyticsService.getInstance(project).getAbout();
            var message = new BackendInfoMessage(
                    JCefMessagesUtils.REQUEST_MESSAGE_TYPE, JCefMessagesUtils.GLOBAL_SET_BACKEND_INFO,
                    about);

            Log.log(LOGGER::trace, project, "sending {} message",JCefMessagesUtils.GLOBAL_SET_BACKEND_INFO);
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(LOGGER, e, "error getting backend info");
        }
    }
}
