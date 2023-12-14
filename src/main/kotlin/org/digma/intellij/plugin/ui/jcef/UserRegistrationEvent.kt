package org.digma.intellij.plugin.ui.jcef

import com.intellij.util.messages.Topic

abstract class UserRegistrationEvent {


    companion object {
        @JvmStatic
        //todo: project or app level ?
        @Topic.AppLevel
        val USER_REGISTRATION_TOPIC: Topic<UserRegistrationEvent> = Topic.create(
            "USER_REGISTRATION_EVENT",
            UserRegistrationEvent::class.java
        )

    }


    abstract fun userRegistered(email: String)


}