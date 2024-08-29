package org.digma.intellij.plugin.activation

import com.intellij.util.messages.Topic

fun interface UserClickedNotificationEvent {

    companion object {
        @JvmStatic
        @Topic.ProjectLevel
        val USER_CLICKED_NOTIFICATION_TOPIC: Topic<UserClickedNotificationEvent> = Topic.create(
            "USER CLICKED NOTIFICATION TOPIC",
            UserClickedNotificationEvent::class.java
        )
    }

    fun notificationClicked(name: String)

}
