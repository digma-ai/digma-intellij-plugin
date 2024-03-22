package org.digma.intellij.plugin.auth.account

import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.kotlin.ext.isZero
import org.digma.intellij.plugin.log.Log

/**
 * AccountsRepository for DigmaAccounts , it's a single account repository but can be
 * easily changed to support multiple accounts.
 */
@State(
    name = "org.digma.intellij.plugin.DigmaPersistentAccounts",
    storages = [Storage(value = "digma-user-accounts.xml")],
    reportStatistic = false, category = SettingsCategory.TOOLS
)
@Service(Service.Level.APP)
internal class DigmaPersistentAccounts
    : AccountsRepository<DigmaAccount>,
    PersistentStateComponent<Array<DigmaAccount>> {

    private val logger: Logger = Logger.getInstance(this::class.java)

    override var accounts: Set<DigmaAccount> = setOf()
        set(value) {
            Log.log(logger::info, "new accounts list set {}", value)
            field = value
        }

    override fun getState(): Array<DigmaAccount> {
        return accounts.toTypedArray()
    }

    override fun loadState(state: Array<DigmaAccount>) {
        Log.log(logger::info, "loading state {}", state)
        if (state.size.isZero()) {
            accounts = setOf()
        }
        accounts = state.toSet()
    }


    override fun noStateLoaded() {
        Log.log(logger::info, "noStateLoaded")
        super.noStateLoaded()
    }

    override fun initializeComponent() {
        Log.log(logger::info, "initializeComponent")
        super.initializeComponent()
    }

}