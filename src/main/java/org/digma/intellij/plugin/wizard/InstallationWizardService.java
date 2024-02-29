package org.digma.intellij.plugin.wizard;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class InstallationWizardService implements Disposable {

    //todo: refactor installation wizard to be managed by a service

    private final Project project;
    private JBCefBrowser jbCefBrowser;

    public InstallationWizardService(Project project) {
        this.project = project;
    }

    public static InstallationWizardService getInstance(Project project) {
        return project.getService(InstallationWizardService.class);
    }

    public void setJcefBrowser(@NotNull JBCefBrowser jbCefBrowser) {
        this.jbCefBrowser = jbCefBrowser;
    }

    @Override
    public void dispose() {
        //nothing to do
    }

}
