package org.digma.intellij.plugin.ui;

import com.intellij.openapi.Disposable;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;

/**
 * CaretContextService is an orchestration service that handles a context change. context change is when the cursor is
 * moving from method to method.
 */
public interface CaretContextService extends Disposable {

    void contextChanged(MethodUnderCaret methodUnderCaret);

    //called when the caret is not under any element , or an unsupported file is opened
    void contextEmpty();

    void contextEmptyNonSupportedFile(String fileUri);
}
