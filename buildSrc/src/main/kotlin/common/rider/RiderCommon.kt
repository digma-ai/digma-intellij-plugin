package common.rider

import org.gradle.api.Project
import org.jetbrains.intellij.IntelliJPluginConstants
import org.jetbrains.intellij.tasks.SetupDependenciesTask
import java.io.File


fun rdLibDirectory(project: Project)  =
    run {
        val setupDependenciesTaskProvider = project.tasks.named(IntelliJPluginConstants.SETUP_DEPENDENCIES_TASK_NAME)
        val setupDependenciesTask = setupDependenciesTaskProvider.get() as SetupDependenciesTask
        File(setupDependenciesTask.idea.get().classes, "lib/rd")
    }


