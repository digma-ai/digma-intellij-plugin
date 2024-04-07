package org.digma.intellij.plugin.ui.updates

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityToolWindowCardsController
import org.digma.intellij.plugin.updates.AggressiveUpdateService

class AggressiveUpdateProjectStartup : StartupActivity {

    override fun runActivity(project: Project) {

        //make sure it started
        AggressiveUpdateService.getInstance()

        //update the state for any project that is starting.
        //maybe those two services are not fully initialized yet. but they both remember the last card called and will show it
        // when they are fully initialized. or they will catch the stateChanged event from AggressiveUpdateService.
        // in any case they will show the correct update state.
        //maybe AggressiveUpdateService will not be fully updated when those services are called and the state is not yet correct,
        // but they will start listening for stateChanged event from AggressiveUpdateService and will show the state when the event is fired.
        MainToolWindowCardsController.getInstance(project).updateStateChanged(AggressiveUpdateService.getInstance().getUpdateState())
        RecentActivityToolWindowCardsController.getInstance(project).updateStateChanged(AggressiveUpdateService.getInstance().getUpdateState())
    }
}