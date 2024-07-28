package org.digma.intellij.plugin.auth.account

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.log.Log
import kotlin.coroutines.coroutineContext

//this is a crud like operations to create,update,delete accounts under lock.
//should be the only place to update or delete accounts.
object SingletonAccountUpdater {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val mutex = Mutex()

    suspend fun updateNewAccount(digmaAccount: DigmaAccount, digmaCredentials: DigmaCredentials) {
        trace("updating account {}", digmaAccount)
        mutex.withLock {
            DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
            DigmaDefaultAccountHolder.getInstance().account = digmaAccount
            CredentialsHolder.digmaCredentials = digmaCredentials
        }
    }

    suspend fun updateAccount(digmaAccount: DigmaAccount, digmaCredentials: DigmaCredentials) {
        trace("updating account {}", digmaAccount)
        mutex.withLock {
            DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
            DigmaDefaultAccountHolder.getInstance().account = digmaAccount
            CredentialsHolder.digmaCredentials = digmaCredentials
        }
    }


    suspend fun deleteAccount(digmaAccount: DigmaAccount) {
        trace("deleting account {}", digmaAccount)
        mutex.withLock {
            try {
                DigmaAccountManager.getInstance().removeAccount(digmaAccount)
            } finally {
                //it's in finally because even if removeAccount failed we want to delete the account and nullify CredentialsHolder.digmaCredentials
                DigmaDefaultAccountHolder.getInstance().account = null
                CredentialsHolder.digmaCredentials = null
            }
        }
    }


    suspend fun trace(format: String, vararg args: Any?) {
        Log.log(logger::trace, "${coroutineContext[CoroutineName]}: $format", *args)
    }

}