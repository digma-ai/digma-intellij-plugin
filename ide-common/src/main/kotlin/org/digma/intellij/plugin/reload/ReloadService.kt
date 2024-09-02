package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.panels.ReloadablePanel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service(Service.Level.APP)
class ReloadService : DisposableAdaptor {

    private val logger = Logger.getInstance(ReloadService::class.java)

    private val reloadables = mutableListOf<ReloadablePanel>()

    private val myReloadAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    private val myReloadLock = ReentrantLock(true)
    private val myEDTReloadLock = ReentrantLock(true)


    fun register(reloadablePanel: ReloadablePanel, parentDisposable: Disposable) {
        reloadables.add(reloadablePanel)
        Disposer.register(parentDisposable) {
            remove(reloadablePanel)
        }
    }

    fun remove(reloadablePanel: ReloadablePanel) {
        reloadables.remove(reloadablePanel)
    }


    fun reload(delay: Long = 3000) {
        myReloadLock.withLock {
            //the delay is to prevent multiple reloads in case multiple events
            // arrive at the same time
            myReloadAlarm.cancelAllRequests()
            myReloadAlarm.addRequest({
                reloadImpl()
            }, delay)
        }
    }

    private fun reloadImpl() {
        Log.log(logger::trace, "Reloading...")
        ProjectManager.getInstance().openProjects.forEach {
            try {
                if (isProjectValid(it)) {
                    MainToolWindowCardsController.getInstance(it).wizardFinished()
                    MainToolWindowCardsController.getInstance(it).troubleshootingFinished()
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("ReloadService.reload", e)
            }
        }

        reloadables.forEach {
            EDT.ensureEDT {
                try {
                    myEDTReloadLock.withLock {
                        Log.log(logger::trace, "Reloading {} for project {}", it::class.simpleName, it.getProject().name)
                        it.reload()
                    }
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("ReloadService.reload", e)
                }
            }
        }

        //without hiding and showing tool window the jcef doesn't always refresh on macOS
        ProjectManager.getInstance().openProjects.forEach {
            try {
                if (isProjectValid(it)) {
                    EDT.ensureEDT {
                        Log.log(logger::trace, "Reloading tool windows for project {}...", it.name)
                        if (ToolWindowShower.getInstance(it).isToolWindowVisible()) {
                            ToolWindowShower.getInstance(it).hideToolWindow()
                            ToolWindowShower.getInstance(it).showToolWindow()
                        }
                        if (RecentActivityToolWindowShower.getInstance(it).isToolWindowVisible()) {
                            RecentActivityToolWindowShower.getInstance(it).hideToolWindow()
                            RecentActivityToolWindowShower.getInstance(it).showToolWindow()
                        }
                    }
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("ReloadService.reload", e)
            }
        }
    }

}