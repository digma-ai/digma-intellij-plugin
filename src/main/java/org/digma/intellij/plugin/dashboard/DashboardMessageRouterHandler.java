package org.digma.intellij.plugin.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.*;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.dashboard.incoming.GoToSpan;
import org.digma.intellij.plugin.dashboard.outgoing.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.jcef.model.*;

import java.util.*;

import static org.digma.intellij.plugin.common.StopWatchUtilsKt.*;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.*;

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

        var objectMapper = new ObjectMapper();
        Backgroundable.executeOnPooledThread( () -> {
            try {

                var stopWatch = stopWatchStart();


                var jsonNode = objectMapper.readTree(request);
                String action = jsonNode.get("action").asText();

                switch (action) {
                    case "DASHBOARD/INITIALIZE" -> onInitialize(browser);
                    case JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        OpenInDefaultBrowserRequest openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInDefaultBrowserRequest.class);
                        if (openBrowserRequest != null && openBrowserRequest.getPayload() != null) {
                            BrowserUtil.browse(openBrowserRequest.getPayload().getUrl());
                        }
                    }
                    case "DASHBOARD/GET_DATA" -> {

                        Map<String, Object> mapRequest = objectMapper.convertValue(jsonNode, Map.class);
                        Map<String, Object> payload = (Map<String, Object>) mapRequest.get("payload");

                        Map<String,String> backendQueryParams = new HashMap<>();
                        // main parameters
                        backendQueryParams.put("environment", payload.get("environment").toString());
                        var dashboardType = payload.get("type").toString();
                        backendQueryParams.put("type", dashboardType);
                        // query parameters
                        Map<String, Object> payloadQueryParams = (Map<String, Object>) payload.get("query");
                        payloadQueryParams.forEach((paramKey, paramValue) -> {
                            backendQueryParams.put(paramKey, paramValue.toString());
                        });

                        try {
                            var dashboardJson = DashboardService.getInstance(project).getDashboard(backendQueryParams);
                            Log.log(logger::trace, project, "got dashboard data {}", dashboardJson);
                            var backendPayload = objectMapper.readTree(dashboardJson);
                            var message = new DashboardData("digma", "DASHBOARD/SET_DATA", backendPayload);
                            Log.log(logger::debug, project, "sending DASHBOARD/SET_DATA message");

                            executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message));
                            ActivityMonitor.getInstance(project).registerSubDashboardViewed(dashboardType);
                        } catch (AnalyticsServiceException e) {
                            var errorCode = e.getErrorCode();
                            var errorMessage = e.getMeaningfulMessage();
                            if (errorCode == 404) {
                                errorMessage = "Digma analysis backend version is outdated. Please update.";
                            }
                            Log.warnWithException(logger, e, "error setting dashboard data");
                            var dashboardError = new DashboardError(null, new ErrorPayload(errorMessage), dashboardType);
                            var errorJsonNode = objectMapper.convertValue(dashboardError, JsonNode.class);
                            var message = new DashboardData("digma", "DASHBOARD/SET_DATA", errorJsonNode);
                            Log.log(logger::trace, project, "sending DASHBOARD/SET_DATA message with error");
                            ErrorReporter.getInstance().reportError(project, "DashboardMessageRouterHandler.SET_DATA", e);
                            try {
                                executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message));
                            } catch (JsonProcessingException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    case "DASHBOARD/GO_TO_SPAN" -> {
                        GoToSpan goToSpan = objectMapper.treeToValue(jsonNode, GoToSpan.class);
                        DashboardService.getInstance(project).goToSpanAndNavigateToCode(goToSpan);
                    }
                    case "GLOBAL/GET_BACKEND_INFO" -> {
                        //do nothing, dashboard app sends that for some reason but it's not necessary
                    }

                    default -> throw new IllegalStateException("Unexpected value: " + action);
                }

                stopWatchStop(stopWatch, time -> Log.log(logger::trace, "action {} took {}",action, time));

            }

            catch (Exception e) {
                Log.warnWithException(logger, e, "error setting dashboard data");
                var jsonNode = objectMapper.convertValue(new Payload(null, new ErrorPayload(e.toString())), JsonNode.class);
                var message = new DashboardData("digma", "DASHBOARD/SET_DATA", jsonNode);
                Log.log(logger::trace, project, "sending DASHBOARD/SET_DATA message with error");
                ErrorReporter.getInstance().reportError(project, "DashboardMessageRouterHandler.SET_DATA", e);
                try {
                    executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }

            }
        });

        callback.success("");

        return true;
    }

    private void onInitialize(CefBrowser browser) {
        try {
            AboutResult about = AnalyticsService.getInstance(project).getAbout();
            var message = new BackendInfoMessage(about);

            Log.log(logger::trace, project, "sending {} message",JCefMessagesUtils.GLOBAL_SET_BACKEND_INFO);
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, e, "error getting backend info");
        }
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(logger::debug, "jcef query canceled");
    }

}
