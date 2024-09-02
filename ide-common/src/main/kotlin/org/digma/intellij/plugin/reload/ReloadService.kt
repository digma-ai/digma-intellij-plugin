package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.APP)
class ReloadService : DisposableAdaptor {

    private val logger = Logger.getInstance(ReloadService::class.java)

    private val reloadables = mutableListOf<ReloadablePanel>()

    private val latestReloadForProject = mutableMapOf<String, Instant>()

    private val myReloadLock = ReentrantLock(true)


    fun register(reloadablePanel: ReloadablePanel, parentDisposable: Disposable) {
        reloadables.add(reloadablePanel)
        Disposer.register(parentDisposable) {
            remove(reloadablePanel)
        }
    }

    fun remove(reloadablePanel: ReloadablePanel) {
        reloadables.remove(reloadablePanel)
    }


    fun reloadAllProjects() {
        ProjectManager.getInstance().openProjects.forEach {
            try {
                if (isProjectValid(it)) {
                    reload(it)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, it, e, "error in reload for project {}", it)
                ErrorReporter.getInstance().reportError("ReloadService.reload", e)
            }
        }
    }

    fun reload(project: Project) {

        Log.log(logger::trace, "Reload for project {} called", project.name)

        if (!isProjectValid(project)) {
            Log.log(logger::trace, "Not reloading project {} because already invalid", project.name)
            return
        }

        if (isProjectReloadedLately(project)) {
            Log.log(logger::trace, "Not reloading project {} because reloaded lately", project.name)
            return
        }

        myReloadLock.withLock {
            if (isProjectReloadedLately(project)) {
                Log.log(logger::trace, "Not reloading project {} because reloaded lately", project.name)
                return
            }

            latestReloadForProject[project.name] = Clock.System.now()
            reloadImpl(project)
        }
    }

    private fun reloadImpl(project: Project) {

        if (!isProjectValid(project)) {
            return
        }

        Log.log(logger::trace, "Reloading jcef for project {}", project.name)

        EDT.ensureEDT {
            try {
                if (isProjectValid(project)) {
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                    MainToolWindowCardsController.getInstance(project).troubleshootingFinished()
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error in reload for project {}", project)
                ErrorReporter.getInstance().reportError("ReloadService.reload", e)
            }
        }


        reloadables.filter { it.getProject().name == project.name }.forEach {
            EDT.ensureEDT {
                try {
                    if (isProjectValid(project)) {
                        Log.log(logger::trace, "Reloading {} for project {}", it::class.simpleName, it.getProject().name)
                        it.reload()
                    }
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "error in reload for project {}", project)
                    ErrorReporter.getInstance().reportError("ReloadService.reload", e)
                }
            }
        }

        //without hiding and showing tool window the jcef doesn't always refresh on macOS
        try {
            if (isProjectValid(project)) {
                EDT.ensureEDT {
                    Log.log(logger::trace, "Reloading tool windows for project {}", project.name)
                    if (ToolWindowShower.getInstance(project).isToolWindowVisible()) {
                        ToolWindowShower.getInstance(project).hideToolWindow()
                        ToolWindowShower.getInstance(project).showToolWindow()
                    }
                    if (RecentActivityToolWindowShower.getInstance(project).isToolWindowVisible()) {
                        RecentActivityToolWindowShower.getInstance(project).hideToolWindow()
                        RecentActivityToolWindowShower.getInstance(project).showToolWindow()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in reload for project {}", project)
            ErrorReporter.getInstance().reportError("ReloadService.reload", e)
        }
    }


    private fun isProjectReloadedLately(project: Project): Boolean {
        return latestReloadForProject[project.name]?.let {
            val duration = Clock.System.now() - it
            duration <= 3.seconds
        } ?: false
    }


}