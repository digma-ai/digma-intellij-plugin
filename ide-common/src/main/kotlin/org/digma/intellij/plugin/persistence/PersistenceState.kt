package org.digma.intellij.plugin.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

//Note: PersistenceState should not be used by plugin code, use PersistenceService.
// it allows to change the persistence structure or implementation without effecting plugin code
// the name is org.digma.intellij.plugin.persistence.PersistenceService for
// backwards compatibility
@State(
    name = "org.digma.intellij.plugin.persistence.PersistenceService",
    storages = [Storage("DigmaPersistence.xml")]
)
internal open class PersistenceState : PersistentStateComponent<PersistenceData> {

    //Note: all persistence properties have get/set methods in this class.
    // never access myPersistenceData directly from plugin code, that enables us to change myPersistenceData
    // when necessary and implement the changes internally here.
    //some properties need to change back and forth,these properties have regular getter.setter.
    //some properties need to change only once, usually marker flags, these properties have a getter and a setter that
    // doesn't accept an argument but always set to true
    private var myPersistenceData: PersistenceData = PersistenceData()

    // just an obligatory override from PersistentStateComponent
    override fun getState(): PersistenceData {
        return myPersistenceData
    }

    // after automatically loading our save state,  we will keep reference to it
    override fun loadState(state: PersistenceData) {
        myPersistenceData = state

        //todo: backwards compatibility, we want to change the name to isObservabilityEnabled.
        // remove isAutoOtel after some versions, can remove in June 2024 when probably all users updated the plugin
        myPersistenceData.isObservabilityEnabled = myPersistenceData.isAutoOtel
    }


}