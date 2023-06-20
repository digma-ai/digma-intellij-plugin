package org.digma.intellij.plugin.toolwindow.recentactivity;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.editor.colors.impl.FontPreferencesImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.common.JsonUtils;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.icons.AppIcons;
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanForTracePayload;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanPayload;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityGoToSpanRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityGoToTraceRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.recentactivity.RecentActivityLogic;
import org.digma.intellij.plugin.service.EditorService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.toolwindow.common.CustomSchemeHandlerFactory;
import org.digma.intellij.plugin.toolwindow.common.JaegerUrlChangedPayload;
import org.digma.intellij.plugin.toolwindow.common.JaegerUrlChangedRequest;
import org.digma.intellij.plugin.toolwindow.common.ThemeChangeListener;
import org.digma.intellij.plugin.toolwindow.common.UICodeFontRequest;
import org.digma.intellij.plugin.toolwindow.common.UIFontRequest;
import org.digma.intellij.plugin.toolwindow.common.UiCodeFontPayload;
import org.digma.intellij.plugin.toolwindow.common.UiFontPayload;
import org.digma.intellij.plugin.toolwindow.recentactivity.incoming.CloseLiveViewMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataPayload;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

import static org.digma.intellij.plugin.common.EnvironmentUtilKt.LOCAL_ENV;
import static org.digma.intellij.plugin.common.EnvironmentUtilKt.SUFFIX_OF_LOCAL;
import static org.digma.intellij.plugin.common.EnvironmentUtilKt.getSortedEnvironments;
import static org.digma.intellij.plugin.recentactivity.RecentActivityLogic.RECENT_EXPIRATION_LIMIT_MILLIS;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_IS_JAEGER_ENABLED;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_UI_CODE_FONT;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_UI_MAIN_FONT;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_CLOSE_LIVE_VIEW;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GO_TO_SPAN;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GO_TO_TRACE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_INITIALIZE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_SET_DATA;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_SET_LIVE_DATA;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.REQUEST_MESSAGE_TYPE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.parseJsonToObject;
import static org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt.openJaegerFromRecentActivity;
import static org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt.traceButtonName;

public class RecentActivityService implements Disposable {

    private static final String DIGMA_SIDE_PANE_TOOL_WINDOW_NAME = "Digma";
    private static final String RESOURCE_FOLDER_NAME = "recentactivity";
    private static final String RECENT_EXPIRATION_LIMIT_VARIABLE = "recentActivityExpirationLimit";
    private static final int FETCHING_LOOP_INTERVAL = 10 * 1000; // 10sec

    private final Logger logger = Logger.getInstance(RecentActivityService.class);
    private final Icon icon = AppIcons.TOOL_WINDOW_OBSERVABILITY;
    private final Icon iconWithGreenDot = ExecutionUtil.getLiveIndicator(icon);
    private final String localHostname;
    private final Project project;
    private final AnalyticsService analyticsService;

    //the recent activity code is not managed in one place that is accessible from the plugin code
    // like a project service, so currently need an init task.
    // it is used to send live data in case the live view button was clicked before the recent activity window was initialized.
    private MyInitTask initTask;
    private Timer myLiveDataTimer;
    private Timer activityFetchingTimer;
    private JBCefBrowser jbCefBrowser;
    private CefMessageRouter msgRouter;
    private RecentActivityResult latestActivityResult;
    private boolean webAppInitialized;

    public static RecentActivityService getInstance(Project project) {
        return project.getService(RecentActivityService.class);
    }

    public RecentActivityService(Project project) {
        this.project = project;
        //initialize AnalyticsService early so the UI already can detect the connection status when created
        this.analyticsService = project.getService(AnalyticsService.class);
        this.localHostname = CommonUtils.getLocalHostname();
        this.latestActivityResult = new RecentActivityResult(null, new ArrayList<>());
    }

    public void startFetchingActivities() {
        if (activityFetchingTimer != null)
            return;

        var activityFetchingTask = new TimerTask() {
            @Override
            public void run() {
                fetchRecentActivities();
            }
        };
        activityFetchingTimer = new Timer();
        activityFetchingTimer.schedule(activityFetchingTask, 0, FETCHING_LOOP_INTERVAL);
    }

    public Content createTabContent() {
        if (!JBCefApp.isSupported()) {
            Log.log(logger::warn, "JBCefApp is not supported");
            return null;
        }

        var contentFactory = ContentFactory.getInstance();
        var editorService = project.getService(EditorService.class);

        jbCefBrowser = JBCefBrowserBuilderCreator.create()
                .setUrl("https://"+RESOURCE_FOLDER_NAME+"/index.html")
                .build();
        CefApp.getInstance()
                .registerSchemeHandlerFactory(
                        "https",
                        RESOURCE_FOLDER_NAME,
                        new CustomSchemeHandlerFactory(RESOURCE_FOLDER_NAME, new HashMap<>() {{
                            put(RECENT_EXPIRATION_LIMIT_VARIABLE, RECENT_EXPIRATION_LIMIT_MILLIS);
                        }})
                );
        jbCefBrowser.getCefBrowser().setFocus(true);

//        var customViewerWindow = new CustomViewerWindow(project, RESOURCE_FOLDER_NAME, new HashMap<>() {{
//            put(RECENT_EXPIRATION_LIMIT_VARIABLE, RECENT_EXPIRATION_LIMIT_MILLIS);
//        }});
//        jbCefBrowser = customViewerWindow.getWebView();

        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

        msgRouter = CefMessageRouter.create();

        ThemeChangeListener listener = new ThemeChangeListener(jbCefBrowser);
        UIManager.addPropertyChangeListener(listener);

        jbCefBrowser.getComponent().addPropertyChangeListener(evt -> {
            if (project.isDisposed()) return;
            if ("font".equals(evt.getPropertyName())) {
                changeFont(jbCefBrowser);
            }
        });

        //todo: temporary, this is a very bad way to do it, waiting for help from jetbrains developers
        var fontPreferences =  AppEditorFontOptions.getInstance().getFontPreferences();
        if (fontPreferences instanceof FontPreferencesImpl){
            ((FontPreferencesImpl)AppEditorFontOptions.getInstance().getFontPreferences()).addChangeListener(e -> {
                changeCodeFont();
            },this);
        }

        SettingsState.getInstance().addChangeListener(settingsState1 -> sendRequestToChangeTraceButtonDisplaying(jbCefBrowser));

        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                Log.log(logger::debug, "request: {}", request);
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
                    ActivityMonitor.getInstance(project).registerButtonClicked(traceButtonName, "recent-activity");
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
                        Log.debugWithException(logger, project, e, "Exception while parsing CloseLiveViewMessage {}", e.getMessage());
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
        java.util.List<String> allEnvironments = analyticsService.getEnvironment().getEnvironments();
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
                    environmentsSupplier.setCurrent(actualEnvName, false, () -> {
                        NotificationUtil.showNotification(project, "code object could not be found in the workspace");
                        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(payload.getSpan().getSpanCodeObjectId());
                    });

                } else {
                    editorService.openWorkspaceFileInEditor(fileAndOffset.getFirst(), fileAndOffset.getSecond());

                    // modifying the selected environment
                    EnvironmentsSupplier environmentsSupplier = analyticsService.getEnvironment();
                    String actualEnvName = adjustBackEnvNameIfNeeded(payload.getEnvironment());
                    environmentsSupplier.setCurrent(actualEnvName, true);


                    ToolWindow digmaSidePaneToolWindow = ToolWindowManager.getInstance(project).getToolWindow(DIGMA_SIDE_PANE_TOOL_WINDOW_NAME);
                    if (digmaSidePaneToolWindow != null && !digmaSidePaneToolWindow.isVisible()) {
                        digmaSidePaneToolWindow.show();
                    } else {
                        Log.log(logger::debug, "digmaSidePaneToolWindow is empty OR is visible already");
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
            Log.log(logger::debug, "processRecentActivityGoToTraceRequest payload is empty");
        }
    }

    private void fetchRecentActivities() {
        java.util.List<String> allEnvironments = analyticsService.getEnvironments();
        if (allEnvironments == null) {
            Log.log(logger::warn, "error while getting environments from server");
            return;
        }
        RecentActivityResult recentActivityData = null;
        try {
            recentActivityData = analyticsService.getRecentActivity(allEnvironments);
        } catch (AnalyticsServiceException e) {
            Log.log(logger::warn, "AnalyticsServiceException for getRecentActivity: {}", e.getMessage());
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

    private void sendLatestActivities(java.util.List<String> allEnvironments) {
        java.util.List<String> sortedEnvironments = getSortedEnvironments(allEnvironments, localHostname);
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
        var toolWindow = getToolWindowOrNull();
        if (toolWindow == null)
            return;

        EDT.ensureEDT(() -> toolWindow.setIcon(iconWithGreenDot));
    }

    private void hideGreenDot() {
        var toolWindow = getToolWindowOrNull();
        if (toolWindow == null)
            return;

        EDT.ensureEDT(() -> toolWindow.setIcon(icon));
    }

    private ToolWindow getToolWindowOrNull() {
        return ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID);
    }

    public void sendLiveData(@NotNull DurationLiveData durationLiveData, @NotNull String codeObjectId) {

        Log.log(logger::debug, project, "Got sendLiveData request for {}", codeObjectId);

        stopLiveDataTimerTask();

        if (jbCefBrowser == null) {
            Log.log(logger::debug, project, "jbCefBrowser is not initialized, calling showToolWindow");
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            //ugly hack for initialization when RECENT_ACTIVITY_INITIALIZE message is sent.
            // if the recent activity window was not yet initialized then we need to send the live data only after
            // RECENT_ACTIVITY_INITIALIZE message is sent.
            initTask = new MyInitTask(codeObjectId) {
                @Override
                public void run() {
                    sendLiveDataImpl(durationLiveData);
                }
            };
        } else {
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            sendLiveDataImpl(durationLiveData);
            startNewLiveDataTimerTask(codeObjectId);
        }
    }


    private void sendLiveDataImpl(DurationLiveData durationLiveData) {

        //should not happen but we need to check
        if (durationLiveData.getDurationData() == null) {
            Log.log(logger::debug, project, "durationLiveData.getDurationData is null, not sending live data for {}", durationLiveData);
            return;
        }

        Log.log(logger::debug, project, "sending live data for {}", durationLiveData.getDurationData().getCodeObjectId());
        LiveDataMessage liveDataMessageMessage =
                new LiveDataMessage("digma", RECENT_ACTIVITY_SET_LIVE_DATA,
                        new LiveDataPayload(durationLiveData.getLiveDataRecords(), durationLiveData.getDurationData()));
        try {
            var strMessage = JsonUtils.javaRecordToJsonString(liveDataMessageMessage);
            JBCefBrowserUtil.postJSMessage(strMessage, jbCefBrowser);
        } catch (Exception e) {
            Log.debugWithException(logger, project, e, "Exception sending live data message");
        }
    }

    private void stopLiveDataTimerTask() {
        Log.log(logger::debug, project, "Stopping timer");
        if (myLiveDataTimer != null) {
            myLiveDataTimer.cancel();
            myLiveDataTimer = null;
        }
    }

    private void startNewLiveDataTimerTask(@NotNull String codeObjectId) {

        Log.log(logger::debug, project, "Starting new timer for {}", codeObjectId);

        if (myLiveDataTimer != null) {
            myLiveDataTimer.cancel();
        }
        myLiveDataTimer = new Timer();
        myLiveDataTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    DurationLiveData newDurationLiveData = AnalyticsService.getInstance(project).getDurationLiveData(codeObjectId);
                    if (newDurationLiveData.getDurationData() == null) {
                        Log.log(logger::debug, project, "newDurationLiveData.getDurationData is null, stopping refresh timer for {}", codeObjectId);
                        stopLiveDataTimerTask();
                        return;
                    }
                    sendLiveDataImpl(newDurationLiveData);
                } catch (AnalyticsServiceException e) {
                    Log.debugWithException(logger, project, e, "got Exception from getDurationLiveData. Stopping refresh timer for {} {}", codeObjectId, e.getMessage());
                    stopLiveDataTimerTask();
                } catch (Exception e) {
                    //catch any other exception and rethrow because it's a bug we should fix
                    Log.debugWithException(logger, project, e, "Exception in myLiveDataTimer,Stopping refresh timer for {} {}", codeObjectId, e.getMessage());
                    stopLiveDataTimerTask();
                    throw e;
                }
            }
        }, 5000, 5000);
    }


    public void runInitTask() {
        if (initTask != null) {
            initTask.run();
            startNewLiveDataTimerTask(initTask.codeObjectId);
            initTask = null;
        }
    }

    public void liveViewClosed(@Nullable CloseLiveViewMessage closeLiveViewMessage) {

        //closeLiveViewMessage may be null if there is an error parsing the message.
        // this is a protection against errors so that the timer is always closed when user clicks the close button

        Log.log(logger::debug, project, "Stopping timer");
        if (closeLiveViewMessage != null) {
            Log.log(logger::debug, project, "Stopping timer for {}", closeLiveViewMessage.payload().codeObjectId());
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(closeLiveViewMessage.payload().codeObjectId());
            //currently not considering the codeObjectId because there is only one timer task.
            // but may be necessary in the future when there are few live views opened
        }
        stopLiveDataTimerTask();
    }

    @Override
    public void dispose() {
        if (myLiveDataTimer != null) {
            myLiveDataTimer.cancel();
        }

        if (activityFetchingTimer != null)
            activityFetchingTimer.cancel();

        if (msgRouter != null)
            msgRouter.dispose();

        if (jbCefBrowser != null)
            jbCefBrowser.dispose();
    }



    private void changeFont(JBCefBrowser jbCefBrowser) {
        String fontName = jbCefBrowser.getComponent().getFont().getFontName();
        if (StringUtils.isNotEmpty(fontName)) {
            String requestMessage = JBCefBrowserUtil.resultToString(
                    new UIFontRequest(
                            REQUEST_MESSAGE_TYPE,
                            GLOBAL_SET_UI_MAIN_FONT,
                            new UiFontPayload(fontName)
                    ));
            JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
        }
    }


    private void changeCodeFont() {
        String fontName = AppEditorFontOptions.getInstance().getFontPreferences().getFontFamily();
        if (StringUtils.isNotEmpty(fontName)) {
            String requestMessage = JBCefBrowserUtil.resultToString(
                    new UICodeFontRequest(
                            REQUEST_MESSAGE_TYPE,
                            GLOBAL_SET_UI_CODE_FONT,
                            new UiCodeFontPayload(fontName)
                    ));
            JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
        }
    }

    private abstract static class MyInitTask implements Runnable {

        private final String codeObjectId;

        public MyInitTask(@NotNull String codeObjectId) {
            this.codeObjectId = codeObjectId;
        }
    }
}
