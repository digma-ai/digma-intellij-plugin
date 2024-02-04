package org.digma.intellij.plugin.idea.psi.navigation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.digma.intellij.plugin.bulklistener.AbstractBulkFileChangeListener;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.idea.navigation.*;
import org.digma.intellij.plugin.idea.psi.JvmLanguageService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * listens for bulk document changes and updates span navigation.
 */
public class BulkFileChangeListenerForJavaSpanNavigation extends AbstractBulkFileChangeListener {

    private static final Logger LOGGER = Logger.getInstance(BulkFileChangeListenerForJavaSpanNavigation.class);

    @Override
    public void processEvents(@NotNull Project project, @NotNull List<? extends VFileEvent> events) {

        if (project.isDisposed()) {
            return;
        }

        try {

            events.forEach(vFileEvent -> {

                var file = vFileEvent.getFile();
                if (file != null && file.isValid() && isRelevantFile(project, file)) {
                    Log.log(LOGGER::debug, "got bulk change for file  {}", vFileEvent.getFile());
                    var languageService = LanguageService.findLanguageServiceByFile(project, file);
                    //only jvm languages are supported here
                    if (JvmLanguageService.class.isAssignableFrom(languageService.getClass()) &&
                            languageService.isRelevant(file)) {

                        if (vFileEvent instanceof VFileDeleteEvent) {
                            JvmSpanNavigationProvider.getInstance(project).fileDeleted(vFileEvent.getFile());
                            JvmEndpointNavigationProvider.getInstance(project).fileDeleted(vFileEvent.getFile());
                        } else {
                            JvmSpanNavigationProvider.getInstance(project).fileChanged(vFileEvent.getFile());
                            JvmEndpointNavigationProvider.getInstance(project).fileChanged(vFileEvent.getFile());
                        }
                    }
                }
            });
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in processEvents");
            ErrorReporter.getInstance().reportError(project, "BulkFileChangeListenerForJavaSpanNavigation.processEvents", e);
        }
    }
}
