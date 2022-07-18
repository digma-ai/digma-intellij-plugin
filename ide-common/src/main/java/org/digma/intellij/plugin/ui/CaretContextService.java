package org.digma.intellij.plugin.ui;

import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;

public interface CaretContextService {

    void start();

    void contextChanged(MethodUnderCaret methodUnderCaret);

    //called when the caret is not under any element , or an unsupported file is opened
    void contextEmpty();

    void contextEmptyNonSupportedFile(String fileUri);
}
