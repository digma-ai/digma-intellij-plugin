package org.digma.intellij.plugin.digmathon

import com.intellij.util.messages.Topic

fun interface DigmathonActivationEvent {

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val DIGMATHON_ACTIVATION_TOPIC: Topic<DigmathonActivationEvent> = Topic.create(
            "DIGMATHON ACTIVATION",
            DigmathonActivationEvent::class.java
        )
    }


    fun digmathonActivationStateChanged(isActive: Boolean)

}