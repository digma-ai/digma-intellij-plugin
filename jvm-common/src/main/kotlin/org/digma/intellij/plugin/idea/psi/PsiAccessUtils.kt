package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import org.digma.intellij.plugin.common.Retries


fun runInReadAccess(project: Project, runnable: Runnable) {
    ProgressManager.getInstance().runProcess({
        DumbService.getInstance(project).runReadActionInSmartMode(runnable)
    }, EmptyProgressIndicator())
}


fun <T> runInReadAccessWithResult(project: Project, computable: Computable<T>): T {
    return ProgressManager.getInstance().runProcess<T>({
        DumbService.getInstance(project).runReadActionInSmartMode(computable)
    }, EmptyProgressIndicator())
}

fun <T> runInReadAccessInSmartModeWithResultAndRetry(project: Project, computable: Computable<T>): T {
    return Retries.retryWithResult({
        ProgressManager.getInstance().runProcess<T>({
            DumbService.getInstance(project).runReadActionInSmartMode(computable)
        }, EmptyProgressIndicator())
    }, Throwable::class.java, 50, 5)
}

