package org.digma.intellij.plugin.digmathon

import com.intellij.util.messages.Topic

fun interface UserFinishedDigmathonEvent {

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val USER_FINISHED_DIGMATHON_TOPIC: Topic<UserFinishedDigmathonEvent> = Topic.create(
            "USER FINISHED DIGMATHON",
            UserFinishedDigmathonEvent::class.java
        )
    }


    fun userFinishedDigmathon()

}