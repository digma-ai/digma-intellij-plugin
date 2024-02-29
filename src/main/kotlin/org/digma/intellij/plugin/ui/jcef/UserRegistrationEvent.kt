package org.digma.intellij.plugin.ui.jcef

import com.intellij.util.messages.Topic

interface UserRegistrationEvent {

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val USER_REGISTRATION_TOPIC: Topic<UserRegistrationEvent> = Topic.create(
            "USER_REGISTRATION_EVENT",
            UserRegistrationEvent::class.java
        )
    }


    fun userRegistered(email: String)
}