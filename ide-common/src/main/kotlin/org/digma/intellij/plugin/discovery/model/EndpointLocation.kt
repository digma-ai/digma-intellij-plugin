package org.digma.intellij.plugin.discovery.model

import com.intellij.openapi.vfs.VirtualFile

class EndpointLocation(val file: VirtualFile, val endpointId: String, val offset: Int, val methodCodeObjectId: String) {
    fun isAlive(): Boolean = file.isValid
}