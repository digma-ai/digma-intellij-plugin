package org.digma.intellij.plugin.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.OptionTag
import java.time.Instant


@State(
    name = "org.digma.intellij.plugin.persistence.IdleNotificationsPersistenceState",
    storages = [Storage("DigmaIdleNotificationsState.xml")]
)
@Service(Service.Level.APP)
class IdleNotificationsPersistenceState : PersistentStateComponent<IdleNotificationsState> {

    private var myPersistenceState: IdleNotificationsState = IdleNotificationsState()

    override fun getState(): IdleNotificationsState {
        return myPersistenceState
    }

    override fun loadState(state: IdleNotificationsState) {
        myPersistenceState = state
    }

}


data class IdleNotificationsState(
    @OptionTag(converter = InstantConverter::class)
    var backendHasntBeenRunningForAWhileLastNotified: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var hasntBeenOpenedForAWhileLastNotified: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var hasntBeenActivatedLastNotified: Instant? = null,
)