package org.digma.intellij.plugin.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.*;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.dashboard.incoming.GoToSpan;
import org.digma.intellij.plugin.dashboard.outgoing.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler;
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.digma.intellij.plugin.common.JsonUtilsKt.objectNodeToMap;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.*;

public class DashboardMessageRouterHandler extends BaseMessageRouterHandler {

    private final Logger logger = Logger.getInstance(this.getClass());

    public DashboardMessageRouterHandler(Project project) {
        super(project);
    }


    @NotNull
    @Override
    public String getOriginForTroubleshootingEvent() {
        return "dashboard";
    }

    @Override
    public boolean doOnQuery(@NotNull Project project, @NotNull CefBrowser browser, @NotNull JsonNode requestJsonNode, @NotNull String rawRequest, @NotNull String action) throws Exception {

        switch (action) {

            case "DASHBOARD/INITIALIZE" -> onInitialize(browser);

            case "DASHBOARD/GET_DATA" -> getData(browser, requestJsonNode);

            case "DASHBOARD/GO_TO_SPAN" -> {
                GoToSpan goToSpan = jsonToObject(requestJsonNode, GoToSpan.class);
                DashboardService.getInstance(project).goToSpan(goToSpan);
            }
            case "GLOBAL/GET_BACKEND_INFO" -> {
                //do nothing, dashboard app sends that for some reason, but it's not necessary
            }
            case "DASHBOARD/GET_ENVIRONMENT_INFO" -> {
                //do nothing, dashboard app sends that for some reason, but it's not necessary
            }

            default -> {
                return false;
            }
        }

        return true;
    }


    private void getData(CefBrowser browser, JsonNode requestJsonNode) throws JsonProcessingException {

        var payload = getPayloadFromRequestNonNull(requestJsonNode);

        Map<String, String> backendQueryParams = new HashMap<>();
        // main parameters
        backendQueryParams.put("environment", payload.get("environment").asText());
        var dashboardType = payload.get("type").asText();
        backendQueryParams.put("type", dashboardType);
        // query parameters
        Map<String, Object> payloadQueryParams = objectNodeToMap((ObjectNode) payload.get("query"));
        payloadQueryParams.forEach((paramKey, paramValue) -> backendQueryParams.put(paramKey, paramValue.toString()));

        try {
            var dashboardJson = DashboardService.getInstance(getProject()).getDashboard(backendQueryParams);
            Log.log(logger::trace, getProject(), "got dashboard data {}", dashboardJson);
            var backendPayload = getObjectMapper().readTree(dashboardJson);
            var message = new DashboardData("digma", "DASHBOARD/SET_DATA", backendPayload);
            Log.log(logger::debug, getProject(), "sending DASHBOARD/SET_DATA message");

            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
            ActivityMonitor.getInstance(getProject()).registerSubDashboardViewed(dashboardType);

        } catch (AnalyticsServiceException e) {

            Log.warnWithException(logger, e, "error setting dashboard data");
            ErrorReporter.getInstance().reportError(getProject(), "DashboardMessageRouterHandler.getData", e);

            var errorCode = e.getErrorCode();
            var errorMessage = e.getMeaningfulMessage();
            if (errorCode == 404) {
                errorMessage = "Digma analysis backend version is outdated. Please update.";
            }
            sendEmptyDataWithError(browser, errorMessage, dashboardType);
        }
    }

    private void sendEmptyDataWithError(CefBrowser browser, String errorMessage, String dashboardType) {

        var dashboardError = new DashboardError(null, new ErrorPayload(errorMessage), dashboardType);
        var errorJsonNode = getObjectMapper().convertValue(dashboardError, JsonNode.class);
        var message = new DashboardData("digma", "DASHBOARD/SET_DATA", errorJsonNode);
        Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_DATA message with error");
        serializeAndExecuteWindowPostMessageJavaScript(browser, message);
    }




    private void onInitialize(CefBrowser browser) {
        doCommonInitialize(browser);
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        Log.log(logger::debug, "jcef query canceled");
    }

}
