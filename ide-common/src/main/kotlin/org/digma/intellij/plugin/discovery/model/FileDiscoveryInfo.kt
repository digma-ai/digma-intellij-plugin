package org.digma.intellij.plugin.discovery.model

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile

class FileDiscoveryInfo(
    val file: VirtualFile,
    val language: Language? = null,
    val methods: MutableMap<String, MethodDiscoveryInfo> = mutableMapOf()
) {

    fun isAlive(): Boolean = file.isValid

    override fun toString(): String {
        return "FileDiscoveryInfo(file=$file, language=$language, methods=$methods)"
    }

    //todo: equals and HashCode and toString


}