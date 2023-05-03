package org.digma.intellij.plugin.jaegerui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.jaegerui.model.GoToSpanMessage;
import org.digma.intellij.plugin.jaegerui.model.SpansMessage;
import org.digma.intellij.plugin.jaegerui.model.SpansWithResolvedLocationMessage;
import org.digma.intellij.plugin.log.Log;

public class JaegerUIMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private static final Logger LOGGER = Logger.getInstance(JaegerUIMessageRouterHandler.class);

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    public JaegerUIMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

        Backgroundable.runInNewBackgroundThread(project, "Processing JaegerUI message", () -> {
            try {
                var objectMapper = new ObjectMapper();
                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();
                switch (action) {
                    case "GET_SPANS_WITH_RESOLVED_LOCATION" -> {

                        SpansMessage spansMessage = objectMapper.treeToValue(jsonNode, SpansMessage.class);

                        var resolvedSpans = JaegerUIService.getInstance(project).getResolvedSpans(spansMessage);

                        var spansWithResolvedLocationMessage = new SpansWithResolvedLocationMessage("digma",
                                "SET_SPANS_WITH_RESOLVED_LOCATION", resolvedSpans);

                        var stringMessage = objectMapper.writeValueAsString(spansWithResolvedLocationMessage);

                        browser.executeJavaScript(
                                "window.postMessage(" + stringMessage + ");",
                                jbCefBrowser.getCefBrowser().getURL(),
                                0
                        );
                    }
                    case "GO_TO_SPAN" -> {
                        GoToSpanMessage goToSpanMessage = objectMapper.treeToValue(jsonNode, GoToSpanMessage.class);
                        JaegerUIService.getInstance(project).goToSpan(goToSpanMessage);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

            } catch (JsonProcessingException e) {
                Log.debugWithException(LOGGER,e,"Exception in onQuery "+request);
            }
        });

        return true;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(LOGGER::debug,"jcef query canceled");
    }

}
