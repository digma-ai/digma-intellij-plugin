package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.*;
import org.cef.CefApp;
import org.cef.browser.*;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.digma.intellij.plugin.reload.ReloadObserver;
import org.digma.intellij.plugin.ui.jcef.JBCefBrowserBuilderCreator;
import org.digma.intellij.plugin.ui.settings.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;

import static org.digma.intellij.plugin.ui.jcef.JCefMessagesUtilsKt.*;

public class DashboardFileEditor extends UserDataHolderBase implements FileEditor {

    static final String RESOURCE_FOLDER_NAME = "dashboard";
    static final String TEMPLATE_FOLDER_NAME = "/webview/dashboard";
    static final String DOMAIN_NAME = "dashboard";
    static final String SCHEMA_NAME = "http";

    private final VirtualFile file;
    private JBCefBrowser jbCefBrowser = null;
    private CefMessageRouter cefMessageRouter = null;
    private boolean disposed = false;

    public DashboardFileEditor(Project project, VirtualFile file) {
        this.file = file;

        if (JBCefApp.isSupported()) {

            jbCefBrowser = JBCefBrowserBuilderCreator.create()
                    .setUrl("http://" + DOMAIN_NAME + "/" + RESOURCE_FOLDER_NAME + "/index.html")
                    .build();

            var jbCefClient = jbCefBrowser.getJBCefClient();
            cefMessageRouter = CefMessageRouter.create();
            cefMessageRouter.addHandler(new DashboardMessageRouterHandler(project), true);
            jbCefClient.getCefClient().addMessageRouter(cefMessageRouter);


            ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(new SettingsChangeListener() {
                @Override
                public void systemFontChange(@NotNull String fontName) {
                    sendRequestToChangeFont(fontName, jbCefBrowser);
                }

                @Override
                public void systemThemeChange(@NotNull Theme theme) {
                    sendRequestToChangeUiTheme(theme, jbCefBrowser);
                }

                @Override
                public void editorFontChange(@NotNull String fontName) {
                    sendRequestToChangeCodeFont(fontName, jbCefBrowser);
                }
            }, this);

            var lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
                @Override
                public void onAfterCreated(CefBrowser browser) {
                    registerAppSchemeHandler(project, (DashboardVirtualFile) file);
                }
            };

            jbCefBrowser.getJBCefClient().addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser());

            Disposer.register(this, () -> jbCefBrowser.getJBCefClient().removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.getCefBrowser()));

            ApplicationManager.getApplication().getService(ReloadObserver.class).register(project, "Dashboard." + file.getName(), jbCefBrowser.getComponent(), this);
        }
    }

    private void registerAppSchemeHandler(Project project, DashboardVirtualFile file) {
        CefApp.getInstance().registerSchemeHandlerFactory("http", DOMAIN_NAME,
                new DashboardSchemeHandlerFactory(project, file));
    }

    @Override
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return getMyComponent();
    }


    private JComponent getMyComponent() {
        if (jbCefBrowser != null) {
            return jbCefBrowser.getComponent();
        } else {
            return new JLabel("JCEF not supported");
        }
    }


    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        if (jbCefBrowser != null) {
            return jbCefBrowser.getComponent();
        } else {
            return null;
        }
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
        return !disposed;
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
        if (jbCefBrowser != null) {
            Disposer.dispose(jbCefBrowser);
            cefMessageRouter.dispose();
        }
        disposed = true;
    }

}
