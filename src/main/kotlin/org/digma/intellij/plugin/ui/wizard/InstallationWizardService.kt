package org.digma.intellij.plugin.ui.wizard

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import org.digma.intellij.plugin.common.DisposableAdaptor

@Service(Service.Level.PROJECT)
class InstallationWizardService(
    private val project: Project
) : DisposableAdaptor {
    private var jbCefBrowser: JBCefBrowser? = null


    companion object {
        fun getInstance(project: Project): InstallationWizardService {
            return project.getService(InstallationWizardService::class.java)
        }
    }


    fun setJcefBrowser(jbCefBrowser: JBCefBrowser) {
        this.jbCefBrowser = jbCefBrowser
    }

}
