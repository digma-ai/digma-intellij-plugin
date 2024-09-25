package org.digma.intellij.plugin.protocol

import com.intellij.ide.RecentProjectListActionProvider
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.ReopenProjectAction
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.navigation.PROJECT_NAME_KEY
import com.intellij.navigation.ProtocolOpenProjectResult
import com.intellij.navigation.openProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.JBProtocolCommand
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.nio.file.Path

const val DIGMA_COMMAND = "digma"
const val DIGMA_PLUGIN_TARGET = "plugin"


//jetbrains://idea/digma/plugin?action=assets
//jetbrains://idea/digma/plugin?project=spring-petclinic&action=assets

class DigmaProtocolCommand : JBProtocolCommand(DIGMA_COMMAND) {
    private val logger: Logger = Logger.getInstance(this::class.java)

    //    override fun perform(target: String?, parameters: Map<String, String>, fragment: String?): Future<String?> {
    override suspend fun execute(target: String?, parameters: Map<String, String>, fragment: String?): String? {
        try {
            return executeImpl(target,parameters,fragment)
        }catch (e:Throwable){
            ErrorReporter.getInstance().reportError("DigmaProtocolCommand.execute",e)
            return "Error $e"
        }
    }

    //returns null on success, message on failure
    private suspend fun executeImpl(target: String?, parameters: Map<String, String>, fragment: String?): String? {

        if (target != DIGMA_PLUGIN_TARGET) {
//            return CompletableFuture.completedFuture("DigmaProtocolCommand Supports Only Plugin Target")
            return "DigmaProtocolCommand Supports Only Plugin Target"
        }

        Log.log(
            logger::trace,
            "execute called with target={},fragment={},parameters={}, thread={}",
            target,
            fragment.toString(),
            parameters.toUrlQueryString(),
            Thread.currentThread().name
        )


        var project = if (parameters.containsKey(PROJECT_NAME_KEY)) {
            when (val openProjectResult = openProject(parameters)) {
                is ProtocolOpenProjectResult.Success -> openProjectResult.project
//                is ProtocolOpenProjectResult.Error -> return CompletableFuture.completedFuture(openProjectResult.message)
                is ProtocolOpenProjectResult.Error -> return openProjectResult.message
            }
        } else {
            findActiveProject()
        }

        if (project == null) {
            val recentProjectPath =
                RecentProjectListActionProvider.getInstance().getActions().asSequence().filterIsInstance(ReopenProjectAction::class.java)
                    .firstOrNull()?.projectPath ?: RecentProjectsManager.getInstance().lastProjectCreationLocation
            if (recentProjectPath != null) {
                project = RecentProjectsManagerBase.getInstanceEx().openProject(Path.of(recentProjectPath), OpenProjectTask())
            }
        }


        if (project != null) {

            Log.log(logger::trace, "got project {}", project.name)

            val action = getActionFromParameters(parameters)
//                ?: return CompletableFuture.completedFuture("DigmaProtocolCommand no action in request")
                ?: return "DigmaProtocolCommand no action in request"


            var waitForJcef = false
            if (!ToolWindowShower.getInstance(project).isToolWindowVisible) {
                waitForJcef = true
                ApplicationManager.getApplication().invokeAndWait {
                    Log.log(logger::trace, "showing tool window")
                    ToolWindowShower.getInstance(project).showToolWindow()
                    Log.log(logger::trace, "tool window shown")
                }
            }

            Log.log(logger::trace, "executing action {}", action)
            val result = project.service<DigmaProtocolApi>().performAction(project, parameters, waitForJcef)
            Log.log(logger::trace, "after execute action {}", action)
//            return CompletableFuture.completedFuture(null)
            return result
        }

//        return CompletableFuture.completedFuture("DigmaProtocolCommand can not find project")
        return "DigmaProtocolCommand can not open any project"

    }

//    override suspend fun execute(target: String?, parameters: Map<String, String>, fragment: String?): String? {
//        return super.execute(target, parameters, fragment)
//    }
}