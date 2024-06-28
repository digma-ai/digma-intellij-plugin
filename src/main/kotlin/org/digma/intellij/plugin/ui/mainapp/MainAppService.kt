package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.navigation.ViewChangedEvent
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.sendCurrentViewsState

@Service(Service.Level.PROJECT)
class MainAppService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null


    companion object {
        @JvmStatic
        fun getInstance(project: Project): MainAppService {
            return project.service<MainAppService>()
        }
    }

    init {
        project.messageBus.connect(this).subscribe(
            ViewChangedEvent.VIEW_CHANGED_TOPIC, ViewChangedEvent { views, createHistoryStep ->
                jCefComponent?.let {
                    sendCurrentViewsState(it.jbCefBrowser.cefBrowser, MAIN_SET_VIEWS_ACTION, views, createHistoryStep)
                }
            })
    }


    override fun dispose() {
        this.jCefComponent = null
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

}