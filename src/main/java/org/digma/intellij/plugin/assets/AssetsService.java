package org.digma.intellij.plugin.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
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
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.posthog.MonitoredPanel;
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier;
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class AssetsService implements Disposable {

    private final Logger logger = Logger.getInstance(AssetsService.class);

    private final Project project;

    static final String RESOURCE_FOLDER_NAME = "/webview/assets";
    static final String DOMAIN_NAME = "assets";
    static final String SCHEMA_NAME = "http";

    private JBCefBrowser jbCefBrowser;
    private CefMessageRouter cefMessageRouter;
    private AssetsMessageRouterHandler messageHandler;



    public AssetsService(Project project) {

        this.project = project;

        if (JBCefApp.isSupported()) {

            jbCefBrowser = JBCefBrowserBuilderCreator.create()
                    .setUrl("http://" + DOMAIN_NAME + "/index.html")
                    .build();

            var jbCefClient = jbCefBrowser.getJBCefClient();
            cefMessageRouter = CefMessageRouter.create();
            messageHandler = new AssetsMessageRouterHandler(project, jbCefBrowser);
            cefMessageRouter.addHandler(messageHandler, true);
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
                    Backgroundable.ensurePooledThread(() -> {
                        try {
                            messageHandler.pushGlobalEnvironmentChange();
                        } catch (JsonProcessingException e) {
                            Log.debugWithException(logger, e, "Exception in pushAssets ");
                        }
                    });
                }

                @Override
                public void environmentsListChanged(List<String> newEnvironments) {
                    //nothing to do
                }
            });
        }

    }


    JComponent getComponent() {
        if (JBCefApp.isSupported()) {
            return jbCefBrowser.getComponent();
        }
        return null;
    }

    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new AssetsSchemeHandlerFactory(project));
    }

    public static AssetsService getInstance(Project project) {
        return project.getService(AssetsService.class);
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

    boolean isIndexHtml(String path) {
        return path.endsWith("index.html");
    }


    public InputStream buildIndexFromTemplate(String path) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        return new AssetsIndexTemplateBuilder().build(project);
    }

    public String getAssetCategories(String[] services) {
        EDT.assertNonDispatchThread();

        try {
            Log.log(logger::trace, project, "got get categories request");
            String categories = AnalyticsService.getInstance(project).getAssetCategories(services);
            AnalyticsService.getInstance(project).checkInsightExists();
            Log.log(logger::trace, project, "got categories [{}]", categories);
            return categories;
        } catch (NoSelectedEnvironmentException e) {
            Log.log(logger::debug, project, "no environment when calling getCategories {}", e.getMessage());
            return "{ \"assetCategories\": [] }";
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error loading categories {}", e.getMessage());
            return "{ \"assetCategories\": [] }";
        }
    }

    public String getServices() {
        try {
            Log.log(logger::trace, project, "got get assets request");
            String services = AnalyticsService.getInstance(project).getServices();
            Log.log(logger::trace, project, "got services");
            return services;
        } catch (NoSelectedEnvironmentException e) {
            Log.log(logger::debug, project, "no environment when calling getAssets {}", e.getMessage());
            return null;
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error getting services {}", e.getMessage());
            return null;
        }

    }

    public String getAssets(@NotNull Map<String,String> queryParams, String[] services) {

        EDT.assertNonDispatchThread();

        try {
            Log.log(logger::trace, project, "got get assets request");
            String assets = AnalyticsService.getInstance(project).getAssets(queryParams, services);
            Log.log(logger::trace, project, "got assets [{}]", assets);
            return assets;
        } catch (NoSelectedEnvironmentException e) {
            Log.log(logger::debug, project, "no environment when calling getAssets {}", e.getMessage());
            return "";
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error loading assets {}", e.getMessage());
            return "";
        }
    }

    public void showAsset(String spanId) {

        EDT.assertNonDispatchThread();

        Log.log(logger::trace, project, "showAsset called for {}", spanId);
        ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Assets);
        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(spanId);
    }
}
