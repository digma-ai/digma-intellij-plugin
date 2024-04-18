package org.digma.intellij.plugin.auth.account

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.intellij.collaboration.auth.Account
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.collaboration.auth.CredentialsRepository
import com.intellij.collaboration.auth.PasswordSafeCredentialsRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.Urls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.APP)
class DigmaAccountManager
    : AccountManagerBase<DigmaAccount, DigmaCredentials>(logger<DigmaAccountManager>()) {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val objectMapper = ObjectMapper()

    init {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.setDateFormat(StdDateFormat())
    }

    companion object {

        const val SERVICE_DISPLAY_NAME: String = "Digma Accounts"

        @JvmStatic
        fun getInstance(): DigmaAccountManager {
            return service<DigmaAccountManager>()
        }

        private val logger: Logger = Logger.getInstance(this::class.java)
        fun createAccount(url: String, userId: String): DigmaAccount {
            Log.log(logger::info, "creating new DigmaAccount for {}", url)
            val name = Urls.newFromEncoded(url).authority.toString()
            return DigmaAccount(Account.generateId(), name, MyServerPath(url), userId)
        }
    }

    override fun accountsRepository(): AccountsRepository<DigmaAccount> = service<DigmaPersistentAccounts>()

    override fun credentialsRepository(): CredentialsRepository<DigmaAccount, DigmaCredentials> =
        PasswordSafeCredentialsRepository(
            SERVICE_DISPLAY_NAME,
            object : PasswordSafeCredentialsRepository.CredentialsMapper<DigmaCredentials> {
                override fun serialize(credentials: DigmaCredentials): String =
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials)

                override fun deserialize(credentials: String): DigmaCredentials =
                    objectMapper.readValue(credentials, DigmaCredentials::class.java)
            })


    override suspend fun findCredentials(account: DigmaAccount): DigmaCredentials? {
        //too many log messages...
//        Log.log(logger::debug, "findCredentials called for account {}", account)
        return super.findCredentials(account)
    }

    override fun getCredentialsFlow(account: DigmaAccount): Flow<DigmaCredentials?> {
        Log.log(logger::debug, "getCredentialsFlow called for account {}", account)
        return super.getCredentialsFlow(account)
    }

    override suspend fun getCredentialsState(scope: CoroutineScope, account: DigmaAccount): StateFlow<DigmaCredentials?> {
        Log.log(logger::debug, "getCredentialsState called for account {}", account)
        return super.getCredentialsState(scope, account)
    }

    override suspend fun removeAccount(account: DigmaAccount) {
        Log.log(logger::info, "removeAccount called for account {}", account)
        super.removeAccount(account)
    }

    override suspend fun updateAccount(account: DigmaAccount, credentials: DigmaCredentials) {
        Log.log(logger::info, "updateAccount called for account {},{}", account, credentials)
        super.updateAccount(account, credentials)
    }

    override suspend fun updateAccounts(accountsWithCredentials: Map<DigmaAccount, DigmaCredentials?>) {
        Log.log(logger::info, "updateAccounts called for account {}", accountsWithCredentials)
        super.updateAccounts(accountsWithCredentials)
    }
}