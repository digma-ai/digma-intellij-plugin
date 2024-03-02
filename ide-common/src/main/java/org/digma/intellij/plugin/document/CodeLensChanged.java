package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;

public interface CodeLensChanged {

    //CodeLensChanged is called when the code lens data has changed
    //see CodeLensProviderDocumentInfoAndEnvironmentChangedListener
    //see CodeLensRefresh


    @Topic.ProjectLevel
    Topic<CodeLensChanged> CODELENS_CHANGED_TOPIC = Topic.create("CODELENS_CHANGED_TOPIC", CodeLensChanged.class);

    void codelensChanged(PsiFile psiFile);

    void codelensChanged();

}
