package org.digma.intellij.plugin.codelens.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.rest.environment.Env

class CodeLensProviderListeners(private val project: Project) : DocumentInfoChanged, EnvironmentChanged {

    override fun documentInfoChanged(file: VirtualFile, documentInfo: DocumentInfo) {
        CodeLensProvider.getInstance(project).loadCodeLens(file,documentInfo)
    }

    override fun documentInfoRemoved(file: VirtualFile) {
        CodeLensProvider.getInstance(project).removeCodeLens(file)
    }

    override fun environmentChanged(newEnv: Env?) {
        CodeLensProvider.getInstance(project).refresh()
    }

    override fun environmentsListChanged(newEnvironments: List<Env>) {

    }
}