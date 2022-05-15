package org.digma.intellij.plugin.ui;

import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;

public interface CaretContextService {

    //called when the caret is under a method
    void contextChanged(MethodUnderCaret elementUnderCaret);

    //called when the caret is not under any element , or an unsupported file is opened
    void contextEmpty();
}
