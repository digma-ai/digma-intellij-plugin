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
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.psi.findClassPsiElementByClassName
import org.digma.intellij.plugin.psi.findClassPsiElementByMethodId
import org.digma.intellij.plugin.psi.findMethodPsiElementByMethodId

@Service(Service.Level.PROJECT)
class TestsRunner(val project: Project) {

    suspend fun executeTestMethod(methodId: String): Boolean {

        ActivityMonitor.getInstance(project).registerUserAction("executeTestMethod", mapOf("methodId" to methodId))

        return try {
            val psiLocation = suspendableRetry {
                val methodElement = findMethodPsiElementByMethodId(project, methodId)
                methodElement?.let {
                    readAction {
                        PsiLocation(it)
                    }
                }
            }

            psiLocation?.let {
                runTestAction(it)
            } ?: false

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "TestsRunner.executeTestMethod", e)
            false
        }
    }

    suspend fun executeTestClassByMethodId(methodId: String): Boolean {

        ActivityMonitor.getInstance(project).registerUserAction("executeTestClassByMethodId", mapOf("methodId" to methodId))

        return try {
            val psiLocation = suspendableRetry {
                val classElement = findClassPsiElementByMethodId(project, methodId)
                classElement?.let {
                    readAction {
                        PsiLocation(it)
                    }
                }
            }

            psiLocation?.let {
                runTestAction(it)
            } ?: false

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "TestsRunner.executeTestClassByMethodId", e)
            false
        }
    }


    suspend fun executeTestClassByClassName(className: String): Boolean {

        ActivityMonitor.getInstance(project).registerUserAction("executeTestClassByClassName", mapOf("className" to className))

        return try {
            val psiLocation = suspendableRetry {
                val classElement = findClassPsiElementByClassName(project, className)
                classElement?.let {
                    readAction {
                        PsiLocation(it)
                    }
                }
            }

            psiLocation?.let {
                runTestAction(it)
            } ?: false

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "TestsRunner.executeTestClassByClassName", e)
            false
        }
    }


    private fun runTestAction(psiLocation: PsiLocation<PsiElement>): Boolean {

        try {

            val runContextAction = RunContextAction(DefaultRunExecutor())
            val dataContext = SimpleDataContext.builder()
                .add(Location.DATA_KEY, psiLocation)
                .add(PlatformCoreDataKeys.MODULE, psiLocation.module)
                .add(CommonDataKeys.PROJECT, project)
                .build()

            val event = AnActionEvent(
                /* inputEvent = */ null,
                /* dataContext = */ dataContext,
                /* place = */ "EditorPopup",
                /* presentation = */ runContextAction.templatePresentation.clone(),
                /* actionManager = */ ActionManager.getInstance(),
                /* modifiers = */ 16,
                /* isContextMenuAction = */ true,
                /* isActionToolbar = */ false
            )

            EDT.ensureEDT {
                allowSlowOperation {
                    runContextAction.actionPerformed(event)
                }
            }

            //that doesn't mean the execution succeeded, only that there was no exception up to this line
            return true
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "TestsRunner.runTestAction", e)
            return false
        }
    }

//    fun test() {
//
//        executeTestMethod("org.springframework.samples.petclinic.owner.PetControllerTest\$_\$testInitCreationForm")
//        executeTestMethod("org.springframework.samples.petclinic.owner.PetControllerTests\$_\$testInitCreationForm")
//
//        Thread.sleep(30000)
//
//        executeTestClassByMethodId("org.springframework.samples.petclinic.owner.PetControllerTest\$_\$testInitCreationForm")
//        executeTestClassByMethodId("org.springframework.samples.petclinic.owner.PetControllerTests\$_\$testInitCreationForm")
//
//        Thread.sleep(30000)
//
//        executeTestClassByClassName("org.springframework.samples.petclinic.owner.PetControllerTest")
//        executeTestClassByClassName("org.springframework.samples.petclinic.owner.PetControllerTests")
//
//    }


}