package org.digma.intellij.plugin.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import java.util.function.Supplier


fun runInReadAccess(runnable: Runnable) {
    ReadActions.ensureReadAction(runnable)
}

fun <T> runInReadAccessWithResult(computable: Computable<T>): T {
    return ReadActions.ensureReadAction(Supplier { computable.compute() })
}


fun runInReadAccessWithRetryIgnorePCE(runnable: Runnable) {
    runWIthRetryIgnorePCE({
        ReadActions.ensureReadAction(runnable)
    }, delayMillis = 20, maxRetries = 5)
}

fun <T> runInReadAccessWithResultAndRetryIgnorePCE(computable: Computable<T>): T {
    return runWIthRetryWithResultIgnorePCE({
        ReadActions.ensureReadAction(Supplier { computable.compute() })
    }, delayMillis = 20, maxRetries = 5)
}


fun runInReadAccessInSmartMode(project: Project, runnable: Runnable, progressIndicator: ProgressIndicator) {
    if (isReadAccessAllowed()) {
        runnable.run()
    } else {
        DumbService.getInstance(project).runReadActionInSmartMode(runnable)
    }
}


fun <T> runInReadAccessInSmartModeWithResult(project: Project, computable: Computable<T>): T {
    return if (isReadAccessAllowed()) {
        computable.compute()
    } else {
        DumbService.getInstance(project).runReadActionInSmartMode(computable)
    }
}


fun runInReadAccessInSmartModeWithRetryIgnorePCE(project: Project, runnable: Runnable) {
    runWIthRetryIgnorePCE({
        if (isReadAccessAllowed()) {
            runnable.run()
        } else {
            DumbService.getInstance(project).runReadActionInSmartMode(runnable)
        }
    }, delayMillis = 50, maxRetries = 5)
}

fun <T> runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project: Project, computable: Computable<T>): T {
    return runWIthRetryWithResultIgnorePCE({
        if (isReadAccessAllowed()) {
            computable.compute()
        } else {
            DumbService.getInstance(project).runReadActionInSmartMode(computable)
        }
    }, delayMillis = 50, maxRetries = 5)
}



fun isReadAccessAllowed(): Boolean {
    return ApplicationManager.getApplication().isReadAccessAllowed
}
