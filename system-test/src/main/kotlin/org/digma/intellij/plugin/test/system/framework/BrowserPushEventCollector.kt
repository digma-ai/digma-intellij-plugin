package org.digma.intellij.plugin.test.system.framework

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


class BrowserPushEventCollector {

    private val logger = Logger.getInstance(BrowserPushEventCollector::class.java)

    private val actionsQueueMap: ConcurrentHashMap<String, ConcurrentLinkedQueue<JsonNode>> = ConcurrentHashMap() // <action, queue<JsonNode>> 


    /**
     * adds an event to the queue for the given action. creates a new queue if action is not present.
     * @param action - action name
     * @param browserJson - json string to be sent to browser
     *
     */
    fun addEvent(action: String, browserJson: JsonNode) {
        Log.test(logger::info, "adding event for action $action, size of the queue: {}", actionsQueueMap[action]?.size ?: 0)
        actionsQueueMap.computeIfAbsent(action) { ConcurrentLinkedQueue() }.add(browserJson)
    }

    /**
     * @return empty string if no event for action found
     */
    fun popEvent(action: String): JsonNode {
        return actionsQueueMap[action]?.poll() ?: JsonNodeFactory.instance.nullNode()
    }

    /**
     * @return true if there are events for the given action
     */
    fun hasEvents(action: String): Boolean {
        return actionsQueueMap[action]?.isNotEmpty() ?: false
    }

    /**
     * clears all events for the given action
     * @param action - action name
     */
    fun clearEvents(action: String) {
        actionsQueueMap[action]?.clear()
    }

}