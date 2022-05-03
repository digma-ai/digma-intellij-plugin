package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;

public interface DocumentInfoChanged {

    Topic<DocumentInfoChanged> DOCUMENT_INFO_CHANGE_TOPIC = Topic.create("custom name", DocumentInfoChanged.class);

    void documentInfoChanged(PsiFile psiFile);
}
