package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class DocumentationFileEditor extends UserDataHolderBase implements FileEditor {

    private final VirtualFile file;

    @Nullable
    private final JCefComponent jCefComponent;


    public DocumentationFileEditor(Project project, DocumentationVirtualFile file) {
        this.file = file;
        jCefComponent = createJcefComponent(project, file);
    }

    @Nullable
    private JCefComponent createJcefComponent(Project project, DocumentationVirtualFile file) {

        if (JBCefApp.isSupported()) {
            return new JCefComponent.JCefComponentBuilder(project, "Documentation", DocumentationService.getInstance(project))
                    .url(DocumentationConstants.DOCUMENTATION_URL)
                    .addMessageRouterHandler(new DocumentationMessageRouterHandler(project))
                    .schemeHandlerFactory(new DocumentationSchemeHandlerFactory(project, file))
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
        if (jCefComponent != null) {
            jCefComponent.dispose();
        }
    }

}
