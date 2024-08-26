package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.executeCatchingIgnorePCE
import org.digma.intellij.plugin.common.executeCatchingWithResultIgnorePCE
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runWIthRetryIgnorePCE
import org.digma.intellij.plugin.common.runWIthRetryWithResultIgnorePCE
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import java.util.function.Supplier


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


fun isRelevantFile(project: Project, file: VirtualFile?): Boolean {
    return isProjectValid(project) &&
            file != null &&
            !file.isDirectory &&
            //don't need to support LightVirtualFile. don't want to use the class because I'm not sure if it's available in all installed IDEs
            file.javaClass.name != "com.intellij.testFramework.LightVirtualFile"
}

//this method checks isInContent which needs read access. don't call it from EDT events like
// onChange in psi tree listeners because it will cause short freezes, only call it on background processing
fun isValidRelevantFile(project: Project, file: VirtualFile?): Boolean {
    return isRelevantFile(project, file) &&
            isValidVirtualFile(file) &&
            file != null &&
            isInContent(project, file)

}

//don't call this method on EDT events like onChange in psi tree listeners because it will
// cause short freezes, only call it on background processing
fun isInContent(project: Project, file: VirtualFile): Boolean {
    return ReadActions.ensureReadAction(Supplier {
        ProjectFileIndex.getInstance(project).isInContent(file)
    })
}