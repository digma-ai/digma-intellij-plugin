package org.digma.intellij.plugin.toolwindow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import kotlin.Pair;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.service.EditorService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static org.digma.intellij.plugin.toolwindow.ToolWindowUtil.*;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.LOCAL_ENV;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.getSortedEnvironments;
import static org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt.openJaegerFromRecentActivity;


/**
 * Digma tool window inside bottom panel
 */
public class DigmaBottomToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOGGER = Logger.getInstance(DigmaBottomToolWindowFactory.class);
    private static final String DIGMA_LEFT_TOOL_WINDOW_NAME = "Digma";
    private static final String SUCCESSFULLY_PROCESSED_JCEF_REQUEST_MESSAGE = "Successfully processed JCEF request with action =";
    private EditorService editorService;
    private AnalyticsService analyticsService;
    private String localHostname;

    /**
     * this is the starting point of the plugin. this method is called when the tool window is opened.
     * before the window is opened there may be no reason to do anything, listen to events for example will be
     * a waste if the user didn't open the window. at least as much as possible, some extensions will be registered
     * but will do nothing if the plugin is not active.
     * after the plugin is active all listeners and extensions are installed and kicking until the IDE is closed.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);
        this.editorService = project.getService(EditorService.class);
        //initialize AnalyticsService early so the UI already can detect the connection status when created
        this.analyticsService = project.getService(AnalyticsService.class);

        this.localHostname = CommonUtils.getLocalHostname();

        var contentFactory = ContentFactory.getInstance();

        Content codeAnalyticsTab = createCodeAnalyticsTab(project, toolWindow, contentFactory);

        if (codeAnalyticsTab != null) {
            toolWindow.getContentManager().setSelectedContent(codeAnalyticsTab, true);
        }

        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it
        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
        if (backendConnectionMonitor.isConnectionOk()) {
            Backgroundable.ensureBackground(project, "change environment", () -> {
                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
                publisher.environmentChanged(analyticsService.getEnvironment().getCurrent());
            });
        }

    }

    private Content createCodeAnalyticsTab(Project project, ToolWindow toolWindow, ContentFactory contentFactory) {
        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            return null;
        }
        var customViewerWindow = project.getService(CustomViewerWindowService.class).getCustomViewerWindow();
        JBCefBrowser jbCefBrowser = customViewerWindow.getWebView();

        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

        CefMessageRouter msgRouter = CefMessageRouter.create();

        ThemeChangeListener listener = new ThemeChangeListener(jbCefBrowser);
        UIManager.addPropertyChangeListener(listener);

        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                Log.log(LOGGER::debug, "request: {}", request);
                JcefMessageRequest reactMessageRequest = parseJsonToObject(request, JcefMessageRequest.class);
                if (RECENT_ACTIVITY_GET_DATA.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    return processRecentActivityGetDataRequest(analyticsService, jbCefBrowser, callback);
                }
                if (RECENT_ACTIVITY_GO_TO_SPAN.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    RecentActivityGoToSpanRequest recentActivityGoToSpanRequest = parseJsonToObject(request, RecentActivityGoToSpanRequest.class);
                    return processRecentActivityGoToSpanRequest(recentActivityGoToSpanRequest.getPayload(), project, callback);
                }
                if (RECENT_ACTIVITY_GO_TO_TRACE.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    RecentActivityGoToTraceRequest recentActivityGoToTraceRequest = parseJsonToObject(request, RecentActivityGoToTraceRequest.class);
                    return processRecentActivityGoToTraceRequest(recentActivityGoToTraceRequest, project, callback);
                }

                callback.success("");
                return true;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                super.onQueryCanceled(browser, frame, queryId);
            }
        }, true);

        jbCefClient.getCefClient().addMessageRouter(msgRouter);

        JPanel browserPanel = new JPanel();
        browserPanel.setLayout(new BorderLayout());
        browserPanel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);


        JPanel jcefDigmaPanel = new JPanel();
        jcefDigmaPanel.setLayout(new BorderLayout());
        jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER);

        var jcefContent = contentFactory.createContent(jcefDigmaPanel, null, false);

        toolWindow.getContentManager().addContent(jcefContent);
        return jcefContent;
    }

    private boolean processRecentActivityGoToSpanRequest(RecentActivityEntrySpanPayload payload, Project project, CefQueryCallback callback) {
        if (payload != null) {
            String methodCodeObjectId = payload.getSpan().getMethodCodeObjectId();

            ApplicationManager.getApplication().invokeLater(() -> {
                LanguageService languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId);
                Map<String, Pair<String, Integer>> workspaceUrisForMethodCodeObjectIds = languageService.findWorkspaceUrisForMethodCodeObjectIds(Collections.singletonList(methodCodeObjectId));
                if (workspaceUrisForMethodCodeObjectIds.containsKey(methodCodeObjectId)) {
                    Pair<String, Integer> result = workspaceUrisForMethodCodeObjectIds.get(methodCodeObjectId);
                    editorService.openWorkspaceFileInEditor(result.getFirst(), result.getSecond());
                    ToolWindow digmaLeftToolWindow = ToolWindowManager.getInstance(project).getToolWindow(DIGMA_LEFT_TOOL_WINDOW_NAME);
                    if (digmaLeftToolWindow != null && !digmaLeftToolWindow.isVisible()) {
                        digmaLeftToolWindow.show();
                    } else {
                        Log.log(LOGGER::debug, "digmaLeftToolWindow is empty OR is visible already");
                    }
                }
            });
            callback.success(SUCCESSFULLY_PROCESSED_JCEF_REQUEST_MESSAGE + " RECENT_ACTIVITY_GO_TO_SPAN at " + new Date());
            return true;
        } else {
            return false;
        }
    }

    private boolean processRecentActivityGoToTraceRequest(RecentActivityGoToTraceRequest recentActivityGoToTraceRequest, Project project, CefQueryCallback callback) {
        RecentActivityEntrySpanForTracePayload payload = recentActivityGoToTraceRequest.getPayload();
        if (payload != null) {
            openJaegerFromRecentActivity(project, payload.getTraceId(), payload.getSpan().getScopeId());
        } else {
            Log.log(LOGGER::debug, "processRecentActivityGoToTraceRequest payload is empty");
        }
        callback.success(SUCCESSFULLY_PROCESSED_JCEF_REQUEST_MESSAGE + " RECENT_ACTIVITY_GO_TO_TRACE at " + new Date());
        return true;
    }

    private boolean processRecentActivityGetDataRequest(AnalyticsService analyticsService, JBCefBrowser jbCefBrowser, CefQueryCallback callback) {
        List<String> allEnvironments = analyticsService.getEnvironments();
        List<String> sortedEnvironments = getSortedEnvironments(allEnvironments, localHostname);
        RecentActivityResult recentActivityData = null;
        try {
            recentActivityData = analyticsService.getRecentActivity(allEnvironments);
        } catch (AnalyticsServiceException e) {
            Log.log(LOGGER::debug, "AnalyticsServiceException for getRecentActivity: {}", e.getMessage());
        }
        String requestMessage = JBCefBrowserUtil.resultToString(new JcefMessageRequest(
                REQUEST_MESSAGE_TYPE,
                RECENT_ACTIVITY_SET_DATA,
                new JcefMessagePayload(
                        sortedEnvironments,
                        getEntriesWithAdjustedLocalEnvs(recentActivityData)
                )
        ));

        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);

        callback.success(SUCCESSFULLY_PROCESSED_JCEF_REQUEST_MESSAGE + " RECENT_ACTIVITY_SET_DATA at " + new Date());
        return true;
    }

    private List<RecentActivityResponseEntry> getEntriesWithAdjustedLocalEnvs(RecentActivityResult recentActivityData) {
        return recentActivityData != null ? recentActivityData.getEntries().stream()
                .map(f -> new RecentActivityResponseEntry(
                        getAdjustedEnvName(f.getEnvironment()),
                        f.getTraceFlowDisplayName(),
                        f.getFirstEntrySpan(),
                        f.getLastEntrySpan(),
                        f.getLatestTraceId(),
                        f.getLatestTraceTimestamp(),
                        f.getLatestTraceDuration(),
                        f.getSlimAggregatedInsights())
                )
                .toList() : new ArrayList<>();
    }

    private String getAdjustedEnvName(String environment) {
        return environment.toUpperCase().endsWith("["+ LOCAL_ENV + "]") ? LOCAL_ENV: environment;
    }

    private <T> T parseJsonToObject(String jsonString, Class<T> jcefMessageRequestClass) {
        JsonObject object = JsonParser.parseString(jsonString).getAsJsonObject();
        return new Gson().fromJson(object, jcefMessageRequestClass);
    }

}
