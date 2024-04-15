package org.digma.intellij.plugin.ui.wizard

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser

//todo: refactor installation wizard to be managed by a service
@Service(Service.Level.PROJECT)
class InstallationWizardService(
    private val project: Project
) : Disposable {
    private var jbCefBrowser: JBCefBrowser? = null


    companion object {
        fun getInstance(project: Project): InstallationWizardService {
            return project.getService(InstallationWizardService::class.java)
        }
    }

    override fun dispose() {
        //nothing to do
    }

    fun setJcefBrowser(jbCefBrowser: JBCefBrowser) {
        this.jbCefBrowser = jbCefBrowser
    }

}
