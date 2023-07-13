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
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider;
import org.digma.intellij.plugin.insights.model.outgoing.Span;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.CodeLessSpan;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.navigation.HomeSwitcherService;
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.recentactivity.RecentActivityService;
import org.digma.intellij.plugin.ui.common.Laf;
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope;
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
import java.util.List;

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
        Log.log(logger::debug, "updateInsightsModel to {}. ", codeLessSpan);
        try {
            var insightsResponse = project.getService(AnalyticsService.class).getInsightsForSingleSpan(codeLessSpan.getSpanId());
            model.setScope(new CodeLessSpanScope(codeLessSpan, insightsResponse.getSpanInfo()));

            var insights = insightsResponse.getInsights();

            messageHandler.pushInsights(insights, Collections.emptyList(), codeLessSpan.getSpanId(), EMPTY_SERVICE_NAME,
                    AnalyticsService.getInstance(project).getEnvironment().getCurrent(), UIInsightsStatus.Default.name());

        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error in getInsightsForSingleSpan");
            emptyInsights();
        }
    }


    @Override
    public void updateInsights(@NotNull MethodInfo methodInfo) {
        Log.log(logger::debug, "updateInsightsModel to {}. ", methodInfo);

        model.setScope(new MethodScope(methodInfo));
        var insights = DocumentInfoService.getInstance(project).getCachedMethodInsights(methodInfo);

        var spans = methodInfo.getSpans().stream().map(spanInfo -> new Span(spanInfo.getId(), spanInfo.getName())).toList();

        //todo: insights status logic
        messageHandler.pushInsights(insights, spans, methodInfo.getId(), EMPTY_SERVICE_NAME,
                AnalyticsService.getInstance(project).getEnvironment().getCurrent(), UIInsightsStatus.Default.name());
    }


    private void emptyInsights() {
        messageHandler.emptyInsights();
    }


    @Override
    public void refreshInsights() {
        Log.log(logger::debug, project, "refreshInsights called, scope is {}", model.getScope().getScope());
        var scope = model.getScope();
        if (scope instanceof MethodScope) {
            updateInsights(((MethodScope) scope).getMethodInfo());
        } else if (scope instanceof CodeLessSpanScope) {
            updateInsights(((CodeLessSpanScope) scope).getSpan());
        } else {
            emptyInsights();
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
