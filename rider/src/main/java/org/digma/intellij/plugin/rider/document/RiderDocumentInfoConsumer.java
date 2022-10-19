package org.digma.intellij.plugin.rider.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.CodeLensProvider;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A listener for DocumentInfoChanged events for rider
 */
public class RiderDocumentInfoConsumer implements DocumentInfoChanged {

    private static final Logger LOGGER = Logger.getInstance(RiderDocumentInfoConsumer.class);

    private final CodeObjectHost codeObjectHost;
    private final CodeLensProvider codeLensProvider;

    private final CSharpLanguageService cSharpLanguageService;

    private final Project project;


    public RiderDocumentInfoConsumer(@NotNull Project project) {
        this.project = project;
        cSharpLanguageService = project.getService(CSharpLanguageService.class);
        codeObjectHost = project.getService(CodeObjectHost.class);
        codeLensProvider = project.getService(CodeLensProvider.class);
    }


    @Override
    public void documentInfoChanged(@NotNull PsiFile psiFile) {
        if (cSharpLanguageService.isSupportedFile(project, psiFile)) {
            Log.log(LOGGER::debug, "Got documentInfoChanged for {}", psiFile.getVirtualFile());
            List<CodeLens> codeLens = codeLensProvider.provideCodeLens(psiFile);
            Log.log(LOGGER::debug, "Got codeLens for {}: {}", psiFile.getVirtualFile(), codeLens);
            codeObjectHost.installCodeLens(psiFile, codeLens);
        }
    }

}
