package org.digma.intellij.plugin.persistence

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.posthog.ActivityMonitor


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
            logger.warn("Getting instance of ${PersistenceService::class.simpleName}")
            val service = ApplicationManager.getApplication().getService(PersistenceService::class.java)
            logger.warn("Returning ${PersistenceService::class.simpleName}")
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
}
