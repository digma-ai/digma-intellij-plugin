package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.CefApp;
import org.cef.browser.CefMessageRouter;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class JaegerUIService implements Disposable {

    static final String RESOURCE_FOLDER_NAME = "/webview/jaegerui";
    static final String DOMAIN_NAME = "jaegerui";
    static final String SCHEMA_NAME = "http";

    private final Project project;

    private JBCefBrowser jbCefBrowser;
    private CefMessageRouter cefMessageRouter;

    public JaegerUIService(Project project) {
        this.project = project;
    }


    public static JaegerUIService getInstance(Project project) {
        return project.getService(JaegerUIService.class);
    }

    @Nullable
    public Component getBrowserComponent() {

        if (!JBCefApp.isSupported()) {
            return null;
        }

        if (jbCefBrowser == null){
            createJBCefBrowser();
        }
        return jbCefBrowser.getComponent();
    }

    private void createJBCefBrowser() {
        jbCefBrowser = JBCefBrowser.createBuilder()
                .setOffScreenRendering(false)
                .setUrl("http://"+DOMAIN_NAME+"/index.html")
                .setEnableOpenDevToolsMenuItem(true)
                .build();

        registerAppSchemeHandler(project);

        var jbCefClient = jbCefBrowser.getJBCefClient();
        cefMessageRouter = CefMessageRouter.create();
        cefMessageRouter.addHandler(new JaegerUIMessageRouterHandler(project,jbCefBrowser),true);
        jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);
    }

    private void registerAppSchemeHandler(Project project) {
        CefApp.getInstance().registerSchemeHandlerFactory("http",DOMAIN_NAME,
                        new JaegerUISchemeHandlerFactory(project));
    }

    @Override
    public void dispose() {
        if (jbCefBrowser != null) {
            jbCefBrowser.dispose();
        }
        if (cefMessageRouter != null){
            cefMessageRouter.dispose();
        }
    }
}


