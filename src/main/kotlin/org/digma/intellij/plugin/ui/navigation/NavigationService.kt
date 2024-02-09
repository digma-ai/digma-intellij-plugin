package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.navigation.ViewChangedEvent
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.sendCurrentView

@Service(Service.Level.PROJECT)
class NavigationService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null


    companion object {
        @JvmStatic
        fun getInstance(project: Project): NavigationService {
            return project.service<NavigationService>()
        }
    }

    init {
        project.messageBus.connect(this).subscribe(
            ViewChangedEvent.VIEW_CHANGED_TOPIC, ViewChangedEvent { viewInfo ->
                jCefComponent?.let {
                    sendCurrentView(it.jbCefBrowser.cefBrowser, viewInfo)
                }
            })
    }


    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }


}