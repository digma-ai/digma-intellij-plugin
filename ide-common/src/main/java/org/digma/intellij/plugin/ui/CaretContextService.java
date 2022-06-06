package org.digma.intellij.plugin.ui;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;

public interface CaretContextService {

    void start(Project project);

    //called when the caret is under a method
    void contextChanged(MethodUnderCaret methodUnderCaret);

    //called when the caret is not under any element , or an unsupported file is opened
    void contextEmpty();
}
