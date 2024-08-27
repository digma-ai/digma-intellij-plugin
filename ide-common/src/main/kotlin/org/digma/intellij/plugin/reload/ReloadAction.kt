package org.digma.intellij.plugin.reload

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import org.digma.intellij.plugin.errorreporting.ErrorReporter

class ReloadAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        try {
            service<ReloadService>().reload()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ReloadAction.actionPerformed", e)
        }
    }
}