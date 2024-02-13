package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;

/**
 * CaretContextService is an orchestration service that handles a context change. context change is when the cursor is
 * moving from method to method.
 * there must be only one service implementing this interface.
 */
public interface CaretContextService extends Disposable {

    static CaretContextService getInstance(Project project) {
        return project.getService(CaretContextService.class);
    }

    void contextChanged(MethodUnderCaret methodUnderCaret);

    //called when the caret is not under any element , or an unsupported file is opened
    void contextEmpty();

    void contextEmptyNonSupportedFile(String fileUri);
}
