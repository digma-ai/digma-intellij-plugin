package org.digma.intellij.plugin.troubleshooting;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier;
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.InputStream;

@Service(Service.Level.PROJECT)
public final class TroubleshootingService implements Disposable {

    private final Project project;

    static final String RESOURCE_FOLDER_NAME = "/webview/troubleshooting";
    static final String DOMAIN_NAME = "troubleshooting";
    static final String SCHEMA_NAME = "http";

    private JBCefBrowser jbCefBrowser;
    private CefMessageRouter cefMessageRouter;
    private TroubleshootingMessageRouterHandler messageHandler;


    public TroubleshootingService(Project project) {

        this.project = project;
    }


    JComponent getComponent() {

        if (JBCefApp.isSupported()) {

            jbCefBrowser = JBCefBrowserBuilderCreator.create()
                    .setUrl("http://" + DOMAIN_NAME + "/index.html")
                    .build();

            var jbCefClient = jbCefBrowser.getJBCefClient();
            cefMessageRouter = CefMessageRouter.create();
            messageHandler = new TroubleshootingMessageRouterHandler(project, jbCefBrowser);
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

            return jbCefBrowser.getComponent();

        } else {
            return null;
        }

    }

    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new TroubleshootingSchemeHandlerFactory(project));
    }

    public static TroubleshootingService getInstance(Project project) {
        return project.getService(TroubleshootingService.class);
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

        return new TroubleshootingIndexTemplateBuilder().build(project);
    }


    public void disposeBrowser() {
        dispose();
    }
}
