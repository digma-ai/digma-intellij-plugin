package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.jetbrains.annotations.NotNull;

public interface IEndpointDiscovery {
    void endpointDiscovery(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo);
}
