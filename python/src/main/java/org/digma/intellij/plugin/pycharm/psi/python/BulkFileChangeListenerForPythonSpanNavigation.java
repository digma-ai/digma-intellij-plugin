package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * listens for bulk document changes and updates span navigation.
 */
public class BulkFileChangeListenerForPythonSpanNavigation implements BulkFileListener {

    private static final Logger LOGGER = Logger.getInstance(BulkFileChangeListenerForPythonSpanNavigation.class);

    private final Project project;

    private final PythonLanguageService pythonLanguageService;


    public BulkFileChangeListenerForPythonSpanNavigation(Project project) {
        this.project = project;
        pythonLanguageService = project.getService(PythonLanguageService.class);
    }


    //todo: implement
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        events.forEach(vFileEvent -> {
            Log.log(LOGGER::debug, "got after bulk change for file  {}", vFileEvent.getFile());
            if (vFileEvent.getFile() != null && pythonLanguageService.isRelevant(vFileEvent.getFile())) {
                PythonSpanNavigationProvider pythonSpanNavigationProvider = project.getService(PythonSpanNavigationProvider.class);
                pythonSpanNavigationProvider.fileChanged(vFileEvent.getFile());
            }
        });
    }


}
