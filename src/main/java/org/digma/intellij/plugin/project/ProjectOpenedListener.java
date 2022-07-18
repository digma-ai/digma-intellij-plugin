package org.digma.intellij.plugin.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

public class ProjectOpenedListener implements ProjectManagerListener {


    private static final Logger LOGGER = Logger.getInstance(ProjectOpenedListener.class);

    private Project myFirst = null;

    @Override
    public void projectOpened(@NotNull Project project) {
        if (myFirst == null) myFirst = project;
        //just logging , there is nothing to do in projectOpened.
        //projectOpened will be fired on each project for every new opened project.
        //so the project argument may be any project that the user opens.
        //projectOpened is not a good place to initialize per-project resources.
        Log.log(LOGGER::info, "ProjectOpenedListener.projectOpened: project:{}, myFirst:{}", project.getName(),myFirst.getName());
    }
}
