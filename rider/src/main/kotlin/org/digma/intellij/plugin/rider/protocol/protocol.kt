@file:JvmName("Protocol")
package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.psi.PsiUtils


//extensions to protocol classes can be written here

/*
There is an issue with file uri when running on windows,
the uri has a file:// schema,
and we use this uri in document info service as the key for a document, this is instead of using the PsiFile object
because PsiFile objects are not safe to use as keys in maps in case the platform decides to reparse a file while a
project is opened.
when calling in resharper IPsiSourceFile.GetLocation().ToUri().ToString() it will return a file schema with 2 slashes,
when calling in the java side to PsiFile.getVirtualFile().getUrl() it will return 3 slashes.
when a document is loaded we use the uri from resharper as key in DocumentInfoService, if we later try to find a
DocumentInfo by calling PsiFile.getVirtualFile().getUrl() the document info will not be found because that call will
return a file schema with 3 slashes.
Another place we use the uri is in ElementUnderCaretDetector, the MethodUnderCaretEvent.fileUri will have 2 slashes.

the solution here is to always normalize the uri that comes from the resharper side to a uri as comes in the jvm side.
so actually every object that passes from resharper to the jvm and has a uri that was taken from
IPsiSourceFile.GetLocation().ToUri().ToString() needs to be normalized.
not all file uri in all objects is used, the main one that causes issues if the MethodUnderCaretEvent.fileUri,
but we normalize all the objects.

Note that PsiUtils.uriToPsiFile(fileUri,project) will always work , with 2 or with 3 slashes.
the issue is only when we use the uri as key in our own code, opening a file with intellij utils will
work regardless of the number of slashes. so for example the uris for span navigation returned from
CodeObjectHost.findWorkspaceUrisForSpanIds or CodeObjectHost.findWorkspaceUrisForCodeObjectIds will also contain
2 slashes, but we don't use these as keys or to find DocumentInfo, they are used to open files which will always work.


 */
fun normalizeFileUri(fileUri: String,project:Project): String {

    if (fileUri.isBlank()){
        return fileUri
    }

    val psiFile = PsiUtils.uriToPsiFile(fileUri,project)
    return PsiUtils.psiFileToUri(psiFile)
}