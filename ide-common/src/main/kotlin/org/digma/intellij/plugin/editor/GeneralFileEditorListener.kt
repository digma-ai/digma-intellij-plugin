package org.digma.intellij.plugin.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.delay
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService
import org.digma.intellij.plugin.settings.SettingsState
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class GeneralFileEditorListener(val project: Project) : FileEditorManagerListener {
    private val logger: Logger = Logger.getInstance(GeneralFileEditorListener::class.java)

    private val refreshService: RefreshService = project.getService(RefreshService::class.java)

    private val settingsState: SettingsState = project.getService(SettingsState::class.java)

    // lifetimeDefinitionMap keeps info about the lifetime coroutine tasks of each opened file
    private val lifetimeDefinitionMap: ConcurrentHashMap<VirtualFile, LifetimeDefinition> = ConcurrentHashMap()
    private val lifetimeDefinitionMapLock = Object()

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)
        refreshAllInsightsForActiveFile(event.newFile)
        setFocusedDocument(event.newFile)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        super.fileClosed(source, file)
        removeInsightsPaginationInfoForClosedDocument(file.name)
    }

    private fun refreshAllInsightsForActiveFile(newFile: VirtualFile?) {
        if (newFile != null) {
            Log.log(logger::debug, "Starting refreshAllInsightsForActiveFile = {}", newFile)
            // lock is required here to avoid access and modification of the same map from multiple threads
            synchronized(lifetimeDefinitionMapLock) {
                terminateActualCoroutineTaskForPreviouslyFocusedOpenedFile()
                createCoroutineTaskForActualFocusedFile(newFile)
            }
            Log.log(logger::debug, "Finished refreshAllInsightsForActiveFile = {}", newFile)
        }
    }

    private fun createCoroutineTaskForActualFocusedFile(newFile: VirtualFile) {
        // create a lifetime for actual focused file
        // Lifetime by itself is a lightweight and heavily optimized object. It is okay to create new lifetime on each selection change without any performance impact whatsoever.
        val newLifetimeDefinition = LifetimeDefinition()
        newLifetimeDefinition.lifetime.launchBackground {
            // next logic is the Coroutine's body
            while (true) {
                delay(settingsState.refreshDelay.toLong() * 1000)// 10 seconds
                refreshService.refreshAllForCurrentFile(newFile)
            }
        }
        lifetimeDefinitionMap[newFile] = newLifetimeDefinition
    }

    private fun terminateActualCoroutineTaskForPreviouslyFocusedOpenedFile() {
        if (lifetimeDefinitionMap.isNotEmpty()) {
            // terminate the corresponding lifetime, first of all. this will also cancel current task execution
            lifetimeDefinitionMap.forEach { lifetimeDefinition -> lifetimeDefinition.value.terminate(true)}
            // remove the lifetime for previously active file from the map
            lifetimeDefinitionMap.clear()
        }
    }

    private fun setFocusedDocument(newFile: VirtualFile?) {
        if (newFile != null) {
            setFocusedDocumentName(newFile.name)
        } else {
            setFocusedDocumentName("")
        }
    }
}