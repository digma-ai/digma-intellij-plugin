package org.digma.intellij.plugin.auth.account

import com.intellij.util.messages.Topic

fun interface DefaultAccountChanged {

    companion object {
        @Topic.AppLevel
        var DEFAULT_ACCOUNT_CHANGED_TOPIC: Topic<DefaultAccountChanged> = Topic.create(
            "DEFAULT_ACCOUNT_CHANGED_TOPIC", DefaultAccountChanged::class.java
        )
    }

    fun defaultAccountChanged()

}