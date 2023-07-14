package org.digma.intellij.plugin.insights;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.CefApp;
import org.cef.browser.CefMessageRouter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.IDEUtilsService;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.document.DocumentInfoContainer;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider;
import org.digma.intellij.plugin.insights.model.outgoing.Method;
import org.digma.intellij.plugin.insights.model.outgoing.Span;
import org.digma.intellij.plugin.insights.model.outgoing.ViewMode;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.CodeLessSpan;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightStatus;
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsightStatus;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.navigation.HomeSwitcherService;
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.recentactivity.RecentActivityService;
import org.digma.intellij.plugin.ui.common.Laf;
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope;
import org.digma.intellij.plugin.ui.model.DocumentScope;
import org.digma.intellij.plugin.ui.model.EmptyScope;
import org.digma.intellij.plugin.ui.model.MethodScope;
import org.digma.intellij.plugin.ui.model.UIInsightsStatus;
import org.digma.intellij.plugin.ui.model.insights.InsightsModelReact;
import org.digma.intellij.plugin.ui.service.InsightsService;
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier;
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class InsightsServiceImpl implements InsightsService, Disposable {

    private final Logger logger = Logger.getInstance(InsightsServiceImpl.class);

    //todo: not implemented yet
    private static final String EMPTY_SERVICE_NAME = "";

    private final Project project;


    static final String RESOURCE_FOLDER_NAME = "/webview/insights";
    static final String DOMAIN_NAME = "insights";
    static final String SCHEMA_NAME = "http";

    private final InsightsModelReact model = new InsightsModelReact();

    private JBCefBrowser jbCefBrowser;
    private CefMessageRouter cefMessageRouter;
    private InsightsMessageRouterHandler messageHandler;

    private final ReentrantLock updateLock = new ReentrantLock();


    public InsightsServiceImpl(Project project) {
        this.project = project;

        if (JBCefApp.isSupported()) {

            jbCefBrowser = JBCefBrowserBuilderCreator.create()
                    .setUrl("http://" + DOMAIN_NAME + "/index.html")
                    .build();
            registerAppSchemeHandler(project);

            var jbCefClient = jbCefBrowser.getJBCefClient();
            cefMessageRouter = CefMessageRouter.create();
            messageHandler = new InsightsMessageRouterHandler(project, jbCefBrowser);
            cefMessageRouter.addHandler(messageHandler, true);
            jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);


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
                public void environmentChanged(String newEnv, boolean refreshInsightsView) {
                    pushInsightsOnEnvironmentChange();
                }

                @Override
                public void environmentsListChanged(List<String> newEnvironments) {
                    //nothing to do
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


    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new InsightsSchemeHandlerFactory(project));
    }


    @Override
    public @Nullable JComponent getComponent() {
        if (JBCefApp.isSupported()) {
            return jbCefBrowser.getComponent();
        }
        return null;
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

        return new InsightsIndexTemplateBuilder().build();
    }


    @Override
    public void updateInsights(@NotNull CodeLessSpan codeLessSpan) {

        withUpdateLock(() -> {
            Log.log(logger::debug, "updateInsightsModel to {}. ", codeLessSpan);
            try {
                var insightsResponse = project.getService(AnalyticsService.class).getInsightsForSingleSpan(codeLessSpan.getSpanId());
                model.setScope(new CodeLessSpanScope(codeLessSpan, insightsResponse.getSpanInfo()));

                var insights = insightsResponse.getInsights();

                var status = UIInsightsStatus.Default.name();
                //todo: how to update status for span,AnalyticsService.getCodeObjectInsightStatus is only for method
//            if (insights.isEmpty()){
//                Log.log(logger::debug, "No insights for CodeLessSpan {}, Starting background thread to update status.", codeLessSpan.getSpanId());
//                status = UIInsightsStatus.Loading.name();
//                updateStatusInBackground();
//            }
                messageHandler.pushInsights(insights, Collections.emptyList(), codeLessSpan.getSpanId(), EMPTY_SERVICE_NAME,
                        AnalyticsService.getInstance(project).getEnvironment().getCurrent(), status,
                        ViewMode.INSIGHTS.name(), Collections.emptyList(), false);

            } catch (AnalyticsServiceException e) {
                Log.warnWithException(logger, project, e, "Error in getInsightsForSingleSpan");
                emptyInsights();
            }
        });
    }


    @Override
    public void updateInsights(@NotNull MethodInfo methodInfo) {

        withUpdateLock(() -> {
            Log.log(logger::debug, "updateInsightsModel to {}. ", methodInfo);

            model.setScope(new MethodScope(methodInfo));
            var insights = DocumentInfoService.getInstance(project).getCachedMethodInsights(methodInfo);

            var spans = methodInfo.getSpans().stream().map(spanInfo -> new Span(spanInfo.getId(), spanInfo.getName())).toList();

            var status = UIInsightsStatus.Default.name();
            if (insights.isEmpty()) {
                Log.log(logger::debug, "No insights for method {}, Starting background thread to update status.", methodInfo.getName());
                status = UIInsightsStatus.Loading.name();
                updateStatusInBackground(methodInfo);
            }
            messageHandler.pushInsights(insights, spans, methodInfo.getId(), EMPTY_SERVICE_NAME,
                    AnalyticsService.getInstance(project).getEnvironment().getCurrent(), status,
                    ViewMode.INSIGHTS.name(), Collections.emptyList(), false);
        });
    }

    private void updateStatusInBackground(@NotNull MethodInfo methodInfo) {

        Backgroundable.runInNewBackgroundThread(project, "Fetching insights status", () -> {

            Log.log(logger::debug, "Loading backend status in background for method {}", methodInfo.getName());
            var insightStatus = getInsightStatus(methodInfo);
            Log.log(logger::debug, "Got status from backend {} for method {}", insightStatus, methodInfo.getName());

            UIInsightsStatus status;
            //if status is null assign EmptyStatus, it probably means there was a communication error
            if (insightStatus == null) {
                status = UIInsightsStatus.NoInsights;
            } else {
                status = toUiInsightStatus(insightStatus, methodInfo.hasRelatedCodeObjectIds());
            }

            updateInsightsWithStatus(methodInfo, status);

        });
    }

    private void updateInsightsWithStatus(@NotNull MethodInfo methodInfo, UIInsightsStatus status) {

        withUpdateLock(() -> {
            if (model.getScope() instanceof MethodScope methodScope && methodScope.getMethodInfo().getId().equals(methodInfo.getId())) {

                Log.log(logger::debug, "updateInsightsWithStatus to {}, {} ", methodInfo, status);

                var insights = DocumentInfoService.getInstance(project).getCachedMethodInsights(methodInfo);

                var spans = methodInfo.getSpans().stream().map(spanInfo -> new Span(spanInfo.getId(), spanInfo.getName())).toList();

                var statusToUse = UIInsightsStatus.Default;
                if (insights.isEmpty()) {
                    statusToUse = status;
                }
                messageHandler.pushInsights(insights, spans, methodInfo.getId(), EMPTY_SERVICE_NAME,
                        AnalyticsService.getInstance(project).getEnvironment().getCurrent(), statusToUse.name(),
                        ViewMode.INSIGHTS.name(), Collections.emptyList(), false);
            }
        });
    }


    private UIInsightsStatus toUiInsightStatus(InsightStatus status, Boolean methodHasRelatedCodeObjectIds) {

        switch (status) {
            case InsightExist, InsightPending -> {
                return UIInsightsStatus.InsightPending;
            }
            case NoSpanData -> {
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
        }

        return UIInsightsStatus.NoInsights;
    }


    @Nullable
    public InsightStatus getInsightStatus(@NotNull MethodInfo methodInfo) {
        try {
            CodeObjectInsightsStatusResponse response = AnalyticsService.getInstance(project).getCodeObjectInsightStatus(List.of(methodInfo));
            MethodWithInsightStatus methodResp = response.getCodeObjectsWithInsightsStatus().stream().findFirst().orElse(null);
            return methodResp == null ? null : methodResp.getInsightStatus();
        } catch (AnalyticsServiceException e) {
            Log.log(logger::debug, "AnalyticsServiceException for getCodeObjectInsightStatus for {}: {}", methodInfo.getId(), e.getMessage());
            return null;
        }
    }


    private void emptyInsights() {
        withUpdateLock(() -> messageHandler.emptyInsights());
    }


    @Override
    public void showDocumentPreviewList(@Nullable DocumentInfoContainer documentInfoContainer, @NotNull String fileUri) {

        withUpdateLock(() -> {

            if (documentInfoContainer == null) {
                model.setScope(new EmptyScope(fileUri.substring(fileUri.lastIndexOf("/"))));
                messageHandler.emptyPreview();
            } else {
                model.setScope(new DocumentScope(documentInfoContainer.getDocumentInfo()));
                var functionsList = getDocumentPreviewItems(documentInfoContainer);

                var status = UIInsightsStatus.Default;
                if (functionsList.isEmpty()) {
                    if (hasDiscoverableCodeObjects(documentInfoContainer)) {
                        status = UIInsightsStatus.NoSpanData;
                    } else {
                        status = UIInsightsStatus.NoInsights;
                    }
                }

                messageHandler.pushInsights(Collections.emptyList(), Collections.emptyList(), fileUri, EMPTY_SERVICE_NAME,
                        AnalyticsService.getInstance(project).getEnvironment().getCurrent(), status.name(), ViewMode.PREVIEW.name(), functionsList, false);

            }
        });
    }

    private boolean hasDiscoverableCodeObjects(DocumentInfoContainer documentInfoContainer) {
        return documentInfoContainer.getDocumentInfo().getMethods().entrySet().stream().anyMatch(entry -> entry.getValue().hasRelatedCodeObjectIds());
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

        withUpdateLock(() -> {
            Log.log(logger::debug, project, "refreshInsights called, scope is {}", model.getScope().getScope());
            var scope = model.getScope();
            if (scope instanceof MethodScope) {
                updateInsights(((MethodScope) scope).getMethodInfo());
            } else if (scope instanceof CodeLessSpanScope) {
                updateInsights(((CodeLessSpanScope) scope).getSpan());
            } else if (scope instanceof DocumentScope) {
                //do nothing, keep the view as is, don't empty. todo: maybe refresh the functions list,requires reloading insights
            } else {
                emptyInsights();
            }
        });
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
        project.getService(HomeSwitcherService.class).switchToInsights();
        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(spanId);
        project.getService(InsightsAndErrorsTabsHelper.class).switchToInsightsTab();
    }


    @Override
    public void openHistogram(@NotNull String instrumentationLibrary, @NotNull String spanName, @NotNull String insightType) {

        Log.log(logger::debug, project, "openHistogram called {},{}", instrumentationLibrary, spanName);

        ActivityMonitor.getInstance(project).registerButtonClicked("histogram", InsightType.valueOf(insightType));

        try {
            String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanPercentiles(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
            DigmaHTMLEditorProvider.openEditor(project, "Percentiles Graph of Span " + spanName, htmlContent);
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error in openHistogram for {},{} {}", instrumentationLibrary, spanName, e.getMessage());
        }
    }


    @Override
    public void openLiveView(@NotNull String prefixedCodeObjectId) {
        Log.log(logger::debug, project, "openLiveView called {}", prefixedCodeObjectId);

        try {
            DurationLiveData durationLiveData = AnalyticsService.getInstance(project).getDurationLiveData(prefixedCodeObjectId);
            RecentActivityService.getInstance(project).sendLiveData(durationLiveData, prefixedCodeObjectId);
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error loading live view {}", e.getMessage());
        }
    }

}
