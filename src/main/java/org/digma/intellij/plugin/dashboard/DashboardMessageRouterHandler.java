package org.digma.intellij.plugin.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.*;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.dashboard.outgoing.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.dashboard.reports.model.*;
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler;
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.digma.intellij.plugin.common.JsonUtilsKt.objectNodeToMap;
import static org.digma.intellij.plugin.ui.jcef.JCEFUtilsKt.getMapFromNode;
import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;

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

            case "DASHBOARD/GET_SERVICES" -> {
                pushServices(browser, requestJsonNode);
            }
            case "DASHBOARD/GET_REPORT_ISSUES_STATS" -> {
                pushIssuesReportStats(browser, requestJsonNode);
            }
            case "DASHBOARD/GET_REPORT_ASSETS_STATS" -> {
                pushAssetsReportStats(browser, requestJsonNode);
            }
            case "DASHBOARD/GET_METRICS_REPORT_DATA" -> {
                pushMetricsData(browser, requestJsonNode);
            }
            case "DASHBOARD/GET_SERVICE_ENDPOINTS" -> {
                 pushEndpoints(browser, requestJsonNode);
            }
            case "DASHBOARD/GET_ENDPOINTS_ISSUES" -> {
                pushEndpointsIssues(browser, requestJsonNode);
            }
            case "DASHBOARD/GET_SERVICE_ENVIRONMENTS" -> {
                pushEnvironmentsData(browser, requestJsonNode);
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


    private void pushServices(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        Log.log(logger::trace, project, "pushServices called");
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        var env = requestPayload.get("environment").textValue();
        try {
            var payload = AnalyticsService.getInstance(project).getServices(env);
            var message = new SetServicesMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_SERVICES message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_SERVICES message with error");
        }
    }

    private void pushEndpoints(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        Log.log(logger::trace, project, "pushEndpoints called");
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        var backendQueryParams = getMapFromNode(requestPayload, getObjectMapper());
        var service = requestPayload.get("service").textValue();
        try {
            var payload = AnalyticsService.getInstance(project).getEndpoints(service, backendQueryParams);
            var message = new SetEndpointsMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_SERVICE_ENDPOINTS message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_SERVICE_ENDPOINTS message with error");
        }
    }

    private void pushEndpointsIssues(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        Log.log(logger::trace, project, "pushEndpointsIssues called");
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        try {
            var payload = AnalyticsService.getInstance(project).getEndpointIssues(requestPayload.toString());
            var message = new SetEndpointIssuesMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_ENDPOINTS_ISSUES message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_ENDPOINTS_ISSUES message with error");
        }
    }

    private void pushAssetsReportStats(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        var backendQueryParams = getMapFromNode(requestPayload, getObjectMapper());
        Log.log(logger::trace, project, "pushAssetsReportStats called");
        try {
            var payload = AnalyticsService.getInstance(project).getAssetsReportStats(backendQueryParams);
            var message = new SetAssetsReportStatsMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_REPORT_ASSETS_STATS message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_REPORT_ASSETS_STATS message with error");
        }
    }

    private void pushIssuesReportStats(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        var backendQueryParams = getMapFromNode(requestPayload, getObjectMapper());
        Log.log(logger::trace, project, "pushIssuesReportStats called");
        try {
            var payload = AnalyticsService.getInstance(project).getIssuesReportStats(backendQueryParams);
            var message = new SetIssuesReportStatsMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_REPORT_ISSUES_STATS message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_REPORT_ISSUES_STATS message with error");
        }
    }

    private void pushMetricsData(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        Log.log(logger::trace, project, "pushMetricsData called");
        try {
            var payload = AnalyticsService.getInstance(project).getServiceReport(requestPayload.toString());
            var message = new SetMetricsReportMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_METRICS_REPORT_DATA message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_METRICS_REPORT_DATA message with error");
        }
    }

    private void pushEnvironmentsData(CefBrowser browser, JsonNode requestJsonNode) {
        var project = getProject();
        var requestPayload = getPayloadFromRequestNonNull(requestJsonNode);
        var service = requestPayload.get("service").textValue();
        Log.log(logger::trace, project, "pushEnvironmentsData called");
        try {
            var payload = AnalyticsService.getInstance(project).getEnvironmentsByService(service);
            var message = new SetEnvironmentsMessage(payload);
            Log.log(logger::trace, project, "sending DASHBOARD/SET_SERVICE_ENVIRONMENTS message");
            serializeAndExecuteWindowPostMessageJavaScript(browser, message);
        } catch (AnalyticsServiceException ex) {
            Log.log(logger::trace, getProject(), "sending DASHBOARD/SET_SERVICE_ENVIRONMENTS message with error");
        }
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
            var errorMessage = e.getNonNullMessage();
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
