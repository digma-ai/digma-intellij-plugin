package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;

//a topic to listen when new document info is available ,like code lenses.
public interface DocumentInfoChanged {

    Topic<DocumentInfoChanged> DOCUMENT_INFO_CHANGED_TOPIC = Topic.create("DOCUMENT_INFO_CHANGE_TOPIC", DocumentInfoChanged.class);

    void documentInfoChanged(PsiFile psiFile);
}
