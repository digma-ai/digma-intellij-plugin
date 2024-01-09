package org.digma.intellij.plugin.teststab

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.NonUrgentExecutor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.psi.LanguageService

@Service(Service.Level.PROJECT)
class TestsRunner(val project: Project) {

    fun executeTestMethod(methodId: String): Boolean {

        return ReadAction.nonBlocking<Boolean> {
            try {
                val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
                val psiElement = languageService.getPsiElementForMethod(methodId)
                psiElement?.let {
                    runTestAction(it)
                } ?: false

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("TestsRunner.executeTestMethod", e)
                false
            }
        }.submit(NonUrgentExecutor.getInstance()).get()
    }

    fun executeTestClassByMethodId(methodId: String): Boolean {

        return ReadAction.nonBlocking<Boolean> {
            try {
                val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
                val psiElement = languageService.getPsiElementForClassByMethodId(methodId)
                psiElement?.let {
                    runTestAction(it)
                } ?: false

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("TestsRunner.executeTestClassByMethodId", e)
                false
            }
        }.submit(NonUrgentExecutor.getInstance()).get()
    }


    fun executeTestClassByClassName(className: String): Boolean {

        return ReadAction.nonBlocking<Boolean> {
            try {
                val languageService = LanguageService.findLanguageServiceByClassName(project, className)
                val psiElement = languageService.getPsiElementForClassByName(className)
                psiElement?.let {
                    runTestAction(it)
                } ?: false

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("TestsRunner.executeTestClassByClassName", e)
                false
            }
        }.submit(NonUrgentExecutor.getInstance()).get()
    }


    private fun runTestAction(psiElement: PsiElement): Boolean {

        try {

            val module = ModuleUtilCore.findModuleForPsiElement(psiElement)
            val runContextAction = RunContextAction(DefaultRunExecutor())
            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, module)
                .add(Location.DATA_KEY, PsiLocation(psiElement))
                .add(CommonDataKeys.PROJECT, project)
                .build()

            val event = AnActionEvent(
                /* inputEvent = */ null,
                /* dataContext = */ dataContext,
                /* place = */ "EditorPopup",
                /* presentation = */ runContextAction.getTemplatePresentation().clone(),
                /* actionManager = */ ActionManager.getInstance(),
                /* modifiers = */ 16,
                /* isContextMenuAction = */ true,
                /* isActionToolbar = */ false
            )
            runContextAction.actionPerformed(event)
            return true
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("TestsRunner.runTestAction", e)
            return false
        }
    }


}