package org.digma.intellij.plugin.recentactivity;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;

public class RecentActivityService implements Disposable {

//    private static final String RESOURCE_FOLDER_NAME = "recentactivity";
//    private static final String RECENT_EXPIRATION_LIMIT_VARIABLE = "recentActivityExpirationLimit";
//    private static final int FETCHING_LOOP_INTERVAL = 10 * 1000; // 10sec
//    private static final Icon icon = AppIcons.TOOL_WINDOW_OBSERVABILITY;

//    private static final String ENV_VARIABLE_IDE = "ide";
//    private static final String USER_EMAIL_VARIABLE = "userEmail";
//    private static final String IS_OBSERVABILITY_ENABLED_VARIABLE = "isObservabilityEnabled";
//    private static final String IS_DOCKER_INSTALLED = "isDockerInstalled";
//    private static final String IS_DOCKER_COMPOSE_INSTALLED = "isDockerComposeInstalled";
//    private static final String IS_DIGMA_ENGINE_INSTALLED = "isDigmaEngineInstalled";
//    private static final String IS_DIGMA_ENGINE_RUNNING = "isDigmaEngineRunning";
//    private static final String IS_JAEGER_ENABLED = "isJaegerEnabled";

//    private final Logger logger = Logger.getInstance(RecentActivityService.class);
//    private final Icon iconWithGreenDot = ExecutionUtil.getLiveIndicator(icon);
//    private final String localHostname;
//    private final Project project;

    //the recent activity code is not managed in one place that is accessible from the plugin code
    // like a project service, so currently need an init task.
    // it is used to send live data in case the live view button was clicked before the recent activity window was initialized.
//    private MyInitTask initTask;
//    private Timer myLiveDataTimer;
//    private Timer activityFetchingTimer;
//    private JBCefBrowser jbCefBrowser;
//    private CefMessageRouter msgRouter;
//    private RecentActivityResult latestActivityResult;
//    private boolean webAppInitialized;

//    public static RecentActivityService getInstance(Project project) {
//        return project.getService(RecentActivityService.class);
//    }

//    public RecentActivityService(Project project) {
//        this.project = project;
//        this.localHostname = CommonUtils.getLocalHostname();
//        this.latestActivityResult = new RecentActivityResult(null, new ArrayList<>());
//    }

    public void startFetchingActivities() {
//        if (activityFetchingTimer != null)
//            return;
//
//        var activityFetchingTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
//                    try {
//                        fetchRecentActivities();
//                    } catch (Exception e) {
//                        Log.warnWithException(logger, e, "Exception in fetchRecentActivities");
//                        ErrorReporter.getInstance().reportError(project, "RecentActivityService.fetchRecentActivities", e);
//                    }
//                }
//            }
//        };
//        activityFetchingTimer = new Timer();
//        activityFetchingTimer.schedule(activityFetchingTask, 0, FETCHING_LOOP_INTERVAL);
    }

//    public Content createTabContent() {
//        if (!JBCefApp.isSupported()) {
//            Log.log(logger::warn, "JBCefApp is not supported");
//            return null;
//        }
//
//        var contentFactory = ContentFactory.getInstance();
//
//        jbCefBrowser = JBCefBrowserBuilderCreator.create()
//                .setUrl("https://" + RESOURCE_FOLDER_NAME + "/index.html")
//                .build();
//
//        var data = new HashMap<String, Object>();
//        JCefTemplateUtils.addCommonEnvVariables(data);
//        data.put(RECENT_EXPIRATION_LIMIT_VARIABLE, RECENT_EXPIRATION_LIMIT_MILLIS);

//        data.put(ENV_VARIABLE_IDE, ApplicationNamesInfo.getInstance().getProductName());
//        data.put(IS_JAEGER_ENABLED, JaegerUtilKt.isJaegerButtonEnabled());
//        var userEmail = PersistenceService.getInstance().getState().getUserEmail();
//        data.put(USER_EMAIL_VARIABLE, userEmail == null ? "" : userEmail);
//        data.put(IS_OBSERVABILITY_ENABLED_VARIABLE, PersistenceService.getInstance().getState().isAutoOtel());
//        data.put(IS_DIGMA_ENGINE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isEngineInstalled());
//        data.put(IS_DIGMA_ENGINE_RUNNING, ApplicationManager.getApplication().getService(DockerService.class).isEngineRunning(project));
//        data.put(IS_DOCKER_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());
//        data.put(IS_DOCKER_COMPOSE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());


//        var lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
//            @Override
//            public void onAfterCreated(CefBrowser browser) {
//                CefApp.getInstance()
//                        .registerSchemeHandlerFactory(
//                                "https",
//                                RESOURCE_FOLDER_NAME,
//                                new CustomSchemeHandlerFactory(RESOURCE_FOLDER_NAME, data)
//                        );
//            }
//        };

//        jbCefBrowser.getJBCefClient().addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser());
//
//        Disposer.register(this, () -> jbCefBrowser.getJBCefClient().removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser()));
//
//        jbCefBrowser.getCefBrowser().setFocus(true);
//
//        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

//        msgRouter = CefMessageRouter.create();


//        SettingsState.getInstance().addChangeListener(settingsState1 -> sendRequestToChangeTraceButtonDisplaying(jbCefBrowser), this);

//        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
//            @Override
//            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
//
//                Backgroundable.executeOnPooledThread( () -> {

//                    var stopWatch = stopWatchStart();

//                    Log.log(logger::trace, "request: {}", request);
//                    JcefMessageRequest reactMessageRequest = parseJsonToObject(request, JcefMessageRequest.class);
//                    if (RECENT_ACTIVITY_INITIALIZE.equalsIgnoreCase(reactMessageRequest.getAction())) {
//                        processRecentActivityInitialized();
//                        RecentActivityService.getInstance(project).runInitTask();
//                    }
//                    if (RECENT_ACTIVITY_GO_TO_SPAN.equalsIgnoreCase(reactMessageRequest.getAction())) {
//                        RecentActivityGoToSpanRequest recentActivityGoToSpanRequest = parseJsonToObject(request, RecentActivityGoToSpanRequest.class);
//                        processRecentActivityGoToSpanRequest(recentActivityGoToSpanRequest.getPayload(), project);
//                    }
//                    if (RECENT_ACTIVITY_GO_TO_TRACE.equalsIgnoreCase(reactMessageRequest.getAction())) {
//                        ActivityMonitor.getInstance(project).registerButtonClicked(MonitoredPanel.RecentActivity, traceButtonName);
//                        RecentActivityGoToTraceRequest recentActivityGoToTraceRequest = parseJsonToObject(request, RecentActivityGoToTraceRequest.class);
//                        processRecentActivityGoToTraceRequest(recentActivityGoToTraceRequest, project);
//                    }
//                    if (RECENT_ACTIVITY_CLOSE_LIVE_VIEW.equalsIgnoreCase(reactMessageRequest.getAction())) {
//                        try {
//                            CloseLiveViewMessage closeLiveViewMessage = JsonUtils.stringToJavaRecord(request, CloseLiveViewMessage.class);
//                            RecentActivityService.getInstance(project).liveViewClosed(closeLiveViewMessage);
//                        } catch (Exception e) {
//                            //we can't miss the close message because then the live view will stay open.
//                            // close the live view even if there is an error parsing the message.
//                            Log.debugWithException(logger, project, e, "Exception while parsing CloseLiveViewMessage {}", e.getMessage());
//                            RecentActivityService.getInstance(project).liveViewClosed(null);
//                        }
//                    }
//                    if (JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER.equalsIgnoreCase(reactMessageRequest.getAction())) {
//                        OpenInBrowserRequest openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInBrowserRequest.class);
//                        if (openBrowserRequest != null && openBrowserRequest.getPayload() != null) {
//                            BrowserUtil.browse(openBrowserRequest.getPayload().getUrl());
//                        }
//                    }
//                    if (JCefMessagesUtils.GLOBAL_OPEN_TROUBLESHOOTING_GUIDE.equalsIgnoreCase(reactMessageRequest.getAction())) {
//                        EDT.ensureEDT(() -> {
//                            ActivityMonitor.getInstance(project).registerCustomEvent("troubleshooting link clicked",Collections.singletonMap("origin","recent activity"));
//                            ToolWindowShower.getInstance(project).showToolWindow();
//                            MainToolWindowCardsController.getInstance(project).showTroubleshooting();
//                        });
//
//                    }

//                    stopWatchStop(stopWatch, time -> Log.log(logger::trace, "request {} took {}",request, time));

//                });
//
//
//                callback.success("");
//                return true;
//            }
//
//        }, true);

//        jbCefClient.getCefClient().addMessageRouter(msgRouter);
//
//        JPanel browserPanel = new JPanel();
//        browserPanel.setLayout(new BorderLayout());
//        browserPanel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
//
//
//        JPanel jcefDigmaPanel = new JPanel();
//        jcefDigmaPanel.setLayout(new BorderLayout());
//        jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER);


//        ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(new SettingsChangeListener() {
//            @Override
//            public void systemFontChange(@NotNull String fontName) {
//                JCefBrowserUtil.sendRequestToChangeFont(fontName, jbCefBrowser);
//            }
//
//            @Override
//            public void systemThemeChange(@NotNull Theme theme) {
//                JCefBrowserUtil.sendRequestToChangeUiTheme(theme, jbCefBrowser);
//            }
//
//            @Override
//            public void editorFontChange(@NotNull String fontName) {
//                JCefBrowserUtil.sendRequestToChangeCodeFont(fontName, jbCefBrowser);
//            }
//        });


//        return contentFactory.createContent(new JPanel(), null, false);
//    }

//    private void processRecentActivityInitialized() {
//        webAppInitialized = true;
//        java.util.List<String> allEnvironments = AnalyticsService.getInstance(project).getEnvironment().getEnvironments();
//        sendLatestActivities(allEnvironments);
//    }

//    private void processRecentActivityGoToSpanRequest(RecentActivityEntrySpanPayload payload, Project project) {
//        if (payload != null) {
//
//            EDT.ensureEDT(() -> {
//
//                //todo: we need to show the insights only after the environment changes. but environment change is done in the background
//                // and its not easy to sync the change environment and showing the insights.
//                // this actually comes to solve the case that the recent activity and the main environment combo
//                // are not the same one and they need to sync. when this is fixed we can remove
//                // the methods EnvironmentsSupplier.setCurrent(java.lang.String, boolean, java.lang.Runnable)
//                // changing environment should be atomic and should not be effected by user activities like
//                // clicking a link in recent activity
//
//
//                var spanId = payload.getSpan().getSpanCodeObjectId();
//                var methodId = payload.getSpan().getMethodCodeObjectId();
//
//                var canNavigate = project.getService(CodeNavigator.class).canNavigateToSpanOrMethod(spanId, methodId);
//                if (canNavigate) {
//                    MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing();
//                    EnvironmentsSupplier environmentsSupplier = AnalyticsService.getInstance(project).getEnvironment();
//                    String actualEnvName = adjustBackEnvNameIfNeeded(payload.getEnvironment());
//                    environmentsSupplier.setCurrent(actualEnvName, false, () -> EDT.ensureEDT(() -> {
//                        project.getService(InsightsViewOrchestrator.class).showInsightsForSpanOrMethodAndNavigateToCode(spanId, methodId);
//                    }));
//                } else {
//                    MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing();
//                    NotificationUtil.showNotification(project, "code object could not be found in the workspace");
//                    EnvironmentsSupplier environmentsSupplier = AnalyticsService.getInstance(project).getEnvironment();
//                    String actualEnvName = adjustBackEnvNameIfNeeded(payload.getEnvironment());
//                    environmentsSupplier.setCurrent(actualEnvName, false, () -> EDT.ensureEDT(() -> {
//                        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(payload.getSpan().getSpanCodeObjectId());
//                    }));
//                }
//
//                ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.RecentActivity, canNavigate);
//            });
//        }
//    }

//    private void processRecentActivityGoToTraceRequest(RecentActivityGoToTraceRequest recentActivityGoToTraceRequest, Project project) {
//        RecentActivityEntrySpanForTracePayload payload = recentActivityGoToTraceRequest.getPayload();
//        if (payload != null) {
//            openJaegerFromRecentActivity(project, payload.getTraceId(), payload.getSpan().getScopeId());
//        } else {
//            Log.log(logger::debug, "processRecentActivityGoToTraceRequest payload is empty");
//        }
//    }

//    private void fetchRecentActivities() {
//        java.util.List<String> allEnvironments = AnalyticsService.getInstance(project).getEnvironments();
//        if (allEnvironments == null) {
//            Log.log(logger::warn, "error while getting environments from server");
//            return;
//        }
//        RecentActivityResult recentActivityData = null;
//        try {
//            recentActivityData = AnalyticsService.getInstance(project).getRecentActivity(allEnvironments);
//        } catch (AnalyticsServiceException e) {
//            Log.log(logger::warn, "AnalyticsServiceException for getRecentActivity: {}", e.getMessage());
//        }
//
//        if (recentActivityData != null) {
//            if (!PersistenceService.getInstance().getState().getFirstTimeRecentActivityReceived() && !recentActivityData.getEntries().isEmpty()) {
//                ActivityMonitor.getInstance(project).registerFirstTimeRecentActivityReceived();
//                PersistenceService.getInstance().getState().setFirstTimeRecentActivityReceived(true);
//            }
//
//            latestActivityResult = recentActivityData;
//
//            // Tool window may not be opened yet
//            if (webAppInitialized) {
//                sendLatestActivities(allEnvironments);
//            }
//        }
//
//        //todo:add in timer
//        if (hasRecentActivity()) {
//            showGreenDot();
//        } else {
//            hideGreenDot();
//        }
//    }

//    private void sendLatestActivities(java.util.List<String> allEnvironments) {
//        java.util.List<String> sortedEnvironments = getSortedEnvironments(allEnvironments, localHostname);
//        String requestMessage = JCefBrowserUtil.resultToString(new JcefMessageRequest(
//                REQUEST_MESSAGE_TYPE,
//                RECENT_ACTIVITY_SET_DATA,
//                new JcefMessagePayload(
//                        sortedEnvironments,
//                        getEntriesWithAdjustedLocalEnvs(latestActivityResult)
//                )
//        ));
//
//        JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
//    }

//    private boolean hasRecentActivity() {
//        var latestActivity = latestActivityResult.getEntries().stream()
//                .map(RecentActivityResponseEntry::getLatestTraceTimestamp)
//                .max(Date::compareTo);
//
//        return latestActivity.isPresent() &&
//                RecentActivityLogic.isRecentTime(latestActivity.get());
//    }

//    private List<RecentActivityResponseEntry> getEntriesWithAdjustedLocalEnvs(RecentActivityResult recentActivityData) {
//        return recentActivityData != null ? recentActivityData.getEntries().stream()
//                .map(f -> new RecentActivityResponseEntry(
//                        getAdjustedEnvName(f.getEnvironment()),
//                        f.getTraceFlowDisplayName(),
//                        f.getFirstEntrySpan(),
//                        f.getLastEntrySpan(),
//                        f.getLatestTraceId(),
//                        f.getLatestTraceTimestamp(),
//                        f.getLatestTraceDuration(),
//                        f.getSlimAggregatedInsights())
//                )
//                .toList() : new ArrayList<>();
//    }

//    private String getAdjustedEnvName(String environment) {
//        String envUcase = environment.toUpperCase();
//
//        if (envUcase.endsWith(SUFFIX_OF_LOCAL))
//            return LOCAL_ENV;
//        if (envUcase.endsWith(SUFFIX_OF_LOCAL_TESTS))
//            return LOCAL_TESTS_ENV;
//
//        return environment;
//    }

//    private String adjustBackEnvNameIfNeeded(String environment) {
//        if (environment.equalsIgnoreCase(LOCAL_ENV)) {
//            return (localHostname + SUFFIX_OF_LOCAL).toUpperCase();
//        }
//        if (environment.equalsIgnoreCase(LOCAL_TESTS_ENV)) {
//            return (localHostname + SUFFIX_OF_LOCAL_TESTS).toUpperCase();
//        }
//        return environment;
//    }

//    private void sendRequestToChangeTraceButtonDisplaying(JBCefBrowser jbCefBrowser) {
//        String requestMessage = JCefBrowserUtil.resultToString(
//                new JaegerUrlChangedRequest(
//                        REQUEST_MESSAGE_TYPE,
//                        GLOBAL_SET_IS_JAEGER_ENABLED,
//                        new JaegerUrlChangedPayload(JaegerUtilKt.isJaegerButtonEnabled())
//                ));
//        JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
//    }

//    private void showGreenDot() {
//        var toolWindow = getToolWindowOrNull();
//        if (toolWindow == null)
//            return;
//
//        EDT.ensureEDT(() -> toolWindow.setIcon(iconWithGreenDot));
//    }
//
//    private void hideGreenDot() {
//        var toolWindow = getToolWindowOrNull();
//        if (toolWindow == null)
//            return;
//
//        EDT.ensureEDT(() -> toolWindow.setIcon(icon));
//    }

//    private ToolWindow getToolWindowOrNull() {
//        return ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID);
//    }

//    public void sendLiveData(@NotNull DurationLiveData durationLiveData, @NotNull String codeObjectId) {

//        Log.log(logger::debug, project, "Got sendLiveData request for {}", codeObjectId);
//
//        stopLiveDataTimerTask();
//
//        if (jbCefBrowser == null) {
//            Log.log(logger::debug, project, "jbCefBrowser is not initialized, calling showToolWindow");
//
//            //ugly hack for initialization when RECENT_ACTIVITY_INITIALIZE message is sent.
//            // if the recent activity window was not yet initialized then we need to send the live data only after
//            // RECENT_ACTIVITY_INITIALIZE message is sent.
//            initTask = new MyInitTask(codeObjectId) {
//                @Override
//                public void run() {
//                    sendLiveDataImpl(durationLiveData);
//                }
//            };
//
//            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
//
//        } else {
//            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
//            sendLiveDataImpl(durationLiveData);
//            startNewLiveDataTimerTask(codeObjectId);
//        }
//    }


//    private void sendLiveDataImpl(DurationLiveData durationLiveData) {

        //should not happen but we need to check
//        if (durationLiveData.getDurationData() == null) {
//            Log.log(logger::debug, project, "durationLiveData.getDurationData is null, not sending live data for {}", durationLiveData);
//            return;
//        }
//
//        Log.log(logger::debug, project, "sending live data for {}", durationLiveData.getDurationData().getCodeObjectId());
//        LiveDataMessage liveDataMessageMessage =
//                new LiveDataMessage("digma", RECENT_ACTIVITY_SET_LIVE_DATA,
//                        new LiveDataPayload(durationLiveData.getLiveDataRecords(), durationLiveData.getDurationData()));
//        try {
//            var strMessage = JsonUtils.javaRecordToJsonString(liveDataMessageMessage);
//            JCefBrowserUtil.postJSMessage(strMessage, jbCefBrowser);
//        } catch (Exception e) {
//            Log.debugWithException(logger, project, e, "Exception sending live data message");
//        }
//    }

//    private void stopLiveDataTimerTask() {
//        Log.log(logger::debug, project, "Stopping timer");
//        if (myLiveDataTimer != null) {
//            myLiveDataTimer.cancel();
//            myLiveDataTimer = null;
//        }
//    }

//    private void startNewLiveDataTimerTask(@NotNull String codeObjectId) {

//        Log.log(logger::debug, project, "Starting new timer for {}", codeObjectId);
//
//        if (myLiveDataTimer != null) {
//            myLiveDataTimer.cancel();
//        }
//        myLiveDataTimer = new Timer();
//        myLiveDataTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    DurationLiveData newDurationLiveData = AnalyticsService.getInstance(project).getDurationLiveData(codeObjectId);
//                    if (newDurationLiveData.getDurationData() == null) {
//                        Log.log(logger::debug, project, "newDurationLiveData.getDurationData is null, stopping refresh timer for {}", codeObjectId);
//                        stopLiveDataTimerTask();
//                        return;
//                    }
//                    sendLiveDataImpl(newDurationLiveData);
//                } catch (AnalyticsServiceException e) {
//                    Log.debugWithException(logger, project, e, "got Exception from getDurationLiveData. Stopping refresh timer for {} {}", codeObjectId, e.getMessage());
//                    stopLiveDataTimerTask();
//                    ErrorReporter.getInstance().reportError(project, "RecentActivityService.startNewLiveDataTimerTask", e);
//                } catch (Exception e) {
//                    //catch any other exception and rethrow because it's a bug we should fix
//                    Log.debugWithException(logger, project, e, "Exception in myLiveDataTimer,Stopping refresh timer for {} {}", codeObjectId, e.getMessage());
//                    stopLiveDataTimerTask();
//                    ErrorReporter.getInstance().reportError(project, "RecentActivityService.startNewLiveDataTimerTask", e);
//                    throw e;
//                }
//            }
//        }, 5000, 5000);
//    }


//    public void runInitTask() {
//        if (initTask != null) {
//            initTask.run();
//            startNewLiveDataTimerTask(initTask.codeObjectId);
//            initTask = null;
//        }
//    }

//    public void liveViewClosed(@Nullable CloseLiveViewMessage closeLiveViewMessage) {
//
//        //closeLiveViewMessage may be null if there is an error parsing the message.
//        // this is a protection against errors so that the timer is always closed when user clicks the close button
//
//        Log.log(logger::debug, project, "Stopping timer");
//        if (closeLiveViewMessage != null) {
//            Log.log(logger::debug, project, "Stopping timer for {}", closeLiveViewMessage.payload().codeObjectId());
//            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(closeLiveViewMessage.payload().codeObjectId());
//            //currently not considering the codeObjectId because there is only one timer task.
//            // but may be necessary in the future when there are few live views opened
//        }
//        stopLiveDataTimerTask();
//    }

    @Override
    public void dispose() {
//        if (myLiveDataTimer != null) {
//            myLiveDataTimer.cancel();
//        }

//        if (activityFetchingTimer != null)
//            activityFetchingTimer.cancel();
//
//        if (msgRouter != null)
//            msgRouter.dispose();

//        if (jbCefBrowser != null)
//            jbCefBrowser.dispose();
    }


//    private abstract static class MyInitTask implements Runnable {
//
//        private final String codeObjectId;
//
//        public MyInitTask(@NotNull String codeObjectId) {
//            this.codeObjectId = codeObjectId;
//        }
//    }
}
