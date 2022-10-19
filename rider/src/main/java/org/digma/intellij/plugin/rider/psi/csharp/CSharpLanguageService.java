package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage;
import kotlin.Pair;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.rider.env.RiderEnvironmentChangedHandler;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.rider.protocol.MethodNavigationHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CSharpLanguageService extends LifetimedProjectComponent implements LanguageService {

    private final Logger LOGGER = Logger.getInstance(CSharpLanguageService.class);

    private final MethodNavigationHost methodNavigationHost;

    private final CodeObjectHost codeObjectHost;

    private final RiderEnvironmentChangedHandler riderEnvironmentChangedHandler;

    public CSharpLanguageService(Project project) {
        super(project);
        methodNavigationHost = project.getService(MethodNavigationHost.class);
        codeObjectHost = project.getService(CodeObjectHost.class);
        riderEnvironmentChangedHandler = project.getService(RiderEnvironmentChangedHandler.class);
    }

    //should not be called !!
    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        if (psiFile == null){
            return false;
        }
        return CSharpLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return CSharpLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public @Nullable MethodUnderCaret detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        throw new UnsupportedOperationException("should not be called");
    }

    @Override
    public void navigateToMethod(String codeObjectId) {
        Log.log(LOGGER::debug, "Navigating to method {}", codeObjectId);
        methodNavigationHost.navigateToMethod(codeObjectId);
    }

    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return language.getClass().equals(CSharpLanguage.class);
    }


    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {
        return codeObjectHost.findWorkspaceUrisForCodeObjectIds(codeObjectIds);
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return codeObjectHost.findWorkspaceUrisForSpanIds(spanIds);
    }

    @Override
    public void environmentChanged(String newEnv) {
        riderEnvironmentChangedHandler.environmentChanged(newEnv);
    }
}
