package org.digma.intellij.plugin.toolwindow.recentactivity;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.Disposable;
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
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.JsonUtils;
import org.digma.intellij.plugin.icons.AppIcons;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanForTracePayload;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanPayload;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityGoToSpanRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityGoToTraceRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.recentactivity.RecentActivityLogic;
import org.digma.intellij.plugin.service.EditorService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.toolwindow.common.CustomViewerWindow;
import org.digma.intellij.plugin.toolwindow.common.JaegerUrlChangedPayload;
import org.digma.intellij.plugin.toolwindow.common.JaegerUrlChangedRequest;
import org.digma.intellij.plugin.toolwindow.common.ThemeChangeListener;
import org.digma.intellij.plugin.toolwindow.recentactivity.incoming.CloseLiveViewMessage;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.digma.intellij.plugin.navigation.codeless.CodelessNavigationKt.showInsightsForSpan;
import static org.digma.intellij.plugin.recentactivity.RecentActivityLogic.RECENT_EXPIRATION_LIMIT_MILLIS;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_IS_JAEGER_ENABLED;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_CLOSE_LIVE_VIEW;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GO_TO_SPAN;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GO_TO_TRACE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_INITIALIZE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_SET_DATA;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.REQUEST_MESSAGE_TYPE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.parseJsonToObject;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.LOCAL_ENV;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.SUFFIX_OF_LOCAL;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.getSortedEnvironments;
import static org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt.openJaegerFromRecentActivity;


/**
 * Digma tool window inside bottom panel
 */
public class DigmaBottomToolWindowFactory implements ToolWindowFactory, Disposable {
    private static final Logger LOGGER = Logger.getInstance(DigmaBottomToolWindowFactory.class);
    private static final String DIGMA_SIDE_PANE_TOOL_WINDOW_NAME = "Digma";
    private static final String RESOURCE_FOLDER_NAME = "recentactivity";
    private static final String RECENT_EXPIRATION_LIMIT_VARIABLE = "recentActivityExpirationLimit";
    private static final int FETCHING_LOOP_INTERVAL = 10 * 1000; // 10sec

    private final Icon icon = AppIcons.TOOL_WINDOW_OBSERVABILITY;
    private final Icon iconWithGreenDot = ExecutionUtil.getLiveIndicator(icon);
    private final Timer timer = new Timer();

    private AnalyticsService analyticsService;
    private String localHostname;
    private ToolWindow toolWindow;
    private JBCefBrowser jbCefBrowser;
    private RecentActivityResult latestActivityResult;
    private boolean webAppInitialized;

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        var project = toolWindow.getProject();

        this.toolWindow = toolWindow;
        //initialize AnalyticsService early so the UI already can detect the connection status when created
        this.analyticsService = project.getService(AnalyticsService.class);
        this.localHostname = CommonUtils.getLocalHostname();
        this.latestActivityResult = new RecentActivityResult(null, new ArrayList<>());

        var activityFetchingTask = new TimerTask() {
            @Override
            public void run() {
                fetchRecentActivities();
            }
        };
        timer.scheduleAtFixedRate(activityFetchingTask, 0, FETCHING_LOOP_INTERVAL);
    }

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

        RecentActivityToolWindowShower.getInstance(project).setToolWindow(toolWindow);
        var contentFactory = ContentFactory.getInstance();
        var codeAnalyticsTab = createCodeAnalyticsTab(project, toolWindow, contentFactory);
        if (codeAnalyticsTab != null) {
            toolWindow.getContentManager().addContent(codeAnalyticsTab);
        }

        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it
        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
        if (backendConnectionMonitor.isConnectionOk()) {
            Backgroundable.ensureBackground(project, "change environment", () -> {
                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
                publisher.environmentChanged(analyticsService.getEnvironment().getCurrent(), true);
            });
        }

    }

    private Content createCodeAnalyticsTab(Project project, ToolWindow toolWindow, ContentFactory contentFactory) {
        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            return null;
        }
        var editorService = project.getService(EditorService.class);
        var customViewerWindow = new CustomViewerWindow(project, RESOURCE_FOLDER_NAME, new HashMap<>() {{
            put(RECENT_EXPIRATION_LIMIT_VARIABLE, RECENT_EXPIRATION_LIMIT_MILLIS);
        }});
        jbCefBrowser = customViewerWindow.getWebView();

        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

        jbCefBrowser.getCefBrowser().setFocus(true);

        RecentActivityService.getInstance(project).setJcefBrowser(jbCefBrowser);

        CefMessageRouter msgRouter = CefMessageRouter.create();

        ThemeChangeListener listener = new ThemeChangeListener(jbCefBrowser);
        UIManager.addPropertyChangeListener(listener);

        SettingsState.getInstance().addChangeListener(settingsState1 -> sendRequestToChangeTraceButtonDisplaying(jbCefBrowser));

        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                Log.log(LOGGER::debug, "request: {}", request);
                JcefMessageRequest reactMessageRequest = parseJsonToObject(request, JcefMessageRequest.class);
                if (RECENT_ACTIVITY_INITIALIZE.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    processRecentActivityInitialized();
                    RecentActivityService.getInstance(project).runInitTask();
                }
                if (RECENT_ACTIVITY_GO_TO_SPAN.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    RecentActivityGoToSpanRequest recentActivityGoToSpanRequest = parseJsonToObject(request, RecentActivityGoToSpanRequest.class);
                    processRecentActivityGoToSpanRequest(recentActivityGoToSpanRequest.getPayload(), project, editorService);
                }
                if (RECENT_ACTIVITY_GO_TO_TRACE.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    RecentActivityGoToTraceRequest recentActivityGoToTraceRequest = parseJsonToObject(request, RecentActivityGoToTraceRequest.class);
                    processRecentActivityGoToTraceRequest(recentActivityGoToTraceRequest, project);
                }
                if (RECENT_ACTIVITY_CLOSE_LIVE_VIEW.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    try {
                        CloseLiveViewMessage closeLiveViewMessage = JsonUtils.stringToJavaRecord(request, CloseLiveViewMessage.class);
                        RecentActivityService.getInstance(project).liveViewClosed(closeLiveViewMessage);
                    } catch (Exception e) {
                        //we can't miss the close message because then the live view will stay open.
                        // close the live view even if there is an error parsing the message.
                        Log.debugWithException(LOGGER, project, e, "Exception while parsing CloseLiveViewMessage {}", e.getMessage());
                        RecentActivityService.getInstance(project).liveViewClosed(null);
                    }
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

        return contentFactory.createContent(jcefDigmaPanel, null, false);
    }

    private void processRecentActivityInitialized() {
        webAppInitialized = true;
        List<String> allEnvironments = analyticsService.getEnvironment().getEnvironments();
        sendLatestActivities(allEnvironments);
    }

    private void processRecentActivityGoToSpanRequest(RecentActivityEntrySpanPayload payload, Project project, EditorService editorService) {
        if (payload != null) {
            String methodCodeObjectId = payload.getSpan().getMethodCodeObjectId();

            ApplicationManager.getApplication().invokeLater(() -> {

                LanguageService languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId);
                Map<String, Pair<String, Integer>> workspaceUrisForMethodCodeObjectIds = languageService.findWorkspaceUrisForMethodCodeObjectIds(Collections.singletonList(methodCodeObjectId));
                final Pair<String, Integer> fileAndOffset = workspaceUrisForMethodCodeObjectIds.get(methodCodeObjectId);

                if (fileAndOffset == null) {

                    // modifying the selected environment
                    EnvironmentsSupplier environmentsSupplier = analyticsService.getEnvironment();
                    String actualEnvName = adjustBackEnvNameIfNeeded(payload.getEnvironment());
                    environmentsSupplier.setCurrent(actualEnvName,false, () -> {
                        NotificationUtil.showNotification(project, "code object could not be found in the workspace");
                        showInsightsForSpan(project, payload.getSpan().getSpanCodeObjectId(), payload.getSpan().getMethodCodeObjectId());
                    });

                }else{
                    editorService.openWorkspaceFileInEditor(fileAndOffset.getFirst(), fileAndOffset.getSecond());

                    // modifying the selected environment
                    EnvironmentsSupplier environmentsSupplier = analyticsService.getEnvironment();
                    String actualEnvName = adjustBackEnvNameIfNeeded(payload.getEnvironment());
                    environmentsSupplier.setCurrent(actualEnvName,true);


                    ToolWindow digmaSidePaneToolWindow = ToolWindowManager.getInstance(project).getToolWindow(DIGMA_SIDE_PANE_TOOL_WINDOW_NAME);
                    if (digmaSidePaneToolWindow != null && !digmaSidePaneToolWindow.isVisible()) {
                        digmaSidePaneToolWindow.show();
                    } else {
                        Log.log(LOGGER::debug, "digmaSidePaneToolWindow is empty OR is visible already");
                    }
                }
            });
        }
    }

    private void processRecentActivityGoToTraceRequest(RecentActivityGoToTraceRequest recentActivityGoToTraceRequest, Project project) {
        RecentActivityEntrySpanForTracePayload payload = recentActivityGoToTraceRequest.getPayload();
        if (payload != null) {
            openJaegerFromRecentActivity(project, payload.getTraceId(), payload.getSpan().getScopeId());
        } else {
            Log.log(LOGGER::debug, "processRecentActivityGoToTraceRequest payload is empty");
        }
    }

    private void fetchRecentActivities() {
        List<String> allEnvironments = analyticsService.getEnvironments();
        if (allEnvironments == null) {
            Log.log(LOGGER::warn, "error while getting environments from server");
            return;
        }
        RecentActivityResult recentActivityData = null;
        try {
            recentActivityData = analyticsService.getRecentActivity(allEnvironments);
        } catch (AnalyticsServiceException e) {
            Log.log(LOGGER::warn, "AnalyticsServiceException for getRecentActivity: {}", e.getMessage());
        }

        if (recentActivityData != null) {
            latestActivityResult = recentActivityData;

            // Tool window may not be opened yet
            if (webAppInitialized) {
                sendLatestActivities(allEnvironments);
            }
        }

        if (hasRecentActivity()) {
            showGreenDot();
        } else {
            hideGreenDot();
        }
    }

    private void sendLatestActivities(List<String> allEnvironments) {
        List<String> sortedEnvironments = getSortedEnvironments(allEnvironments, localHostname);
        String requestMessage = JBCefBrowserUtil.resultToString(new JcefMessageRequest(
                REQUEST_MESSAGE_TYPE,
                RECENT_ACTIVITY_SET_DATA,
                new JcefMessagePayload(
                        sortedEnvironments,
                        getEntriesWithAdjustedLocalEnvs(latestActivityResult)
                )
        ));

        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }

    private boolean hasRecentActivity() {
        var latestActivity = latestActivityResult.getEntries().stream()
                .map(RecentActivityResponseEntry::getLatestTraceTimestamp)
                .max(Date::compareTo);

        return latestActivity.isPresent() &&
                RecentActivityLogic.isRecentTime(latestActivity.get());
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
        return environment.toUpperCase().endsWith(SUFFIX_OF_LOCAL) ? LOCAL_ENV : environment;
    }

    private String adjustBackEnvNameIfNeeded(String environment) {
        if (environment.equalsIgnoreCase(LOCAL_ENV)) {
            return (localHostname + SUFFIX_OF_LOCAL).toUpperCase();
        }
        return environment;
    }

    private void sendRequestToChangeTraceButtonDisplaying(JBCefBrowser jbCefBrowser) {
        String requestMessage = JBCefBrowserUtil.resultToString(
                new JaegerUrlChangedRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_IS_JAEGER_ENABLED,
                        new JaegerUrlChangedPayload(JaegerUtilKt.isJaegerButtonEnabled())
                ));
        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }

    private void showGreenDot() {
        EDT.ensureEDT(() -> {
            toolWindow.setIcon(iconWithGreenDot);
        });
    }

    private void hideGreenDot() {
        EDT.ensureEDT(() -> {
            toolWindow.setIcon(icon);
        });
    }

    @Override
    public void dispose() {
        timer.cancel();
    }
}
