package org.digma.intellij.plugin.reload

import com.intellij.openapi.project.Project

interface ReloadableJCefContainer {
    fun reload()
    fun getProject(): Project
}