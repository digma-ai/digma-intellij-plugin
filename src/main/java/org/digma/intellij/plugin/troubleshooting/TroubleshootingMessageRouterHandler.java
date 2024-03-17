package org.digma.intellij.plugin.troubleshooting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.*;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.documentation.DocumentationService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.jcef.*;
import org.digma.intellij.plugin.ui.jcef.model.*;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.jsonToObject;

class TroubleshootingMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private static final Logger LOGGER = Logger.getInstance(TroubleshootingMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TroubleshootingMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;
    }


    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

        Backgroundable.runInNewBackgroundThread(project, "Processing troubleshooting message", () -> {
            try {

                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();
                switch (action) {
                    case "TROUBLESHOOTING/CLOSE" -> EDT.ensureEDT(() -> MainToolWindowCardsController.getInstance(project).troubleshootingFinished());

                    case JCEFGlobalConstants.GLOBAL_OPEN_DOCUMENTATION -> {
                        Log.log(LOGGER::trace, project, "got {} message", JCEFGlobalConstants.GLOBAL_OPEN_DOCUMENTATION);
                        var page = objectMapper.readTree(jsonNode.get("payload").toString()).get("page").asText();
                        Log.log(LOGGER::trace, project, "got page {}", page);
                        DocumentationService.getInstance(project).openDocumentation(page);
                    }


                    case JCEFGlobalConstants.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        OpenInDefaultBrowserRequest openBrowserRequest = jsonToObject(request, OpenInDefaultBrowserRequest.class);
                        if (openBrowserRequest != null && openBrowserRequest.getPayload() != null) {
                            BrowserUtil.browse(openBrowserRequest.getPayload().getUrl());
                        }
                    }

                    case JCEFGlobalConstants.GLOBAL_SEND_TRACKING_EVENT -> {
                        SendTrackingEventRequest trackingRequest = jsonToObject(request, SendTrackingEventRequest.class);
                        if (trackingRequest != null && trackingRequest.getPayload() != null) {
                            if (trackingRequest.getPayload().getData() == null) {
                                ActivityMonitor.getInstance(project).registerCustomEvent(trackingRequest.getPayload().getEventName(), Collections.emptyMap());
                            } else {
                                ActivityMonitor.getInstance(project).registerCustomEvent(trackingRequest.getPayload().getEventName(), trackingRequest.getPayload().getData());
                            }
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


    void sendRequestToChangeUiTheme(@NotNull Theme theme) {
        JCefMessagesUtilsKt.sendRequestToChangeUiTheme(theme, jbCefBrowser);
    }

    void sendRequestToChangeFont(String fontName) {
        JCefMessagesUtilsKt.sendRequestToChangeFont(fontName, jbCefBrowser);
    }

    void sendRequestToChangeCodeFont(String fontName) {
        JCefMessagesUtilsKt.sendRequestToChangeCodeFont(fontName, jbCefBrowser);
    }


    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(LOGGER::debug, "jcef query canceled");
    }
}
