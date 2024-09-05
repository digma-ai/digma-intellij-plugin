package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.JCefComponent

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


    override fun dispose() {
        this.jCefComponent = null
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

}