package org.digma.intellij.plugin.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.assets.model.outgoing.SetAssetsDataMessage;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;

import java.util.List;

public class AssetsMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private static final Logger LOGGER = Logger.getInstance(AssetsMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssetsMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;

        project.getMessageBus().connect().subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, new EnvironmentChanged() {
            @Override
            public void environmentChanged(String newEnv, boolean refreshInsightsView) {
                try {
                    pushAssets(jbCefBrowser.getCefBrowser(),objectMapper);
                } catch (JsonProcessingException e) {
                    Log.debugWithException(LOGGER, e, "Exception in pushAssets ");
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

        Backgroundable.runInNewBackgroundThread(project, "Processing Assets message", () -> {
            try {

                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();
                switch (action) {
                    case "ASSETS/GET_DATA" -> pushAssets(browser, objectMapper);

                    case "ASSETS/GO_TO_ASSET" -> gotToAsset(jsonNode);

                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

            } catch (Exception e) {
                Log.debugWithException(LOGGER, e, "Exception in onQuery " + request);
            }
        });

        return true;
    }

    private void gotToAsset(JsonNode jsonNode) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got ASSETS/GO_TO_ASSET message");
        var spanId = objectMapper.readTree(jsonNode.get("payload").toString()).get("entry").get("span").get("spanCodeObjectId").asText();
        Log.log(LOGGER::debug, project, "got span id {}",spanId);
        AssetsService.getInstance(project).showAsset(spanId);
    }


    private synchronized void pushAssets(CefBrowser browser, ObjectMapper objectMapper) throws JsonProcessingException {
        Log.log(LOGGER::debug, project, "got ASSETS/GET_DATA message");
        var payload = objectMapper.readTree(AssetsService.getInstance(project).getAssets());
        var message = new SetAssetsDataMessage("digma", "ASSETS/SET_DATA", payload);
        Log.log(LOGGER::debug, project, "sending ASSETS/SET_DATA message");
        browser.executeJavaScript(
                "window.postMessage(" + objectMapper.writeValueAsString(message) + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0);
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(LOGGER::debug, "jcef query canceled");
    }

}
