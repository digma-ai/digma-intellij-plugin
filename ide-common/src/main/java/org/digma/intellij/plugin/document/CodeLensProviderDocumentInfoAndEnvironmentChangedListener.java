package org.digma.intellij.plugin.document;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CodeLensProviderDocumentInfoAndEnvironmentChangedListener implements DocumentInfoChanged, EnvironmentChanged {

    private final Project project;

    public CodeLensProviderDocumentInfoAndEnvironmentChangedListener(Project project) {
        this.project = project;
    }

    @Override
    public void documentInfoChanged(PsiFile psiFile) {
        Backgroundable.executeOnPooledThread(() -> {
            try {
                boolean changed = CodeLensProvider.getInstance(project).buildCodeLens(psiFile);
                if (changed) {
                    project.getMessageBus().syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged(psiFile);
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
                var changedPsiFiles = CodeLensProvider.getInstance(project).refresh();
                if (!changedPsiFiles.isEmpty()) {
                    project.getMessageBus().syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged(changedPsiFiles);
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
