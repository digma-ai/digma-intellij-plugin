package org.digma.intellij.plugin.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.digma.intellij.plugin.assets.model.outgoing.SetAssetsDataMessage;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.toolwindow.common.UICodeFontRequest;
import org.digma.intellij.plugin.toolwindow.common.UIFontRequest;
import org.digma.intellij.plugin.toolwindow.common.UIThemeRequest;
import org.digma.intellij.plugin.toolwindow.common.UiCodeFontPayload;
import org.digma.intellij.plugin.toolwindow.common.UiFontPayload;
import org.digma.intellij.plugin.toolwindow.common.UiThemePayload;
import org.digma.intellij.plugin.toolwindow.recentactivity.JBCefBrowserUtil;

import java.util.List;

import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_UI_CODE_FONT;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_UI_MAIN_FONT;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_UI_THEME;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.REQUEST_MESSAGE_TYPE;

public class AssetsMessageRouterHandler extends CefMessageRouterHandlerAdapter implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(AssetsMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    private final MessageBusConnection messageBusConnection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssetsMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;

        messageBusConnection = project.getMessageBus().connect(this);
        messageBusConnection.subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, new EnvironmentChanged() {
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



    void sendRequestToChangeUiTheme(String uiTheme) {
        String requestMessage = JBCefBrowserUtil.resultToString(
                new UIThemeRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_UI_THEME,
                        new UiThemePayload(uiTheme)
                ));
        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }

    void sendRequestToChangeFont(String font) {
        String requestMessage = JBCefBrowserUtil.resultToString(
                new UIFontRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_UI_MAIN_FONT,
                        new UiFontPayload(font)
                ));
        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }

    void sendRequestToChangeCodeFont(String font) {
        String requestMessage = JBCefBrowserUtil.resultToString(
                new UICodeFontRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_UI_CODE_FONT,
                        new UiCodeFontPayload(font)
                ));
        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
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
