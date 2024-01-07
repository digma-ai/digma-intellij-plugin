package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class TestsService(val project: Project) : Disposable {
    override fun dispose() {
        //nothing to do
    }

}