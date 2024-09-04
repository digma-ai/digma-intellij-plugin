package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;

import static org.digma.intellij.plugin.ui.jcef.JBcefBrowserPropertiesKt.JCEF_DOCUMENTATION_FILE_PROPERTY_NAME;

public class DocumentationFileEditor extends UserDataHolderBase implements FileEditor {

    private final DocumentationVirtualFile file;

    @Nullable
    private JCefComponent jCefComponent;

    private boolean disposed = false;

    public DocumentationFileEditor(Project project, DocumentationVirtualFile file) {
        this.file = file;
        jCefComponent = createJcefComponent(project, file);
    }

    @Nullable
    private JCefComponent createJcefComponent(Project project, DocumentationVirtualFile file) {

        if (JBCefApp.isSupported()) {
            return new JCefComponent.JCefComponentBuilder(project, "Documentation", this,
                    DocumentationConstants.DOCUMENTATION_URL,
                    new DocumentationMessageRouterHandler(project))
                    .withArg(JCEF_DOCUMENTATION_FILE_PROPERTY_NAME,file)
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
