package org.digma.intellij.plugin.insights;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.IDEUtilsService;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.document.DocumentInfoContainer;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider;
import org.digma.intellij.plugin.insights.model.outgoing.Method;
import org.digma.intellij.plugin.insights.model.outgoing.Span;
import org.digma.intellij.plugin.insights.model.outgoing.ViewMode;
import org.digma.intellij.plugin.instrumentation.MethodInstrumentationPresenter;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.UserRegistrationEvent;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.CodeLessSpan;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightStatus;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse;
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsightStatus;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.common.Laf;
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope;
import org.digma.intellij.plugin.ui.model.DocumentScope;
import org.digma.intellij.plugin.ui.model.EmptyScope;
import org.digma.intellij.plugin.ui.model.EndpointScope;
import org.digma.intellij.plugin.ui.model.MethodScope;
import org.digma.intellij.plugin.ui.model.Scope;
import org.digma.intellij.plugin.ui.model.UIInsightsStatus;
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService;
import org.digma.intellij.plugin.ui.service.InsightsService;
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier;
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.digma.intellij.plugin.ui.tests.TestsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.ui.jcef.JCefMessagesUtilsKt.sendUserEmail;

public final class InsightsServiceImpl implements InsightsService, Disposable {

    private final Logger logger = Logger.getInstance(InsightsServiceImpl.class);

    //todo: not implemented yet
    private static final String EMPTY_SERVICE_NAME = "";

    private final Project project;


    static final String RESOURCE_FOLDER_NAME = "/webview/insights";
    static final String DOMAIN_NAME = "insights";
    static final String SCHEMA_NAME = "http";

    static final String MODEL_PROP_INSTRUMENTATION = "MODEL_PROP_INSTRUMENTATION";


    static final Long MAX_SECONDS_WAIT_FOR_DEPENDENCY = 6L;
    static final Long WAIT_FOR_DEPENDENCY_INTERVAL_MILLIS = 250L;

    private JBCefBrowser jbCefBrowser;
    private CefMessageRouter cefMessageRouter;
    private InsightsMessageRouterHandler messageHandler;

    private final ReentrantLock updateLock = new ReentrantLock();


    public InsightsServiceImpl(Project project) {
        this.project = project;

        //TestsService depends on InsightsModelReact.scope so make sure its initialized and listening.
        // It may also be called from TestsTabPanel, whom even comes first
        project.getService(TestsService.class);

        if (JBCefApp.isSupported()) {

            jbCefBrowser = JBCefBrowserBuilderCreator.create()
                    .setUrl("http://" + DOMAIN_NAME + "/index.html")
                    .build();

            var jbCefClient = jbCefBrowser.getJBCefClient();
            cefMessageRouter = CefMessageRouter.create();
            messageHandler = new InsightsMessageRouterHandler(project, jbCefBrowser);
            cefMessageRouter.addHandler(messageHandler, true);
            jbCefClient.getCefClient().addDownloadHandler(new DownloadHandlerAdapter());
            jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);


            var lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
                @Override
                public void onAfterCreated(CefBrowser browser) {
                    registerAppSchemeHandler(project);
                }
            };

            jbCefBrowser.getJBCefClient().addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser());

            Disposer.register(this, () -> jbCefBrowser.getJBCefClient().removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser()));


            ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(new SettingsChangeListener() {
                @Override
                public void systemFontChange(@NotNull String fontName) {
                    messageHandler.sendRequestToChangeFont(fontName);
                }

                @Override
                public void systemThemeChange(@NotNull Theme theme) {
                    messageHandler.sendRequestToChangeUiTheme(theme);
                }

                @Override
                public void editorFontChange(@NotNull String fontName) {
                    messageHandler.sendRequestToChangeCodeFont(fontName);
                }
            });


            project.getMessageBus().connect(this).subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, new EnvironmentChanged() {
                @Override
                public void environmentChanged(@Nullable String newEnv, boolean refreshInsightsView) {
                    Backgroundable.ensurePooledThread(InsightsServiceImpl.this::pushInsightsOnEnvironmentChange);
                }

                @Override
                public void environmentsListChanged(List<String> newEnvironments) {
                    //nothing to do
                }
            });

            //todo: change to JaegerButtonStateListener().start(project, jCefComponent)
            SettingsState.getInstance().addChangeListener(settingsState -> JCefBrowserUtil.sendRequestToChangeTraceButtonEnabled(jbCefBrowser), this);


            project.getMessageBus().connect(this).subscribe(UserRegistrationEvent.USER_REGISTRATION_TOPIC, new UserRegistrationEvent() {
                @Override
                public void userRegistered(@NotNull String email) {
                    sendUserEmail(jbCefBrowser.getCefBrowser(), email);
                }
            });
        }
    }

    @Override
    public void dispose() {
        if (jbCefBrowser != null) {
            jbCefBrowser.dispose();
        }
        if (cefMessageRouter != null) {
            cefMessageRouter.dispose();
        }
    }

    private InsightsModelReact model() {
        return InsightsModelReact.getInstance(project);
    }

    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new InsightsSchemeHandlerFactory(project));
    }

    @Override
    public @NotNull JComponent getComponent() {
        if (JBCefApp.isSupported()) {
            return jbCefBrowser.getComponent();
        }
        return new JLabel("JCef not supported");
    }


    @Override
    public boolean isIndexHtml(@NotNull String path) {
        return path.endsWith("index.html");
    }


    @Override
    @Nullable
    public InputStream buildIndexFromTemplate(@NotNull String path) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        return new InsightsIndexTemplateBuilder().build(project);
    }


    @Override
    public void updateInsights(@NotNull CodeLessSpan codeLessSpan) {
        //only interface methods should set the scope when it really changes because InsightsModelReact
        // fires scope change event and some other services depend on that event. refresh methods that are
        // internal to this service should not change the scope.
        model().clearProperties();
        var scope = new CodeLessSpanScope(codeLessSpan, null);
        model().setScope(scope);
        updateInsights(scope);
    }


    private void updateInsights(@NotNull CodeLessSpanScope codeLessSpanScope) {

        Backgroundable.ensurePooledThread(() -> withUpdateLock(() -> {

            var codeLessSpan = codeLessSpanScope.getSpan();

            Log.log(logger::debug, "updateInsightsModel to {}. ", codeLessSpan);

            try {
                var insightsResponse = AnalyticsService.getInstance(project).getInsightsForSingleSpan(codeLessSpan.getSpanId());
                codeLessSpanScope.setSpanInfo(insightsResponse.getSpanInfo());

                var insights = insightsResponse.getInsights();

                var status = UIInsightsStatus.Default;
                if (insights == null || insights.isEmpty()) {
                    status = UIInsightsStatus.NoInsights;
                }

                messageHandler.pushInsights(insights, Collections.emptyList(), codeLessSpan.getSpanId(), EMPTY_SERVICE_NAME,
                        AnalyticsService.getInstance(project).getEnvironment().getCurrent(), status.name(),
                        ViewMode.INSIGHTS.name(), Collections.emptyList(), false, false, false);

            } catch (AnalyticsServiceException e) {
                Log.warnWithException(logger, project, e, "Error in getInsightsForSingleSpan");
                ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.updateInsights", e);
                emptyInsights();
            }
        }));
    }


    @Override
    public void updateInsights(@NotNull EndpointInfo endpointInfo) {
        //only interface methods should set the scope when it really changes because InsightsModelReact
        // fires scope change event and some other services depend on that event. refresh methods that are
        // internal to this service should not change the scope.
        model().clearProperties();
        var scope = new EndpointScope(endpointInfo);
        model().setScope(scope);
        updateInsights(scope);
    }

    private void updateInsights(@NotNull EndpointScope endpointScope) {

        Backgroundable.ensurePooledThread(() -> withUpdateLock(() -> {

            var endpointInfo = endpointScope.getEndpoint();

            Log.log(logger::debug, "updateInsightsModel to {}. ", endpointInfo);

            try {
                var insightsResponse = AnalyticsService.getInstance(project).getInsightsForSingleEndpoint(endpointInfo.idWithType());

                var methodWithInsights = insightsResponse.getMethodsWithInsights().stream().findAny().orElse(null);
                if (methodWithInsights != null) {
                    var status = UIInsightsStatus.Default;
                    if (methodWithInsights.getInsights().isEmpty()) {
                        status = UIInsightsStatus.NoInsights;
                    }

                    messageHandler.pushInsights(methodWithInsights.getInsights(), Collections.emptyList(), endpointInfo.getId(), EMPTY_SERVICE_NAME,
                            AnalyticsService.getInstance(project).getEnvironment().getCurrent(), status.name(),
                            ViewMode.INSIGHTS.name(), Collections.emptyList(), false, false, false);
                }

            } catch (AnalyticsServiceException e) {
                Log.warnWithException(logger, project, e, "Error in getInsightsForSingleEndpoint");
                ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.updateInsights", e);
                emptyInsights();
            }
        }));
    }


    @Override
    public void updateInsights(@NotNull MethodInfo methodInfo) {
        //only interface methods should set the scope when it really changes because InsightsModelReact
        // fires scope change event and some other services depend on that event. refresh methods that are
        // internal to this service should not change the scope.
        model().clearProperties();
        var scope = new MethodScope(methodInfo);
        model().setScope(scope);
        updateInsights(scope, null);
    }


    private void updateInsights(@NotNull MethodScope methodScope, @Nullable UIInsightsStatus predefinedStatus) {

        Backgroundable.ensurePooledThread(() -> withUpdateLock(() -> {
            //check we are still on the same method. while updating the status the scope may already change
            if (model().getScope() instanceof MethodScope currentMethodScope &&
                    methodScope.getMethodInfo().getId().equals(currentMethodScope.getMethodInfo().getId())) {
                updateInsightsImpl(methodScope, predefinedStatus);
            }
        }));
    }


    private void updateInsightsImpl(@NotNull MethodScope methodScope, @Nullable UIInsightsStatus predefinedStatus) {

        Backgroundable.ensurePooledThread(() -> withUpdateLock(() -> {

            var methodInfo = methodScope.getMethodInfo();

            Log.log(logger::debug, "updateInsightsModel to {}. ", methodInfo);

            try {

                var methodInstrumentationPresenter = new MethodInstrumentationPresenter(project);
                ApplicationManager.getApplication().runReadAction(() -> methodInstrumentationPresenter.update(methodInfo.getId()));
                var hasMissingDependency = methodInstrumentationPresenter.getCannotBecauseMissingDependency();
                var canInstrumentMethod = methodInstrumentationPresenter.getCanInstrumentMethod();
                model().addProperty(MODEL_PROP_INSTRUMENTATION, methodInstrumentationPresenter);

                var insights = getInsightsByMethodInfo(methodInfo);

                var spans = methodInfo.getSpans().stream().map(spanInfo -> new Span(spanInfo.getId(), spanInfo.getName())).toList();

                var statusToUse = predefinedStatus != null ? predefinedStatus.name() : null;
                if (predefinedStatus == null) {
                    statusToUse = UIInsightsStatus.Default.name();
                    if (insights.isEmpty()) {
                        Log.log(logger::debug, "No insights for method {}, Starting background thread to update status.", methodInfo.getName());
                        statusToUse = UIInsightsStatus.Loading.name();
                        updateStatusInBackground(methodScope);
                    }
                }


                boolean needsObservabilityFix = checkObservability(methodInfo, insights);

                messageHandler.pushInsights(insights, spans, methodInfo.getId(), EMPTY_SERVICE_NAME,
                        AnalyticsService.getInstance(project).getEnvironment().getCurrent(), statusToUse,
                        ViewMode.INSIGHTS.name(), Collections.emptyList(), hasMissingDependency, canInstrumentMethod, needsObservabilityFix);

            } catch (Exception e) {
                Log.warnWithException(logger, project, e, "error in updateInsightsImpl for ", methodInfo.getId());
                ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.updateInsightsImpl", e);
            }
        }));
    }


    private List<CodeObjectInsight> getInsightsByMethodInfo(@NotNull MethodInfo methodInfo) throws AnalyticsServiceException {

        try {
//        var insights = DocumentInfoService.getInstance(project).getCachedMethodInsights(methodInfo);
            InsightsOfMethodsResponse insightsOfMethodsResponse = AnalyticsService.getInstance(project).getInsightsOfMethods(Collections.singletonList(methodInfo));

            if (insightsOfMethodsResponse.getMethodsWithInsights().isEmpty()) {
                Log.log(logger::debug, project, "could not find insights for {}", methodInfo);
                return Collections.emptyList();
            }

            var methodsWithInsights = insightsOfMethodsResponse.getMethodsWithInsights().stream().findAny().orElse(null);
            return methodsWithInsights == null ? Collections.emptyList() : methodsWithInsights.getInsights();
        } catch (NoSelectedEnvironmentException e) {
            //this may happen a lot when there is no connection or no environments
            return Collections.emptyList();
        }
    }


    private void updateStatusInBackground(@NotNull MethodScope methodScope) {

        Backgroundable.executeOnPooledThread(() -> {

            var methodInfo = methodScope.getMethodInfo();

            Log.log(logger::debug, "Loading backend status in background for method {}", methodInfo.getName());
            var insightStatus = getInsightStatus(methodInfo);
            Log.log(logger::debug, "Got status from backend {} for method {}", insightStatus, methodInfo.getName());

            UIInsightsStatus status = toUiInsightStatus(insightStatus, methodInfo.hasRelatedCodeObjectIds());

            updateInsights(methodScope, status);

        });
    }


    private UIInsightsStatus toUiInsightStatus(InsightStatus status, Boolean methodHasRelatedCodeObjectIds) {

        if (status == InsightStatus.InsightExist || status == InsightStatus.InsightPending) {
            return UIInsightsStatus.InsightPending;
        }
        if (status == InsightStatus.NoSpanData || status == null) {
            if (Boolean.TRUE.equals(methodHasRelatedCodeObjectIds)) {
                return UIInsightsStatus.NoSpanData;
            } else {
                if (IDEUtilsService.getInstance(project).isJavaProject()) {
                    return UIInsightsStatus.NoObservability;
                } else {
                    return UIInsightsStatus.NoInsights;
                }
            }
        }

        return UIInsightsStatus.NoInsights;
    }


    @Nullable
    private InsightStatus getInsightStatus(@NotNull MethodInfo methodInfo) {
        try {
            CodeObjectInsightsStatusResponse response = AnalyticsService.getInstance(project).getCodeObjectInsightStatus(List.of(methodInfo));
            MethodWithInsightStatus methodResp = response.getCodeObjectsWithInsightsStatus().stream().findFirst().orElse(null);
            return methodResp == null ? null : methodResp.getInsightStatus();
        } catch (AnalyticsServiceException e) {
            Log.log(logger::debug, "AnalyticsServiceException for getCodeObjectInsightStatus for {}: {}", methodInfo.getId(), e.getMessage());
            ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.getInsightStatus", e);
            return null;
        }
    }


    private void emptyInsights() {
        withUpdateLock(() -> {
            model().clearProperties();
            messageHandler.emptyInsights();
        });
    }


    @Override
    public void showDocumentPreviewList(@Nullable DocumentInfoContainer documentInfoContainer, @NotNull String fileUri) {
        model().clearProperties();
        if (documentInfoContainer == null) {
            model().setScope(new EmptyScope(fileUri.substring(fileUri.lastIndexOf("/"))));
            messageHandler.emptyPreview();
        } else {
            var documentScope = new DocumentScope(documentInfoContainer.getDocumentInfo());
            model().setScope(documentScope);
            showDocumentPreviewList(documentScope, fileUri);
        }
    }

    private void showDocumentPreviewList(@NotNull DocumentScope documentScope, @NotNull String fileUri) {

        withUpdateLock(() -> {

            var documentInfo = documentScope.getDocumentInfo();
            var documentInfoContainer = DocumentInfoService.getInstance(project).getDocumentInfo(documentInfo.getFileUri());

            var functionsList = Collections.<Method>emptyList();
            if (documentInfoContainer != null) {
                functionsList = getDocumentPreviewItems(documentInfoContainer);
            }

            var status = UIInsightsStatus.Default;
            if (functionsList.isEmpty()) {
                if (hasDiscoverableCodeObjects(documentInfo)) {
                    status = UIInsightsStatus.NoSpanData;
                } else {
                    status = UIInsightsStatus.NoInsights;
                }
            }

            messageHandler.pushInsights(Collections.emptyList(), Collections.emptyList(), fileUri, EMPTY_SERVICE_NAME,
                    AnalyticsService.getInstance(project).getEnvironment().getCurrent(), status.name(), ViewMode.PREVIEW.name(), functionsList, false, false, false);
        });
    }

    private boolean hasDiscoverableCodeObjects(DocumentInfo documentInfo) {
        return documentInfo.getMethods().entrySet().stream().anyMatch(entry -> entry.getValue().hasRelatedCodeObjectIds());
    }


    private List<Method> getDocumentPreviewItems(DocumentInfoContainer documentInfoContainer) {

        List<Method> methods = new java.util.ArrayList<>(documentInfoContainer.getDocumentInfo()
                .getMethods().entrySet().stream().filter(entry -> documentInfoContainer.hasInsights(entry.getKey()))
                .map(entry -> new Method(entry.getKey(), entry.getValue().getName())).toList());

        methods.sort(Comparator.comparing(Method::name));

        return methods;
    }


    @Override
    public void refreshInsights() {

        Backgroundable.ensurePooledThread(() -> withUpdateLock(() -> {
            Log.log(logger::debug, project, "refreshInsights called, scope is {}", getScopeObject(model().getScope()));
            var scope = model().getScope();
            if (scope instanceof MethodScope) {
                updateInsights((MethodScope) scope, null);
            } else if (scope instanceof CodeLessSpanScope) {
                updateInsights((CodeLessSpanScope) scope);
            } else if (scope instanceof EndpointScope) {
                updateInsights((EndpointScope) scope);
            } else if (scope instanceof DocumentScope) {
                showDocumentPreviewList((DocumentScope) scope, ((DocumentScope) scope).getDocumentInfo().getFileUri());
            } else {
                emptyInsights();
            }
        }));
    }


    private boolean checkObservability(@NotNull MethodInfo methodInfo, List<CodeObjectInsight> insights) {
        if (!IDEUtilsService.getInstance(project).isJavaProject()) return false;
        if (methodInfo.hasRelatedCodeObjectIds()) return false;

        Set<InsightType> insightTypes = insights.stream()
                .map(CodeObjectInsight::getType)
                .collect(Collectors.toSet());

        boolean hasInsightOfErrors = insightTypes.remove(InsightType.Errors);
        boolean hasInsightOfHotSpot = insightTypes.remove(InsightType.HotSpot);

        if (hasInsightOfErrors || hasInsightOfHotSpot) {
            boolean onlyErrorInsights = insightTypes.isEmpty();
            if (onlyErrorInsights) {
                return true;
            }
        }

        return false;
    }


    @Override
    public void addAnnotation(@NotNull String methodId) {
        if (model().getScope() instanceof MethodScope methodScope && methodScope.getMethodInfo().getId().equals(methodId)) {
            MethodInstrumentationPresenter methodInstrumentationPresenter = (MethodInstrumentationPresenter) model().getProperty(MODEL_PROP_INSTRUMENTATION);
            if (methodInstrumentationPresenter != null) {
                EDT.ensureEDT(() -> {
                    var succeeded = WriteAction.compute(methodInstrumentationPresenter::instrumentMethod);
                    if (succeeded) {
                        refreshInsights();
                    } else {
                        NotificationUtil.notifyError(project, "Failed to add annotation");
                    }
                });
            }
        }
    }

    @Override
    public void fixMissingDependencies(@NotNull String methodId) {
        if (model().getScope() instanceof MethodScope methodScope && methodScope.getMethodInfo().getId().equals(methodId)) {
            MethodInstrumentationPresenter methodInstrumentationPresenter = (MethodInstrumentationPresenter) model().getProperty(MODEL_PROP_INSTRUMENTATION);
            if (methodInstrumentationPresenter != null) {

                EDT.ensureEDT(() -> WriteAction.run(methodInstrumentationPresenter::addDependencyToOtelLibAndRefresh));

                Backgroundable.executeOnPooledThread(() -> waitForOtelDependencyToBeAvailable(methodInstrumentationPresenter));
            }
        }
    }

    private void waitForOtelDependencyToBeAvailable(MethodInstrumentationPresenter methodInstrumentationPresenter) {
        var startPollingTimeSeconds = Instant.now().getEpochSecond();
        var canInstrument = methodInstrumentationPresenter.getCanInstrumentMethod();
        while (!canInstrument) {
            var nowTimeSeconds = Instant.now().getEpochSecond();
            if (nowTimeSeconds >= startPollingTimeSeconds + MAX_SECONDS_WAIT_FOR_DEPENDENCY) {
                break;
            }
            try {
                Thread.sleep(WAIT_FOR_DEPENDENCY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                //ignore
            }

            methodInstrumentationPresenter.update(methodInstrumentationPresenter.getSelectedMethodId());
            canInstrument = methodInstrumentationPresenter.getCanInstrumentMethod();
        }
    }


    //todo: need to make sure that DocumentInfoService already refreshed its insights cache.
    // currently if scope is codeless span then it doesn't depend on DocumentInfoService,
    // but method scope does, if its method scope the language service will trigger a method under caret event
    // anyway and the insights will refresh, probably twice.
    // maybe its better to call refresh from EnvironmentChangeHandler after DocumentInfoService has refreshed.
    private void pushInsightsOnEnvironmentChange() {
        Log.log(logger::debug, project, "pushInsightsOnEnvironmentChange called");
        refreshInsights();
    }


    private void withUpdateLock(Runnable task) {
        try {
            updateLock.lock();
            task.run();

        } finally {
            if (updateLock.isHeldByCurrentThread()) {
                updateLock.unlock();
            }
        }
    }


    @Override
    public void showInsight(@NotNull String spanId) {
        Log.log(logger::debug, project, "showInsight called {}", spanId);
        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(spanId);
    }


    @Override
    public void openHistogram(@NotNull String instrumentationLibrary, @NotNull String spanName, @NotNull String insightType, @Nullable String displayName) {

        Log.log(logger::debug, project, "openHistogram called {},{}", instrumentationLibrary, spanName);

        ActivityMonitor.getInstance(project).registerButtonClicked("histogram", InsightType.valueOf(insightType));
        var title  = displayName != null? displayName: spanName;
        try {

            try {
                switch (InsightType.valueOf(insightType)) {
                    case SpanDurations -> {
                        String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanPercentiles(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                        DigmaHTMLEditorProvider.openEditor(project, "Percentiles Graph of Span " + title, htmlContent);
                    }
                    case SpanScaling -> {
                        String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanScaling(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                        DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span " + title, htmlContent);
                    }
                    default -> {
                        //todo: a fallback when the type is unknown, we should add support for more types if necessary
                        String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanScaling(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                        DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span " + title, htmlContent);
                    }
                }
            } catch (IllegalArgumentException e) {
                //fallback for span type that is not in the enum
                String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanScaling(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span " + title, htmlContent);
            }

        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error in openHistogram for {},{} {}", instrumentationLibrary, title, e.getMessage());
            ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.openHistogram", e);
        }
    }


    @Override
    public void openLiveView(@NotNull String prefixedCodeObjectId) {
        Log.log(logger::debug, project, "openLiveView called {}", prefixedCodeObjectId);
        project.getService(RecentActivityService.class).startLiveView(prefixedCodeObjectId);
        ActivityMonitor.getInstance(project).registerCustomEvent("live view clicked", Collections.emptyMap());
    }


    @Override
    public void recalculate(@NotNull String prefixedCodeObjectId, @NotNull String insightType) {
        try {
            AnalyticsService.getInstance(project).setInsightCustomStartTime(prefixedCodeObjectId, InsightType.valueOf(insightType));
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error in setInsightCustomStartTime {}", e.getMessage());
        }
        ActivityMonitor.getInstance(project).registerButtonClicked("recalculate", InsightType.valueOf(insightType));
    }

    @Override
    public void refresh(@NotNull InsightType insightType) {
        //TODO: do a real refresh, after refactoring the RefreshService, refresh the insights
        RefreshService.getInstance(project).refreshAllInBackground();
        ActivityMonitor.getInstance(project).registerButtonClicked("refresh", insightType);
    }


    @Override
    public void goToTrace(@NotNull String traceId, @NotNull String traceName, @NotNull InsightType insightType, @Nullable String spanCodeObjectId) {
        JaegerUtilKt.openJaegerFromInsight(project, traceId, traceName, insightType, spanCodeObjectId);
    }

    @Override
    public void goToTraceComparison(@NotNull String traceId1, @NotNull String traceName1, @NotNull String traceId2, @NotNull String traceName2, @NotNull InsightType insightType) {
        JaegerUtilKt.openJaegerComparisonFromInsight(project, traceId1, traceName1, traceId2, traceName2, insightType);
    }


    private Object getScopeObject(Scope scope) {
        if (scope instanceof CodeLessSpanScope codeLessSpanScope) {
            return codeLessSpanScope.getSpan();
        }
        if (scope instanceof MethodScope methodScope) {
            return methodScope.getMethodInfo();
        }
        if (scope instanceof DocumentScope documentScope) {
            return documentScope.getDocumentInfo().getFileUri();
        }
        return scope;
    }

}
