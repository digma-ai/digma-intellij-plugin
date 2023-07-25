package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.digma.intellij.plugin.bulklistener.AbstractBulkFileChangeListener;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * listens for bulk document changes and updates span navigation.
 */
public class BulkFileChangeListenerForPythonSpanNavigation extends AbstractBulkFileChangeListener {

    private static final Logger LOGGER = Logger.getInstance(BulkFileChangeListenerForPythonSpanNavigation.class);


    @Override
    public void processEvents(@NotNull Project project, @NotNull List<? extends VFileEvent> events) {

        events.forEach(vFileEvent -> {

            var file = vFileEvent.getFile();
            if (file != null && isRelevantFile(project, file)) {
                Log.log(LOGGER::debug, "got bulk change for file  {}", vFileEvent.getFile());
                var javaLanguageService = project.getService(PythonLanguageService.class);
                if (javaLanguageService.isRelevant(file)) {
                    if (vFileEvent instanceof VFileDeleteEvent) {
                        PythonSpanNavigationProvider.getInstance(project).fileDeleted(vFileEvent.getFile());
                    } else {
                        PythonSpanNavigationProvider.getInstance(project).fileChanged(vFileEvent.getFile());
                    }
                }
            }
        });
    }

}
