package org.digma.intellij.plugin.idea.psi.java

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.psi.AbstractJvmInstrumentationProvider

class JavaInstrumentationProvider(project: Project, javaLanguageService: JavaLanguageService) :
    AbstractJvmInstrumentationProvider(project, javaLanguageService)