package org.digma.intellij.plugin.codelens.provider

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

interface CodeLensChanged {

    companion object {
        @JvmField
        @Topic.ProjectLevel
        val CODELENS_CHANGED_TOPIC: Topic<CodeLensChanged> = Topic.create<CodeLensChanged>("CODELENS_CHANGED_TOPIC", CodeLensChanged::class.java)
    }

    fun codelensChanged(virtualFile: VirtualFile){}
    fun codelensRemoved(virtualFile: VirtualFile){}
    fun codelensCleared(){}
}
