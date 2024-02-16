package org.digma.intellij.plugin.codelens

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.SupportedLanguages

class CodeLensRefresh(
    private val project: Project,
    private val codeLensProvider: CodeLensProvider,
) {


    fun start() {
        @Suppress("UnstableApiUsage")
        codeLensProvider.disposingScope().launch {

            while (isActive) {

                delay(60000)

                if (isActive) {
                    SupportedLanguages.values().forEach { language ->
                        if (isActive) {
                            LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)?.refreshCodeLens()
                        }
                    }
                }
            }
        }
    }
}