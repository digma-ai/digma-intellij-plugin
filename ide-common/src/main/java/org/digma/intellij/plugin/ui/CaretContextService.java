package org.digma.intellij.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.service.EditorService;

/**
 * CaretContextService is an orchestration service that handles a context change. context change is when the cursor is
 * moving from method to method.
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
