package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.util.Computable
import org.digma.intellij.plugin.common.executeCatchingIgnorePCE
import org.digma.intellij.plugin.common.executeCatchingWithResultIgnorePCE
import org.digma.intellij.plugin.common.runWIthRetryIgnorePCE
import org.digma.intellij.plugin.common.runWIthRetryWithResultIgnorePCE
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext


fun executeCatchingWithRetry(
    context: NavigationProcessContext, errorHint: String, delayMillis: Int = 50,
    maxRetries: Int = 5, runnable: Runnable,
) {
    executeCatchingIgnorePCE({
        runWIthRetryIgnorePCE({
            runnable.run()
        }, delayMillis = delayMillis, maxRetries = maxRetries)
    }, { e ->
        context.addError(errorHint, e)
    })
}

fun <T> executeCatchingWithResultWithRetry(
    context: NavigationProcessContext, errorHint: String, delayMillis: Int = 50,
    maxRetries: Int = 5, computable: Computable<T>,
): T? {
    return executeCatchingWithResultIgnorePCE({
        runWIthRetryWithResultIgnorePCE({
            computable.compute()
        }, delayMillis = delayMillis, maxRetries = maxRetries)
    }, { e ->
        context.addError(errorHint, e)
        null
    })
}
