package org.digma.intellij.plugin.wizard;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.digma.intellij.plugin.jcef.common.InstallationWizardSetCurrentStepPayload;
import org.digma.intellij.plugin.jcef.common.InstallationWizardSetCurrentStepRequest;
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil;
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class InstallationWizardService {

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

    public void sendCurrentStep(@NotNull String step) {

        if (jbCefBrowser == null) {
            return;
        }

        var payload = new InstallationWizardSetCurrentStepPayload(step);
        var message = JCefBrowserUtil.resultToString(new InstallationWizardSetCurrentStepRequest(
                JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
                JCefMessagesUtils.INSTALLATION_WIZARD_SET_CURRENT_STEP,
                payload));

        JCefBrowserUtil.postJSMessage(message, jbCefBrowser);
    }
}
