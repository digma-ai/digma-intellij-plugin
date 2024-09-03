package org.digma.intellij.plugin.reload

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import org.digma.intellij.plugin.errorreporting.ErrorReporter

//devkit doesn't know that the action is registered because it looks for plugin.xml in the current project
@Suppress("ComponentNotRegistered")
class ReloadAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        try {
            service<ReloadService>().reloadAllProjects()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ReloadAction.actionPerformed", e)
        }
    }
}