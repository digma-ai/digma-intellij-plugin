package org.digma.intellij.plugin.rider.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.document.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.rider.protocol.CodeLensHost;
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A listener for DocumentInfoChanged events for rider
 */
public class RiderDocumentInfoConsumer implements DocumentInfoChanged {

    private static final Logger LOGGER = Logger.getInstance(RiderDocumentInfoConsumer.class);

    private final CSharpLanguageService cSharpLanguageService;

    private final Project project;


    public RiderDocumentInfoConsumer(@NotNull Project project) {
        this.project = project;
        cSharpLanguageService = project.getService(CSharpLanguageService.class);
    }


    @Override
    public void documentInfoChanged(@NotNull VirtualFile file, @NotNull DocumentInfo documentInfo) {
        try {
            if (cSharpLanguageService.isSupportedFile(file)) {
                Log.log(LOGGER::debug, "Got documentInfoChanged for {}", file);
                Set<CodeLens> codeLens = CodeLensProvider.getInstance(project).provideCodeLens(file);
                Log.log(LOGGER::debug, "Got codeLens for {}: {}", file, codeLens);
                CodeLensHost.getInstance(project).installCodeLens(file, codeLens);
            }
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "Exception in documentInfoChanged");
            ErrorReporter.getInstance().reportError(project, "RiderDocumentInfoConsumer.documentInfoChanged", e);
        }
    }

}
