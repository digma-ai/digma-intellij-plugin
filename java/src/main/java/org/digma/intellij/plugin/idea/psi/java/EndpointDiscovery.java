package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public interface EndpointDiscovery {

    //must run with read access
    //using searchScope supplier because building SearchScope needs read access
    List<EndpointInfo> lookForEndpoints(@NotNull Supplier<SearchScope> searchScopeSupplier);

    // default method uses fileScope. however, in some cases logic could be bit different
    default List<EndpointInfo> lookForEndpoints(@NotNull PsiFile psiFile) {
        return lookForEndpoints(() -> GlobalSearchScope.fileScope(psiFile));
    }

    default void endpointDiscovery(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        List<EndpointInfo> endpointInfos = lookForEndpoints(psiFile);

        for (EndpointInfo endpointInfo : endpointInfos) {
            var methodId = endpointInfo.getContainingMethodId();
            final MethodInfo methodInfo = documentInfo.getMethods().get(methodId);
            //this method must exist in the document info
            Objects.requireNonNull(methodInfo, "method info " + methodId + " must exist in DocumentInfo for " + documentInfo.getFileUri());

            methodInfo.addEndpoint(endpointInfo);
        }
    }


    default void runInReadAccess(Project project, Runnable runnable) {
        ProgressManager.getInstance().runProcess(() -> DumbService.getInstance(project).runReadActionInSmartMode(runnable), new EmptyProgressIndicator());
    }

    default <T> T runInReadAccessWithResult(Project project, Computable<T> computable) {
        return ProgressManager.getInstance().runProcess(() -> DumbService.getInstance(project).runReadActionInSmartMode(computable), new EmptyProgressIndicator());
    }

}
