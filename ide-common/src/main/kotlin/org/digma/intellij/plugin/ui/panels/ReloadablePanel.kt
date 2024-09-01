package org.digma.intellij.plugin.ui.panels

import com.intellij.openapi.project.Project

interface ReloadablePanel {
    fun reload()
    fun getProject(): Project
}