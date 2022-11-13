package org.digma.intellij.plugin.rider.env;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.document.CodeLensProvider;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.psi.PsiFileNotFountException;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;

import java.util.List;

public class RiderEnvironmentChangedHandler extends LifetimedProjectComponent {

    private final Logger LOGGER = Logger.getInstance(RiderEnvironmentChangedHandler.class);

    private final CodeObjectHost codeObjectHost;
    private final ElementUnderCaretDetector elementUnderCaretDetector;
    private final DocumentInfoService documentInfoService;
    private final CodeLensProvider codeLensProvider;

    public RiderEnvironmentChangedHandler(Project project) {
        super(project);
        codeObjectHost = project.getService(CodeObjectHost.class);
        elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        codeLensProvider = project.getService(CodeLensProvider.class);
    }

    public void environmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Got environmentChanged {}", newEnv);

        //this code is orchestration of what needs to be done on environmentChanged for rider.
        //must be run in background thread to not freeze the UI.
        //when fired from the environment object it runs on background.


        //install new code lens for all open documents
        documentInfoService.allKeys().forEach(psiFileUri -> {
            try {
                PsiFile psiFile = PsiUtils.uriToPsiFile(psiFileUri,getProject());
                Log.log(LOGGER::debug, "Requesting code lens for {}", psiFile.getVirtualFile());
                List<CodeLens> codeLens = codeLensProvider.provideCodeLens(psiFile);
                Log.log(LOGGER::debug, "Got codeLens for {}: {}", psiFile.getVirtualFile(), codeLens);
                codeObjectHost.installCodeLens(psiFile, codeLens);
            } catch (PsiFileNotFountException e) {
                Log.log(LOGGER::error, "Could not find psi file {}", psiFileUri);
            }
        });

        //trigger a refresh of element under current, after environment change it will cause a contextChange
        //and the UI will refresh with the new environment
        elementUnderCaretDetector.refresh();
    }
}

