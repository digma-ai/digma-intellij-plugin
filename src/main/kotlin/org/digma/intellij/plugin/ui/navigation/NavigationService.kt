package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class NavigationService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): NavigationService {
            return project.service<NavigationService>()
        }
    }

    override fun dispose() {
        //nothing to do , used as parent disposable
    }


}