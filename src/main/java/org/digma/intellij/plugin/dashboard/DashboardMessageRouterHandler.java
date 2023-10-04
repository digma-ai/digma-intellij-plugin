package org.digma.intellij.plugin.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.dashboard.incoming.GoToSpan;
import org.digma.intellij.plugin.dashboard.outgoing.DashboardData;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.ActivityMonitor;

import java.util.HashMap;
import java.util.Map;

import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStart;
import static org.digma.intellij.plugin.common.StopWatchUtilsKt.stopWatchStop;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.executeWindowPostMessageJavaScript;

public class DashboardMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private final Logger logger = Logger.getInstance(DashboardMessageRouterHandler.class);
    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    public DashboardMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
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
                    case "DASHBOARD/INITIALIZE" -> {

                    }
                    case "DASHBOARD/GET_DATA" -> {

                        Map<String, Object> mapRequest = objectMapper.convertValue(jsonNode, Map.class);
                        Map<String, Object> payload = (Map<String, Object>) mapRequest.get("payload");

                        Map<String,String> backendQueryParams = new HashMap<>();
                        // main parameters
                        backendQueryParams.put("environment", payload.get("environment").toString());
                        backendQueryParams.put("type", payload.get("type").toString());
                        // query parameters
                        Map<String, Object> payloadQueryParams = (Map<String, Object>) payload.get("query");
                        payloadQueryParams.forEach((paramKey, paramValue) -> {
                            backendQueryParams.put(paramKey, paramValue.toString());
                        });

                        var dashboardJson = DashboardService.getInstance(project).getDashboard(backendQueryParams);
                        Log.log(logger::trace, project, "got notifications {}", dashboardJson);
                        var backendPayload = objectMapper.readTree(dashboardJson);
                        var message = new DashboardData("digma", "DASHBOARD/SET_DATA", backendPayload);
                        Log.log(logger::debug, project, "sending DASHBOARD/SET_DATA message");
                        executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message));
                    }
                    case "DASHBOARD/GO_TO_SPAN" -> {
                        GoToSpan goToSpan = objectMapper.treeToValue(jsonNode, GoToSpan.class);
                        DashboardService.getInstance(project).goToSpanAndNavigateToCode(goToSpan);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

                stopWatchStop(stopWatch, time -> Log.log(logger::trace, "action {} took {}",action, time));

            } catch (Exception e) {
                Log.debugWithException(logger, e, "Exception in onQuery " + request);
                ErrorReporter.getInstance().reportError(project, "DashboardMessageRouterHandler.onQuery", e);
            }
        });

        callback.success("");

        return true;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(logger::debug, "jcef query canceled");
    }

}
