package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.PROJECT)
class BackendConnectionUtil(private val project: Project) {
    private val logger: Logger = Logger.getInstance(BackendConnectionUtil::class.java)


    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionUtil {
            return project.getService(BackendConnectionUtil::class.java)
        }
    }

    fun testConnectionToBackend(): Boolean {

        //if called on background thread refreshNowOnBackground will run on the same thread ,
        // otherwise refreshNowOnBackground will run on background and isConnectionOk will return old result,
        // next call will be ok
        Log.log(logger::debug, "Triggering environmentsSupplier.refresh")
        AnalyticsService.getInstance(project).environment.refreshNowOnBackground()

        return BackendConnectionMonitor.getInstance(project).isConnectionOk()
    }

}