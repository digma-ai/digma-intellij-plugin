package org.digma.intellij.plugin.discovery

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo

interface FileDiscoveryProvider {
    suspend fun discover(project: Project, file: VirtualFile): FileDiscoveryInfo
}