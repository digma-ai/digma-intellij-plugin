package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.insights.InsightsServiceImpl
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.jcef.common.UserRegistrationEvent
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.sendUserEmail
import org.digma.intellij.plugin.ui.tests.TestsService

@Service(Service.Level.PROJECT)
class InsightsService(val project: Project) : InsightsServiceImpl(project) {

    private var jCefComponent: JCefComponent? = null

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsService {
            return project.service<InsightsService>()
        }
    }

    init {
        //TestsService depends on InsightsModelReact.scope so make sure its initialized and listening.
        // It may also be called from TestsPanel, whom even comes first
        project.getService(TestsService::class.java)

        SettingsState.getInstance().addChangeListener({
            jCefComponent?.let {
                JCefBrowserUtil.sendRequestToChangeTraceButtonEnabled(it.jbCefBrowser)
            }
        }, this)


        project.messageBus.connect(this).subscribe(UserRegistrationEvent.USER_REGISTRATION_TOPIC, UserRegistrationEvent { email ->
            jCefComponent?.let {
                sendUserEmail(it.jbCefBrowser.cefBrowser, email)
            }

        })

    }

    fun setJCefComponent(jCefComponent: JCefComponent?) {
        this.jCefComponent = jCefComponent
    }

}