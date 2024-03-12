package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;

/**
 * Event fired when a document info has changed.
 * document info changes first when a file is opened. then when the file content changes
 * document info is computed again.
 */
public interface DocumentInfoChanged {

    Topic<DocumentInfoChanged> DOCUMENT_INFO_CHANGED_TOPIC = Topic.create("DOCUMENT_INFO_CHANGE_TOPIC", DocumentInfoChanged.class);

    void documentInfoChanged(PsiFile psiFile);
}
