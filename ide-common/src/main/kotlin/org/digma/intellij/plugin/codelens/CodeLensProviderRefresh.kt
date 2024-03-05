package org.digma.intellij.plugin.codelens

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.document.CodeLensChanged
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter

/**
 * this is a repeatable call to CodeLensProvider.refresh
 */
class CodeLensProviderRefresh(
    private val project: Project,
    private val parentDisposable: Disposable,
) {


    //call CodeLensProvider.refresh every 20 seconds.
    //if the lens data has changed will fire codelensChanged event

    fun start() {

        @Suppress("UnstableApiUsage")
        parentDisposable.disposingScope().launch {
            while (isActive) {
                delay(20000)
                if (isActive) {
                    try {
                        val changedPsiFiles = CodeLensProvider.getInstance(project).refresh()
                        if (changedPsiFiles.isNotEmpty()) {
                            project.messageBus.syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged(changedPsiFiles)
                        }
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("CodeLensProviderRefresh.CodeLensProvider.refresh", e)
                    }
                }
            }
        }
    }
}