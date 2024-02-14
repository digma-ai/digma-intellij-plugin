package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.psi.AbstractJvmInstrumentationProvider

class KotlinInstrumentationProvider(project: Project, kotlinLanguageService: KotlinLanguageService) :
    AbstractJvmInstrumentationProvider(project, kotlinLanguageService) {
}