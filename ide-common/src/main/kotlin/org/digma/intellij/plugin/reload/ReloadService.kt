package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.ui.panels.ReloadablePanel

@Service(Service.Level.APP)
class ReloadService {

    private val reloadables = mutableListOf<ReloadablePanel>()


    fun register(reloadablePanel: ReloadablePanel, parentDisposable: Disposable) {
        reloadables.add(reloadablePanel)
        Disposer.register(parentDisposable) {
            remove(reloadablePanel)
        }
    }

    fun remove(reloadablePanel: ReloadablePanel) {
        reloadables.remove(reloadablePanel)
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