package org.digma.intellij.plugin.auth.account

import org.digma.intellij.plugin.auth.credentials.DigmaCredentials

//credentials holder is always updated when updating the account and credentials
//we need it in DefaultAccountTokenProvider.
//without it, we need to call DigmaAccountManager.getInstance().findCredentials(digmaAccount) which is a suspending function
// and needs to be called in a coroutine like runBlocking, but we don't want to do that in DefaultAccountTokenProvider because it is part
// of okhttp interceptor and should be very fast.
//it is an object and thus a singleton per jvm.
object CredentialsHolder {
    var digmaCredentials: DigmaCredentials? = null
}