package org.digma.intellij.plugin.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

public class ProjectOpenedListener implements ProjectManagerListener {

    private static final Logger LOGGER = Logger.getInstance(ProjectOpenedListener.class);

    @Override
    public void projectOpened(@NotNull Project project) {
        Log.log(LOGGER::info, "ProjectOpenedListener.projectOpened: project:{}", project);
    }
}
