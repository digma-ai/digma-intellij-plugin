package org.digma.intellij.plugin.assets;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.editor.colors.impl.FontPreferencesImpl;
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

import static org.digma.intellij.plugin.ui.list.ListCommonKt.listBackground;

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
                .build();
        registerAppSchemeHandler(project);

        var jbCefClient = jbCefBrowser.getJBCefClient();
        cefMessageRouter = CefMessageRouter.create();
        messageHandler = new AssetsMessageRouterHandler(project,parentDisposable, jbCefBrowser);
        cefMessageRouter.addHandler(messageHandler, true);
        jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());
        add(jbCefBrowser.getComponent());
        setBackground(listBackground());

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


        //todo: temporary, this is a very bad way to do it, waiting for help from jetbrains developers
        var fontPreferences =  AppEditorFontOptions.getInstance().getFontPreferences();
        if (fontPreferences instanceof FontPreferencesImpl){
            ((FontPreferencesImpl)AppEditorFontOptions.getInstance().getFontPreferences()).addChangeListener(e -> changeCodeFont(),this);
        }

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

  private void changeCodeFont() {
        String fontName = AppEditorFontOptions.getInstance().getFontPreferences().getFontFamily();
        if (StringUtils.isNotEmpty(fontName) && messageHandler != null) {
            messageHandler.sendRequestToChangeCodeFont(fontName);
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
