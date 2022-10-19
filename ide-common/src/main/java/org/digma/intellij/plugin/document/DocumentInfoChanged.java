package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;

/**
 * Event fired when a document info has changed after a document is analyzed and its backend info is retrieved from
 * the analytics service, or after environment change.
 */
public interface DocumentInfoChanged {

    Topic<DocumentInfoChanged> DOCUMENT_INFO_CHANGED_TOPIC = Topic.create("DOCUMENT_INFO_CHANGE_TOPIC", DocumentInfoChanged.class);

    void documentInfoChanged(PsiFile psiFile);
}
