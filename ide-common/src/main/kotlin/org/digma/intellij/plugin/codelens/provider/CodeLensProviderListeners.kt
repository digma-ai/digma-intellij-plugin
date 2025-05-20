package org.digma.intellij.plugin.codelens.provider

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.rest.environment.Env

class CodeLensProviderListeners(private val project: Project) : DocumentInfoChanged, EnvironmentChanged {

    private val logger = Logger.getInstance(this::class.java)

    override fun documentInfoChanged(file: VirtualFile, documentInfo: DocumentInfo) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, project, "CodeLensProviderListeners.documentInfoChanged {}", file)
        }
        CodeLensProvider.getInstance(project).loadCodeLens(file,documentInfo)
    }

    override fun documentInfoRemoved(file: VirtualFile) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, project, "CodeLensProviderListeners.documentInfoRemoved {}", file)
        }
        CodeLensProvider.getInstance(project).removeCodeLens(file)
    }

    override fun environmentChanged(newEnv: Env?) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, project, "CodeLensProviderListeners.environmentChanged {}", newEnv)
        }
        CodeLensProvider.getInstance(project).refresh()
    }

    override fun environmentsListChanged(newEnvironments: List<Env>) {

    }
}