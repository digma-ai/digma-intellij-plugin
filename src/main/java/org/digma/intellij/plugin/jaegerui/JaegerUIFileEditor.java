package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class JaegerUIFileEditor extends UserDataHolderBase implements FileEditor {

    static final String RESOURCE_FOLDER_NAME = "/webview/jaegerui";
    static final String DOMAIN_NAME = "jaegerui";
    static final String SCHEMA_NAME = "http";

    private final VirtualFile file;
    private final JBCefBrowser jbCefBrowser;
    private final CefMessageRouter cefMessageRouter;

    public JaegerUIFileEditor(Project project, VirtualFile file) {
        this.file = file;
        jbCefBrowser = JBCefBrowserBuilderCreator.create()
                .setUrl("http://" + DOMAIN_NAME + "/index.html")
                .build();

        var jbCefClient = jbCefBrowser.getJBCefClient();
        cefMessageRouter = CefMessageRouter.create();
        cefMessageRouter.addHandler(new JaegerUIMessageRouterHandler(project, jbCefBrowser), true);
        jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);

        var lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                registerAppSchemeHandler(project, (JaegerUIVirtualFile) file);
            }
        };

        jbCefBrowser.getJBCefClient().addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser());

        Disposer.register(this, () -> jbCefBrowser.getJBCefClient().removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser()));


    }



    private void registerAppSchemeHandler(Project project, JaegerUIVirtualFile file) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new JaegerUISchemeHandlerFactory(project,file));
    }



    @Override
    public VirtualFile getFile() {
        return file;
    }


    @Override
    public @NotNull JComponent getComponent() {
        return jbCefBrowser.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return jbCefBrowser.getComponent();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return file.getName();
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        //nothing to do
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        //nothing to do
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        //nothing to do
    }

    @Override
    public void dispose() {
        jbCefBrowser.dispose();
        cefMessageRouter.dispose();
    }

}
