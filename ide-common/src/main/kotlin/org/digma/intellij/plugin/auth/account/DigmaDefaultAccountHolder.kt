package org.digma.intellij.plugin.auth.account

import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.DefaultAccountHolder
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.util.Urls
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.log.Log

/**
 * hold the default account.
 * jetbrains have an implementation, but it has behaviour that doesn't fit with our design.
 * com.intellij.collaboration.auth.PersistentDefaultAccountHolder
 */
@State(
    name = "org.digma.intellij.plugin.DigmaDefaultAccount",
    storages = [Storage(value = "digma-default-account.xml")],
    reportStatistic = false, category = SettingsCategory.TOOLS
)
@Service(Service.Level.APP)
class DigmaDefaultAccountHolder :
    PersistentStateComponent<AccountState>,
    DefaultAccountHolder<DigmaAccount> {

    private val logger: Logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(): DigmaDefaultAccountHolder {
            return service<DigmaDefaultAccountHolder>()
        }
    }

    private var fireNotification = true

    val accountName: AtomicProperty<String> = AtomicProperty("Not logged in")

    private val accountManager: AccountManager<DigmaAccount, DigmaCredentials> = DigmaAccountManager.getInstance()

    override var account: DigmaAccount? = null
        set(value) {
            Log.log(logger::info, "setAccount called with {}", value)

            val oldAccount = this.account

            field = value

            if (value == null) {
                accountName.set("Not logged in")
            } else {
                val name = Urls.newFromEncoded(value.server.url).authority
                accountName.set("Using account $name")
            }

            if (oldAccount != this.account && fireNotification) {
                notifyDefaultAccountChanged()
            }
        }


    private fun notifyDefaultAccountChanged() {
        Log.log(logger::info, "notifying DefaultAccountChanged")
        service<DefaultAccountChangedManager>().defaultAccountChanged()
    }

    private fun notifyDefaultAccountMissing() {
        Log.log(logger::info, "notifyDefaultAccountMissing called")

        //don't show a notification but maybe need to show for centralized backend
//        NotificationUtil.showBalloonWarning(
//            findActiveProject(),
//            "default Digma account is missing. please try to re-login"
//        )
    }

    override fun getState(): AccountState {
        val state = AccountState().apply { defaultAccountId = account?.id }
        Log.log(logger::info, "getState called {}", state)
        return state
    }

    override fun loadState(state: AccountState) {
        Log.log(logger::info, "loadState called {}", state)
        fireNotification = false
        try {
            account = state.defaultAccountId?.let { id ->
                accountManager.accountsState.value.find { it.id == id }.also {
                    if (it == null) notifyDefaultAccountMissing()
                }
            }
        } finally {
            fireNotification = true
        }

    }


    override fun noStateLoaded() {
        Log.log(logger::info, "noStateLoaded called")
        super.noStateLoaded()
    }

    override fun initializeComponent() {
        Log.log(logger::info, "initializeComponent called")
        super.initializeComponent()
    }

}

class AccountState {
    var defaultAccountId: String? = null
}