package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.codelens.provider.CodeLensChanged

class RiderCodelensListener(private val project: Project) : CodeLensChanged {

    override fun codelensChanged(virtualFile: VirtualFile) {
        CodeLensHost.getInstance(project).refreshFile(virtualFile)
    }
}