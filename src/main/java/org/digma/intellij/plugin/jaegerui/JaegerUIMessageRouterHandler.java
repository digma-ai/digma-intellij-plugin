package org.digma.intellij.plugin.jaegerui;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.jaegerui.model.incoming.GoToSpanMessage;
import org.digma.intellij.plugin.jaegerui.model.incoming.SpansMessage;
import org.digma.intellij.plugin.jaegerui.model.outgoing.SpanData;
import org.digma.intellij.plugin.jaegerui.model.outgoing.SpansWithResolvedLocationMessage;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest;

import java.util.Collections;
import java.util.Map;

import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStart;
import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStop;

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

        Backgroundable.executeOnPooledThread( () -> {
            try {

                var stopWatch = stopWatchStart();

                var objectMapper = new ObjectMapper();
                var jsonNode = objectMapper.readTree(request);

                String action = jsonNode.get("action").asText();
                switch (action) {
                    case "GET_SPANS_DATA" -> {

                        SpansMessage spansMessage = objectMapper.treeToValue(jsonNode, SpansMessage.class);

                        Map<String, SpanData> resolvedSpans;
                        try {
                            resolvedSpans = JaegerUIService.getInstance(project).getResolvedSpans(spansMessage);
                        }catch (Exception e){
                            Log.debugWithException(LOGGER,e,"Exception while resolving spans for GET_SPANS_DATA");
                            resolvedSpans = Collections.emptyMap();
                        }

                        var spansWithResolvedLocationMessage = new SpansWithResolvedLocationMessage("digma",
                                "SET_SPANS_DATA", resolvedSpans);

                        var stringMessage = objectMapper.writeValueAsString(spansWithResolvedLocationMessage);

                        browser.executeJavaScript(
                                "window.postMessage(" + stringMessage + ");",
                                jbCefBrowser.getCefBrowser().getURL(),
                                0
                        );
                    }
                    case "GO_TO_SPAN" -> {
                        GoToSpanMessage goToSpanMessage = objectMapper.treeToValue(jsonNode, GoToSpanMessage.class);
                        JaegerUIService.getInstance(project).goToSpanAndNavigateToCode(goToSpanMessage);
                    }
                    case "GO_TO_INSIGHTS" -> {
                        //it's the same message as go to span
                        GoToSpanMessage goToSpanMessage = objectMapper.treeToValue(jsonNode, GoToSpanMessage.class);
                        JaegerUIService.getInstance(project).goToInsight(goToSpanMessage);
                    }
                    case JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        OpenInDefaultBrowserRequest openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInDefaultBrowserRequest.class);
                        if (openBrowserRequest != null && openBrowserRequest.getPayload() != null) {
                            BrowserUtil.browse(openBrowserRequest.getPayload().getUrl());
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

                stopWatchStop(stopWatch, time -> Log.log(LOGGER::trace, "action {} took {}",action, time));

            } catch (Throwable e) {
                Log.debugWithException(LOGGER,e,"Exception in onQuery "+request);
                ErrorReporter.getInstance().reportError("JaegerUIMessageRouterHandler.onQuery", e);
            }
        });

        callback.success("");

        return true;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(LOGGER::debug,"jcef query canceled");
    }

}
