package org.digma.intellij.plugin.persistence

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log


// the @State annotation helps IntelliJ automatically serialize and save our state
@State(
        name = "org.digma.intellij.plugin.persistence.PersistenceService",
        storages = [Storage("DigmaPersistence.xml")]
)
open class PersistenceService : PersistentStateComponent<PersistenceData> {

    // this is how we're going to call the component from different classes
    companion object {
        private val logger = Logger.getInstance(PersistenceService::class.java)
        @JvmStatic
        fun getInstance() : PersistenceService {
            Log.test(logger, "Getting instance of ${PersistenceService::class.simpleName}")
            val service = ApplicationManager.getApplication().getService(PersistenceService::class.java)
            Log.test(logger, "Returning ${PersistenceService::class.simpleName}")
            return service
        }
    }


    // the component will always keep our state as a variable
    private var myPersistenceData: PersistenceData = PersistenceData()

    // just an obligatory override from PersistentStateComponent
    override fun getState(): PersistenceData {
        return myPersistenceData
    }

    // after automatically loading our save state,  we will keep reference to it
    override fun loadState(state: PersistenceData) {
        myPersistenceData = state
    }

    fun firstWizardLaunchDone() {
        state.firstWizardLaunch = false
    }

    fun isFirstWizardLaunch(): Boolean {
        return state.firstWizardLaunch
    }

    fun isLocalEngineInstalled(): Boolean? {
        return state.isLocalEngineInstalled
    }

    fun setLocalEngineInstalled(isInstalled: Boolean) {
        state.isLocalEngineInstalled = isInstalled
    }

    fun setSelectedServices(projectName: String, services: Array<String>?) {
        if (services == null) {
            if (state.selectedServices.containsKey(projectName))
            {
                state.selectedServices.remove(projectName)
            }
        } else {
            state.selectedServices[projectName] = services
        }
    }

    fun isFirstTimePluginLoaded(): Boolean {
        return state.isFirstTimePluginLoaded
    }

    fun setFirstTimePluginLoaded() {
        state.isFirstTimePluginLoaded = true
    }

    fun setNoInsightsYetNotificationPassed() {
        state.noInsightsYetNotificationPassed = true
    }


}
