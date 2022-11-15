@file:JvmName("Protocol")
package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.psi.PsiUtils


//extensions to protocol classes can be written here

/*
There is an issue with file uri when running on windows,
the uri has a file:// schema,
when calling in resharper IPsiSourceFile.GetLocation().ToUri().ToString() it will return a file schema with different
number of slashes then when calling in the java side to PsiFile.getVirtualFile().getUrl().
it causes issues when using the uri in various maps.
the solution here is to always normalize the uri that comes from the resharper side to a uri as comes in the jvm side.
so actually every object that passes from resharper to the jvm and has a uri that was taken from
IPsiSourceFile.GetLocation().ToUri().ToString() needs to be normalized.
PsiUtils.uriToPsiFile(fileUri,project) will always work , with 2 or with 3 slashes.
not all file uri in all objects is used, the main one that causes issues if the MethodUnderCaretEvent.fileUri,
but we normalize all the objects.
 */
fun normalizeFileUri(fileUri: String,project:Project): String {

    if (fileUri.isBlank()){
        return fileUri
    }

    val psiFile = PsiUtils.uriToPsiFile(fileUri,project)
    return PsiUtils.psiFileToUri(psiFile)
}