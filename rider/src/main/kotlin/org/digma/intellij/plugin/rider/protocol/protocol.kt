@file:JvmName("Protocol")
package org.digma.intellij.plugin.rider.protocol


/*
the backend send a file with file system protocol: file:///some/path/class.cs
we need to keep is when ever converting to and from psi file
 */

//@Nullable
//fun pathToPsiFile(file: String, project: Project): PsiFile? {
//    var virtualFile: VirtualFile? = VirtualFileManager.getInstance().findFileByUrl(file)
//    if (virtualFile != null) {
//        return PsiManager.getInstance(project).findFile(virtualFile);
//    }
//    return null;
//}

