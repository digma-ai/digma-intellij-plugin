package org.digma.intellij.plugin.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;


public class PsiUtils {

    private PsiUtils() {
    }



    /*
    Note: our DocumentInfo object has a fileUri which is an uri including the 'file:' schema and is used to convert PsiFile
    to and from string. it is mainly used to store the file uri that the document belongs to and to find the file when
    necessary.
     */

    @NotNull
    public static String psiFileToUri(@NotNull PsiFile psiFile) {

        //usually this method should only be called for source files in the project and then
        //psiFile.getVirtualFile() will never return null.
        //this is a fallback to protect against NPE in case we have some bug somewhere, psiFile.getName
        //will never help us, but we will probably discover the bug in some other way because things will not work
        //correctly with psiFile.getName.
        if (psiFile.getVirtualFile() == null){
            return psiFile.getName();
        }

        return psiFile.getVirtualFile().getUrl();
    }

    /*
    This method should either succeed or throw exception,never return null
     */
    @NotNull
    public static PsiFile uriToPsiFile(@NotNull String uri, @NotNull Project project) throws PsiFileNotFountException {
        //todo: check if ReadAction is necessary
        return ReadAction.compute(() -> {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(uri);
            if (virtualFile == null) {
                throw new PsiFileNotFountException("could not locate VirtualFile for uri "+uri);
            }
            var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile == null){
                throw new PsiFileNotFountException("could not locate PsiFile for uri "+uri+", virtual file:"+virtualFile);
            }
            return psiFile;
        });
    }

}
