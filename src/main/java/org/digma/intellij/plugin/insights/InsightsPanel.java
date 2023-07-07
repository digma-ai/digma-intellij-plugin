package org.digma.intellij.plugin.insights;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import org.cef.CefApp;
import org.cef.browser.CefMessageRouter;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier;
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static org.digma.intellij.plugin.ui.list.ListCommonKt.listBackground;

public class InsightsPanel extends JPanel implements Disposable {

    static final String RESOURCE_FOLDER_NAME = "/webview/insights";
    static final String DOMAIN_NAME = "insights";
    static final String SCHEMA_NAME = "http";

    private final transient JBCefBrowser jbCefBrowser;
    private final transient CefMessageRouter cefMessageRouter;
    private final transient InsightsMessageRouterHandler messageHandler;

    public InsightsPanel(Project project,Disposable parentDisposable) {

        Disposer.register(parentDisposable,this);

        jbCefBrowser = JBCefBrowserBuilderCreator.create()
                .setUrl("http://" + DOMAIN_NAME + "/index.html")
                .build();
        registerAppSchemeHandler(project);

        var jbCefClient = jbCefBrowser.getJBCefClient();
        cefMessageRouter = CefMessageRouter.create();
        messageHandler = new InsightsMessageRouterHandler(project,parentDisposable, jbCefBrowser);
        InsightsService.getInstance(project).setMessageRouter(messageHandler);
        cefMessageRouter.addHandler(messageHandler, true);
        jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());
        add(jbCefBrowser.getComponent());
        setBackground(listBackground());


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
    }


    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new InsightsSchemeHandlerFactory(project));
    }



    @Override
    public void dispose() {
        if (jbCefBrowser != null){
            jbCefBrowser.dispose();
        }
        if(cefMessageRouter != null){
            cefMessageRouter.dispose();
        }
        if (messageHandler != null) {
            messageHandler.dispose();
        }
    }
}
