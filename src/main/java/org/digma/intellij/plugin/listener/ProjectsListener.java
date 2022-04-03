package org.digma.intellij.plugin.listener;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.EditorInteractionService;
import org.jetbrains.annotations.NotNull;

public class ProjectsListener implements ProjectManagerListener {

    private static final Logger LOGGER = Logger.getInstance(ProjectsListener.class);

    @Override
    public void projectOpened(@NotNull Project project) {
        Log.log(LOGGER::debug, "projectOpened {}", project);
        EditorInteractionService.getInstance(project).start(project);
    }
}
