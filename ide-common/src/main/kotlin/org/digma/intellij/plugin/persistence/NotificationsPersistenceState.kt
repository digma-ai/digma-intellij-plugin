package org.digma.intellij.plugin.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.OptionTag
import java.time.Instant

//the name of the file is a bit misleading,the Idle part is history, changing file names needs to keep latest entries.
// this file holds timestamp of last time some notification was shown to help calculate when to show it again.
@State(
    name = "org.digma.intellij.plugin.persistence.IdleNotificationsPersistenceState",
    storages = [Storage("DigmaIdleNotificationsState.xml")]
)
@Service(Service.Level.APP)
class NotificationsPersistenceState : PersistentStateComponent<IdleNotificationsState> {

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
    @OptionTag(converter = InstantConverter::class)
    var requestRegistrationLastNotified: Instant? = null,
)