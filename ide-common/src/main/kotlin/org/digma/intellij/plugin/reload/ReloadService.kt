package org.digma.intellij.plugin.reload

import com.intellij.openapi.components.Service
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.ui.panels.ReloadablePanel

@Service(Service.Level.APP)
class ReloadService {

    private val reloadables = mutableListOf<ReloadablePanel>()


    fun register(reloadablePanel: ReloadablePanel) {
        reloadables.add(reloadablePanel)
    }


    fun reload() {
        reloadables.forEach {
            EDT.ensureEDT {
                try {
                    it.reload()
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("ReloadService.reload", e)
                }
            }
        }
    }


}