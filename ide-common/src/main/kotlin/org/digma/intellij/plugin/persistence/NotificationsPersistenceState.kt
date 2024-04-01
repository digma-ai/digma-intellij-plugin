package org.digma.intellij.plugin.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.OptionTag
import java.time.Instant

//the name of the file is a bit misleading,the Idle part is history, changing file names is a hassle.
// this file holds timestamp of last time some notification was shown to help calculate when to show it again.
@State(
    name = "org.digma.intellij.plugin.persistence.IdleNotificationsPersistenceState",
    storages = [Storage("DigmaIdleNotificationsState.xml")]
)
@Service(Service.Level.APP)
class NotificationsPersistenceState : PersistentStateComponent<NotificationsState> {

    private var myPersistenceState: NotificationsState = NotificationsState()

    override fun getState(): NotificationsState {
        return myPersistenceState
    }

    override fun loadState(state: NotificationsState) {
        myPersistenceState = state
    }

}


data class NotificationsState(
    @OptionTag(converter = InstantConverter::class)
    var backendHasntBeenRunningForAWhileLastNotified: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var hasntBeenOpenedForAWhileLastNotified: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var hasntBeenActivatedLastNotified: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var requestRegistrationLastNotified: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var aggressiveUpdateLastNotified: Instant? = null,
)