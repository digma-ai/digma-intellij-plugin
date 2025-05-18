package org.digma.intellij.plugin.document;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

import java.util.List;

public interface CodeLensChanged {

    //CodeLensChanged is called when the code lens data has changed
    //see CodeLensProviderDocumentInfoAndEnvironmentChangedListener
    //see CodeLensProviderRefresh


    @Topic.ProjectLevel
    Topic<CodeLensChanged> CODELENS_CHANGED_TOPIC = Topic.create("CODELENS_CHANGED_TOPIC", CodeLensChanged.class);

    void codelensChanged(VirtualFile virtualFile);

    //this event receives psi urls and not files because CodeLensProvider does not keep a reference to the psi file
    // but uses its url as key. when code lens changes it will return the keys. consumers of this method
    // will convert it to PsiFile. we use PsiUtils.uriToPsiFile which proves to work fine.
    void codelensChanged(List<VirtualFile> virtualFileList);

    void codelensChanged();

}
