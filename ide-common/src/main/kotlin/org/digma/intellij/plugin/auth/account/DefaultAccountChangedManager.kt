package org.digma.intellij.plugin.auth.account

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.util.Alarm
import org.digma.intellij.plugin.log.Log

/**
 * a project service that is called when the default account is changed
 * and orchestras the actions to perform.
 */
@Service(Service.Level.APP)
class DefaultAccountChangedManager : Disposable {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val myAccountChangedAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    //a property that can be listed to
    //todo: not sure we need this feature
    var changingDefaultAccount = AtomicBooleanProperty(false)

    fun defaultAccountChanged() {
        Log.log(logger::info, "got defaultAccountChanged")
        myAccountChangedAlarm.cancelAllRequests()
        myAccountChangedAlarm.addRequest({

            Log.log(logger::info, "firing defaultAccountChanged event")
            ApplicationManager.getApplication().messageBus.syncPublisher(DefaultAccountChanged.DEFAULT_ACCOUNT_CHANGED_TOPIC).defaultAccountChanged()

            try {
                changingDefaultAccount.set(true)

                //todo: maybe need to do something

            } finally {
                changingDefaultAccount.set(false)
            }
        }, 200)
    }

    override fun dispose() {
        Log.log(logger::info, "disposing")
        myAccountChangedAlarm.dispose()
    }
}