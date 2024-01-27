package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Ref
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.Retries
import java.util.function.Supplier


fun <T> runInReadAccessWithResult(computable: Computable<T>): T {
    return ReadActions.ensureReadAction(Supplier { computable.compute() })
}

fun <T> runInReadAccessWithResultAndRetry(computable: Computable<T>): T {
    return Retries.retryWithResult({
        ReadActions.ensureReadAction(Supplier { computable.compute() })
    }, Throwable::class.java, 20, 5)
}


fun runInReadAccessInSmartMode(project: Project, runnable: Runnable) {
    if (isReadAccessAllowed()) {
        runnable.run()
    } else {
        ProgressManager.getInstance().runProcess({
            DumbService.getInstance(project).runReadActionInSmartMode(runnable)
        }, EmptyProgressIndicator())
    }
}

fun runInReadAccessInSmartModeAndRetry(project: Project, runnable: Runnable) {
    Retries.simpleRetry({
        if (isReadAccessAllowed()) {
            runnable.run()
        } else {
            ProgressManager.getInstance().runProcess({
                DumbService.getInstance(project).runReadActionInSmartMode(runnable)
            }, EmptyProgressIndicator())
        }
    }, Throwable::class.java, 50, 5)
}


fun runInReadAccessInSmartMode(project: Project, runnable: Runnable, progressIndicator: ProgressIndicator) {
    if (isReadAccessAllowed()) {
        runnable.run()
    } else {
        ProgressManager.getInstance().runProcess({
            DumbService.getInstance(project).runReadActionInSmartMode(runnable)
        }, progressIndicator)
    }
}

fun runInReadAccessInSmartModeAndRetry(project: Project, runnable: Runnable, progressIndicator: ProgressIndicator) {
    Retries.simpleRetry({
        if (isReadAccessAllowed()) {
            runnable.run()
        } else {
            ProgressManager.getInstance().runProcess({
                DumbService.getInstance(project).runReadActionInSmartMode(runnable)
            }, progressIndicator)
        }
    }, Throwable::class.java, 50, 5)
}


fun <T> runInReadAccessInSmartModeWithResult(project: Project, computable: Computable<T>): T {
    return if (isReadAccessAllowed()) {
        computable.compute()
    } else {
        ProgressManager.getInstance().runProcess<T>({
            DumbService.getInstance(project).runReadActionInSmartMode(computable)
        }, EmptyProgressIndicator())
    }
}


fun <T> runInReadAccessInSmartModeWithResultAndRetry(project: Project, computable: Computable<T>): T {
    return Retries.retryWithResult({
        if (isReadAccessAllowed()) {
            computable.compute()
        } else {
            ProgressManager.getInstance().runProcess<T>({
                DumbService.getInstance(project).runReadActionInSmartMode(computable)
            }, EmptyProgressIndicator())

        }
    }, Throwable::class.java, 50, 5)
}


fun <T> runInReadAccessInSmartModeWithWriteActionPriorityWithRetry(project: Project, computable: Computable<T>): T {
    return Retries.retryWithResult({

        //don't wait for smart mode if in read action
        if (!isReadAccessAllowed()) {
            DumbService.getInstance(project).waitForSmartMode()
        }
        //todo: can retry until success a few times
        val result = Ref<T>()
        val success = ProgressManager.getInstance().runInReadActionWithWriteActionPriority({
            result.set(computable.compute())
        }, EmptyProgressIndicator())
        result.get()

    }, Throwable::class.java, 50, 5)
}


fun isReadAccessAllowed(): Boolean {
    return ApplicationManager.getApplication().isReadAccessAllowed
}

