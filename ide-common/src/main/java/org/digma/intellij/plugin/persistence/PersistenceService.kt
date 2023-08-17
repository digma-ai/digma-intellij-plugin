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
        @JvmStatic
        fun getInstance() : PersistenceService {
            return ApplicationManager.getApplication().getService(PersistenceService::class.java)
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
