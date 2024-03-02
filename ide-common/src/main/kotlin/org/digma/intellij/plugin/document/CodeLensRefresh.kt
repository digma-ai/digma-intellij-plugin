package org.digma.intellij.plugin.document

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.errorreporting.ErrorReporter

class CodeLensRefresh(
    private val project: Project,
    private val codeLensProvider: CodeLensProvider,
) {


    //call CodeLensProvider.refresh every 20 seconds.
    //if the lens data has changed will fire codelensChanged event

    fun start() {

        @Suppress("UnstableApiUsage")
        codeLensProvider.disposingScope().launch {
            while (isActive) {
                delay(20000)
                if (isActive) {
                    try {
                        val changed = CodeLensProvider.getInstance(project).refresh()
                        if (changed) {
                            project.messageBus.syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged()
                        }
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("CodeLensRefresh.CodeLensProvider.refresh", e)
                    }
                }
            }
        }
    }
}