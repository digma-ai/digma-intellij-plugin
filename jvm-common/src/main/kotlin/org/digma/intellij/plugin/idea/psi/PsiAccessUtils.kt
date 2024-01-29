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
import org.digma.intellij.plugin.common.runWIthRetry
import org.digma.intellij.plugin.common.runWIthRetryWithResult
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier


fun runInReadAccess(runnable: Runnable) {
    ReadActions.ensureReadAction(runnable)
}

fun runInReadAccessWithRetry(runnable: Runnable) {
    return runWIthRetryWithResult({
        ReadActions.ensureReadAction(runnable)
    }, backOffMillis = 20, maxRetries = 5)
}

fun <T> runInReadAccessWithResult(computable: Computable<T>): T {
    return ReadActions.ensureReadAction(Supplier { computable.compute() })
}

fun <T> runInReadAccessWithResultAndRetry(computable: Computable<T>): T {
    return runWIthRetryWithResult({
        ReadActions.ensureReadAction(Supplier { computable.compute() })
    }, backOffMillis = 20, maxRetries = 5)
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
    runWIthRetry({
        if (isReadAccessAllowed()) {
            runnable.run()
        } else {
            ProgressManager.getInstance().runProcess({
                DumbService.getInstance(project).runReadActionInSmartMode(runnable)
            }, EmptyProgressIndicator())
        }
    }, backOffMillis = 50, maxRetries = 5)
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
    runWIthRetryWithResult({
        if (isReadAccessAllowed()) {
            runnable.run()
        } else {
            ProgressManager.getInstance().runProcess({
                DumbService.getInstance(project).runReadActionInSmartMode(runnable)
            }, progressIndicator)
        }
    }, backOffMillis = 50, maxRetries = 5)
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
    return runWIthRetryWithResult({
        if (isReadAccessAllowed()) {
            computable.compute()
        } else {
            ProgressManager.getInstance().runProcess<T>({
                DumbService.getInstance(project).runReadActionInSmartMode(computable)
            }, EmptyProgressIndicator())

        }
    }, backOffMillis = 50, maxRetries = 5)
}


fun <T> runInReadAccessInSmartModeWithWriteActionPriorityWithRetry(project: Project, computable: Computable<T>): T {
    return runWIthRetryWithResult({

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

    }, backOffMillis = 50, maxRetries = 5)
}


fun isReadAccessAllowed(): Boolean {
    return ApplicationManager.getApplication().isReadAccessAllowed
}


fun executeCatching(runnable: Runnable, onException: Consumer<Throwable>) {
    return try {
        runnable.run()
    } catch (e: Throwable) {
        onException.accept(e)
    }
}

fun <T> executeCatching(computable: Computable<T>, onException: Function<Throwable, T>): T {
    return try {
        computable.compute()
    } catch (e: Throwable) {
        onException.apply(e)
    }
}