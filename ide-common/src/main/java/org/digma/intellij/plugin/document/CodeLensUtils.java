package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;

public class CodeLensUtils {

    @NotNull
    public static String psiFileToKey(@NotNull PsiFile psiFile) {
        return PsiUtils.psiFileToUri(psiFile);
    }

}
