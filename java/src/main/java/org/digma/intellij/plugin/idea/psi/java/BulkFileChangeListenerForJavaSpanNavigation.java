package org.digma.intellij.plugin.idea.psi.java;

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
public class BulkFileChangeListenerForJavaSpanNavigation implements BulkFileListener {

    private static final Logger LOGGER = Logger.getInstance(BulkFileChangeListenerForJavaSpanNavigation.class);

    private final Project project;


    public BulkFileChangeListenerForJavaSpanNavigation(Project project) {
        this.project = project;
    }

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        events.forEach(vFileEvent -> {
            Log.log(LOGGER::debug, "got after bulk change for file  {}", vFileEvent.getFile());
            if (JavaLanguageUtils.isRelevantFile(project,vFileEvent.getFile())) {
                JavaSpanNavigationProvider javaSpanNavigationProvider = project.getService(JavaSpanNavigationProvider.class);
                javaSpanNavigationProvider.fileChanged(vFileEvent.getFile());
            }
        });
    }


}
