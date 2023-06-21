package org.digma.intellij.plugin.assets;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.cef.CefApp;
import org.cef.browser.CefMessageRouter;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.toolwindow.common.ThemeUtil;

import javax.swing.*;
import java.awt.*;

public class AssetsPanel extends JPanel implements Disposable {

    static final String RESOURCE_FOLDER_NAME = "/webview/assets";
    static final String DOMAIN_NAME = "assets";
    static final String SCHEMA_NAME = "http";

    private final transient JBCefBrowser jbCefBrowser;
    private final transient CefMessageRouter cefMessageRouter;
    private final transient AssetsMessageRouterHandler messageHandler;

    public AssetsPanel(Project project,Disposable parentDisposable) {

        Disposer.register(parentDisposable,this);

        jbCefBrowser = JBCefBrowserBuilderCreator.create()
                .setUrl("http://" + DOMAIN_NAME + "/index.html")
                .setEnableOpenDevToolsMenuItem(true)
                .build();
        registerAppSchemeHandler(project);

        var jbCefClient = jbCefBrowser.getJBCefClient();
        cefMessageRouter = CefMessageRouter.create();
        messageHandler = new AssetsMessageRouterHandler(project, jbCefBrowser);
        cefMessageRouter.addHandler(messageHandler, true);
        jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());
        add(jbCefBrowser.getComponent());

        addPropertyChangeListener(evt -> {
            if (project.isDisposed()) return;
            if ("font".equals(evt.getPropertyName())) {
                changeFont();
            }
        });

        UIManager.addPropertyChangeListener(evt -> {
            if (project.isDisposed()) return;
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                changeTheme();
            }
        });
    }


    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new AssetsSchemeHandlerFactory(project));
    }



    private void changeTheme() {
        String theme = ThemeUtil.getCurrentThemeName();
        if (StringUtils.isNotEmpty(theme) && messageHandler != null) {
            messageHandler.sendRequestToChangeUiTheme(theme);
        }
    }

   private void changeFont() {
        String fontName = getFont().getFontName();
        if (StringUtils.isNotEmpty(fontName) && messageHandler != null) {
            messageHandler.sendRequestToChangeFont(fontName);
        }
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
