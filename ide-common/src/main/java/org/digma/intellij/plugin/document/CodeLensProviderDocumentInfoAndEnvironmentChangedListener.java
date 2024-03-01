package org.digma.intellij.plugin.document;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.env.Env;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;

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
                CodeLensProvider.getInstance(project).buildCodeLens(psiFile);
                //todo: refresh editor
            } catch (AnalyticsServiceException e) {
                ErrorReporter.getInstance().reportError("CodeLensProviderDocumentInfoAndEnvironmentChangedListener.documentInfoChanged", e);
            }
        });
    }

    @Override
    public void environmentChanged(Env newEnv) {
        Backgroundable.executeOnPooledThread(() -> {
            try {
                CodeLensProvider.getInstance(project).refresh();
                //todo:refresh editor
            } catch (Throwable e) {
                ErrorReporter.getInstance().reportError("CodeLensProviderDocumentInfoAndEnvironmentChangedListener.environmentChanged", e);
            }
        });
    }

    @Override
    public void environmentsListChanged(List<Env> newEnvironments) {

    }
}
