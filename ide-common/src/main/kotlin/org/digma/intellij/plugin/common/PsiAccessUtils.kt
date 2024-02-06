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

//fun runInReadAccessWithRetry(runnable: Runnable) {
//    runWIthRetry({
//        ReadActions.ensureReadAction(runnable)
//    }, backOffMillis = 20, maxRetries = 5)
//}

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

//fun <T> runInReadAccessWithResultAndRetry(computable: Computable<T>): T {
//    return runWIthRetryWithResult({
//        ReadActions.ensureReadAction(Supplier { computable.compute() })
//    }, backOffMillis = 20, maxRetries = 5)
//}


//fun runInReadAccessInSmartMode(project: Project, runnable: Runnable) {
//    if (isReadAccessAllowed()) {
//        runnable.run()
//    } else {
//        ProgressManager.getInstance().runProcess({
//            DumbService.getInstance(project).runReadActionInSmartMode(runnable)
//        }, EmptyProgressIndicator())
//    }
//}

//fun runInReadAccessInSmartModeAndRetry(project: Project, runnable: Runnable) {
//    runWIthRetry({
//        if (isReadAccessAllowed()) {
//            runnable.run()
//        } else {
//            ProgressManager.getInstance().runProcess({
//                DumbService.getInstance(project).runReadActionInSmartMode(runnable)
//            }, EmptyProgressIndicator())
//        }
//    }, backOffMillis = 50, maxRetries = 5)
//}


fun runInReadAccessInSmartMode(project: Project, runnable: Runnable, progressIndicator: ProgressIndicator) {
    if (isReadAccessAllowed()) {
        runnable.run()
    } else {
        DumbService.getInstance(project).runReadActionInSmartMode(runnable)
    }
}

//fun runInReadAccessInSmartModeAndRetry(project: Project, runnable: Runnable, progressIndicator: ProgressIndicator) {
//    runWIthRetryWithResult({
//        if (isReadAccessAllowed()) {
//            runnable.run()
//        } else {
//            ProgressManager.getInstance().runProcess({
//                DumbService.getInstance(project).runReadActionInSmartMode(runnable)
//            }, progressIndicator)
//        }
//    }, backOffMillis = 50, maxRetries = 5)
//}


fun <T> runInReadAccessInSmartModeWithResult(project: Project, computable: Computable<T>): T {
    return if (isReadAccessAllowed()) {
        computable.compute()
    } else {
        DumbService.getInstance(project).runReadActionInSmartMode(computable)
    }
}


//fun <T> runInReadAccessInSmartModeWithResultAndRetry(project: Project, computable: Computable<T>): T {
//    return runWIthRetryWithResult({
//        if (isReadAccessAllowed()) {
//            computable.compute()
//        } else {
//            ProgressManager.getInstance().runProcess<T>({
//                DumbService.getInstance(project).runReadActionInSmartMode(computable)
//            }, EmptyProgressIndicator())
//
//        }
//    }, backOffMillis = 50, maxRetries = 5)
//}


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

//fun <T> runInReadAccessInSmartModeWithRetryIgnorePCE(project: Project, computable: Computable<T>): T {
//    return runWIthRetryWithResultIgnorePCE({
//        if (isReadAccessAllowed()) {
//            computable.compute()
//        } else {
//            DumbService.getInstance(project).runReadActionInSmartMode(computable)
//        }
//    }, delayMillis = 50, maxRetries = 5)
//}


//fun <T> runInReadAccessInSmartModeWithWriteActionPriorityWithRetry(project: Project, computable: Computable<T>): T {
//    return runWIthRetryWithResult({
//
//        //don't wait for smart mode if in read action
//        if (!isReadAccessAllowed()) {
//            DumbService.getInstance(project).waitForSmartMode()
//        }
//        //todo: can retry until success a few times
//        val result = Ref<T>()
//        val success = ProgressManager.getInstance().runInReadActionWithWriteActionPriority({
//            result.set(computable.compute())
//        }, EmptyProgressIndicator())
//        result.get()
//
//    }, backOffMillis = 50, maxRetries = 5)
//}


fun isReadAccessAllowed(): Boolean {
    return ApplicationManager.getApplication().isReadAccessAllowed
}


