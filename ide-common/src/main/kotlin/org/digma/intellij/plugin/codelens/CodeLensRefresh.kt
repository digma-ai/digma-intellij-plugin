package org.digma.intellij.plugin.codelens

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.SupportedLanguages

class CodeLensRefresh(
    private val project: Project,
    private val codeLensProvider: CodeLensProvider,
) {

    //two coroutines:
    //one calls refresh on CodeLensProvider to refresh its backend data
    //one calls refresh for the editor to refresh code visions.
    //they are not related and not synchronized but at the end the code lens are refreshed every some seconds.
    //it is made to separate the calls to the backend from the UI code vision refresh


    fun start() {

        @Suppress("UnstableApiUsage")
        codeLensProvider.disposingScope().launch {
            while (isActive) {
                delay(20000)
                if (isActive) {
                    try {
                        CodeLensProvider.getInstance(project).refresh()
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("CodeLensRefresh.CodeLensProvider.refresh", e)
                    }
                }
            }
        }



        @Suppress("UnstableApiUsage")
        codeLensProvider.disposingScope().launch {

            while (isActive) {

                delay(60000)

                if (isActive) {
                    SupportedLanguages.values().forEach { language ->
                        if (isActive) {
                            try {
                                LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)?.refreshCodeLens()
                            } catch (e: Throwable) {
                                ErrorReporter.getInstance().reportError("CodeLensRefresh.refreshCodeLens", e)
                            }
                        }
                    }
                }
            }
        }
    }
}