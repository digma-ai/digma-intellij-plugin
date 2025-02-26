package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.apache.commons.codec.digest.DigestUtils
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.APP)
class ReloadService : DisposableAdaptor {

    private val logger = Logger.getInstance(ReloadService::class.java)

    private val reloadables = mutableListOf<ReloadableJCefContainer>()

    private val latestReloadForProject = mutableMapOf<String, Instant>()

    private val myReloadLock = ReentrantLock(true)


    fun register(reloadableJCefContainer: ReloadableJCefContainer, parentDisposable: Disposable) {
        reloadables.add(reloadableJCefContainer)
        Disposer.register(parentDisposable) {
            remove(reloadableJCefContainer)
        }
    }

    fun remove(reloadableJCefContainer: ReloadableJCefContainer) {
        reloadables.remove(reloadableJCefContainer)
    }


    fun reloadAllProjects(source: ReloadSource) {
        ProjectManager.getInstance().openProjects.forEach {
            try {
                if (isProjectValid(it)) {
                    reload(it, source)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, it, e, "error in reload for project {}", it)
                ErrorReporter.getInstance().reportError("ReloadService.reload", e)
            }
        }
    }

    fun reload(project: Project, source: ReloadSource) {

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

            //run the whole project reload on EDT, it may cause a short freeze, but we are aware of that.
            //it is necessary for finding the selected editor
            EDT.ensureEDT {
                reloadImpl(project, source)
            }
        }
    }

    private fun reloadImpl(project: Project, source: ReloadSource) {

        if (!isProjectValid(project)) {
            return
        }

        ActivityMonitor.getInstance(project).registerCustomEvent(
            "ReloadJcef", mapOf(
                "source" to source.toString(),
                "project.hash" to DigestUtils.md2Hex(project.name)
            )
        )

        Log.log(logger::trace, "Reloading jcef for project {}", project.name)

        //todo: maybe we want to close the wizard or troubleshooting on reload?
//        EDT.ensureEDT {
//            try {
//                if (isProjectValid(project)) {
//                    MainToolWindowCardsController.getInstance(project).wizardFinished()
//                    MainToolWindowCardsController.getInstance(project).troubleshootingFinished()
//                }
//            } catch (e: Throwable) {
//                Log.warnWithException(logger, project, e, "error in reload for project {}", project)
//                ErrorReporter.getInstance().reportError("ReloadService.reload", e)
//            }
//        }

        //because of the way we reload jaeger ui and dashboard by closing and opening the files again,
        // we want to try and restore the selected editor
        val selectedEditor = if (EDT.isEdt()) {
            FileEditorManager.getInstance(project).selectedEditor
        } else {
            null
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

        EDT.ensureEDT {
            selectedEditor?.takeIf { it.file.isValid }?.let {
                FileEditorManager.getInstance(project).openFile(it.file)
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