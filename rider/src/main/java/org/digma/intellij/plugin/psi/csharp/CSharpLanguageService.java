package org.digma.intellij.plugin.psi.csharp;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage;
import org.digma.intellij.plugin.psi.*;
import org.digma.rider.protocol.*;
import org.jetbrains.annotations.Nullable;

public class CSharpLanguageService implements LanguageService {

    private MethodInfoService methodInfoService;

    public CSharpLanguageService(Project project) {
        this.methodInfoService = project.getService(MethodInfoService.class);
    }

    @Override
    public boolean accept(Language language) {
        return CSharpLanguage.INSTANCE.equals(language);
    }

    @Override
    @Nullable
    public MethodIdentifier detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        //rider does it differently, through resharper
        MethodInfo methodInfo = methodInfoService.getMethodUnderCaret();
        if (methodInfo.getFqn().isEmpty()) {
            return null;
        }
        return new MethodIdentifier(methodInfo.getFqn(), methodInfo.getFilePath());
    }
}
