package org.digma.intellij.plugin.document;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.jetbrains.annotations.*;

import java.util.List;

public class CodeLensProviderDocumentInfoAndEnvironmentChangedListener implements DocumentInfoChanged, EnvironmentChanged {

    private final Project project;

    public CodeLensProviderDocumentInfoAndEnvironmentChangedListener(Project project) {
        this.project = project;
    }

    @Override
    public void documentInfoChanged(@NotNull VirtualFile file,@NotNull DocumentInfo documentInfo) {
        Backgroundable.executeOnPooledThread(() -> {
            try {
                boolean changed = CodeLensProvider.getInstance(project).buildCodeLens(file,documentInfo);
                if (changed) {
                    project.getMessageBus().syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged(file);
                }
            } catch (Throwable e) {
                ErrorReporter.getInstance().reportError(project, "CodeLensProviderDocumentInfoAndEnvironmentChangedListener.documentInfoChanged", e);
            }
        });
    }

    @Override
    public void environmentChanged(@Nullable Env newEnv) {
        Backgroundable.executeOnPooledThread(() -> {
            try {
                var changedFiles = CodeLensProvider.getInstance(project).refresh();
                if (!changedFiles.isEmpty()) {
                    project.getMessageBus().syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged(changedFiles);
                }
            } catch (Throwable e) {
                ErrorReporter.getInstance().reportError(project, "CodeLensProviderDocumentInfoAndEnvironmentChangedListener.environmentChanged", e);
            }
        });
    }

    @Override
    public void environmentsListChanged(List<Env> newEnvironments) {

    }
}
