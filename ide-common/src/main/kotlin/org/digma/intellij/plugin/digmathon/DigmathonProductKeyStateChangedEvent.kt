package org.digma.intellij.plugin.digmathon

import com.intellij.util.messages.Topic

fun interface DigmathonProductKeyStateChangedEvent {

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val PRODUCT_KEY_STATE_CHANGED_TOPIC: Topic<DigmathonProductKeyStateChangedEvent> = Topic.create(
            "PRODUCT KEY STATE CHANGED",
            DigmathonProductKeyStateChangedEvent::class.java
        )
    }


    fun productKey(productKey: String?)

}