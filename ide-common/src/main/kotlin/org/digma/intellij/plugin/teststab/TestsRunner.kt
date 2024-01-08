package org.digma.intellij.plugin.teststab

import com.intellij.concurrency.resetThreadContext
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.psi.LanguageService

@Service(Service.Level.PROJECT)
class TestsRunner(val project: Project) {


    fun executeTest(methodId: String): Boolean {

        val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)

        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)

        languageService.executeTestMethod(methodId)


//        languageService.navigateToMethod(methodId)
//
//        val runContextAction = RunContextAction(DefaultRunExecutor())
//
////        runContextAction.actionPerformed()
//
////        runContextAction.
//
//        PlatformTestUtil.getRunConfiguration()

        return true
    }


}