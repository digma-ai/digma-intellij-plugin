package org.digma.intellij.plugin.jaegerui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.*;
import org.digma.intellij.plugin.auth.account.CredentialsHolder;
import org.digma.intellij.plugin.common.JsonUtilsKt;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.reload.ReloadObserver;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.net.*;

import static org.digma.intellij.plugin.psi.LanguageService.LOGGER;
import static org.digma.intellij.plugin.ui.jcef.JBcefBrowserPropertiesKt.JCEF_JAEGER_UI_FILE_PROPERTY_NAME;

public class JaegerUIFileEditor extends UserDataHolderBase implements FileEditor {
    private final Logger logger = Logger.getInstance(JaegerUIFileEditor.class);
    private final JaegerUIVirtualFile file;

    @Nullable
    private JCefComponent jCefComponent;

    private boolean disposed = false;

    public JaegerUIFileEditor(Project project, JaegerUIVirtualFile file) {
        this.file = file;
        jCefComponent = createJcefComponent(project, file);
        setAuthCookie(project, file);
        ApplicationManager.getApplication().getService(ReloadObserver.class).register(project, "JaegerUI." + file.getName(), jCefComponent.getComponent(), this);
    }

    private void setAuthCookie(Project project, JaegerUIVirtualFile file){
        var jaegerBaseUrl = file.getJaegerBaseUrl();
        var digmaCredentials = CredentialsHolder.INSTANCE.getDigmaCredentials();
        if (digmaCredentials == null)
            return;

        try {
            var jaegerDomain = new URI(jaegerBaseUrl).getHost();
            var cookie = new JBCefCookie("auth_token", JsonUtilsKt.objectToJson(digmaCredentials), jaegerDomain, "/", true, true);
            jCefComponent.getJbCefBrowser().getJBCefCookieManager().setCookie(jaegerBaseUrl, cookie);
//            jCefComponent.getJbCefBrowser().openDevtools();
        } catch (Exception e) {
            Log.debugWithException(logger, e, "Failed to set auth cookie to jaeger", e.getMessage());
        }
    }

    @Nullable
    private JCefComponent createJcefComponent(Project project, JaegerUIVirtualFile file) {

        if (JBCefApp.isSupported()) {
            return new JCefComponent.JCefComponentBuilder(project, "JaegerUI", this,
                    JaegerUIConstants.JAEGER_UI_URL,
                    new JaegerUIMessageRouterHandler(project))
                    .withArg(JCEF_JAEGER_UI_FILE_PROPERTY_NAME, file)
                    .withDownloadAdapter(new DownloadHandlerAdapter())
                    .build();

        } else {
            return null;
        }
    }


    @Override
    public VirtualFile getFile() {
        return file;
    }

    private JComponent getMyComponent() {
        if (jCefComponent != null) {
            return jCefComponent.getComponent();
        } else {
            return new JLabel("JCEF not supported");
        }
    }


    @Override
    public @NotNull JComponent getComponent() {
        return getMyComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return getMyComponent();
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
        if (jCefComponent != null) {
            Disposer.dispose(jCefComponent);
            jCefComponent = null;
        }
        disposed = true;
    }

}
