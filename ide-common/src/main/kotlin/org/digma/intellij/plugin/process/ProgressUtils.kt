package org.digma.intellij.plugin.process

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager


fun assertUnderProgress() {
    if (ProgressManager.getGlobalProgressIndicator() == null) {
        //we usually don't write error, but this error must be caught during development
        // to warn us of wrong use of retryable task. it will pop up a red balloon.
        Logger.getInstance("ProgressUtilsKt").error("must run under progress")
        throw RuntimeException("must run under progress")
    }
}

fun assertNotUnderProgress() {
    if (ProgressManager.getGlobalProgressIndicator() != null) {
        //we usually don't write error, but this error must be caught during development
        // to warn us of wrong use of retryable task. it will pop up a red balloon.
        Logger.getInstance("ProgressUtilsKt").error("must NOT be called under progress")
        throw RuntimeException("must NOT be called under progress")
    }
}