package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Observes property change events on jcef components and decides if to reload the jcef apps.
 * will do nothing if it's not macOS
 */
@Service(Service.Level.APP)
class ReloadObserver(cs: CoroutineScope) {

    private val logger = Logger.getInstance(ReloadObserver::class.java)

    private val propertyChangedEvents: Queue<Pair<ComponentDetails, PropertyChangeEvent>> = ConcurrentLinkedQueue()

    init {

        if (SystemInfo.isMac) {
            //a long-running coroutine that processes the events in the order they arrive
            cs.launch {
                while (isActive) {
                    try {
                        val event = propertyChangedEvents.poll()
                        if (event == null) {
                            delay(2000)
                        } else {
                            checkChangesAndReload(event)
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("ReloadObserver.mainLoop", e)
                    }
                }
            }
        }
    }


    fun register(jcefWrapperPanel: JPanel, jcefUiComponent: JComponent, parentDisposable: Disposable) {

        if (GraphicsEnvironment.isHeadless()) {
            Log.log(logger::trace, "GraphicsEnvironment is headless, not registering components")
            return
        }

        if (!SystemInfo.isMac) {
            Log.log(logger::trace, "system is not mac, not registering components")
            return
        }

        val jcefPropertyChangeListener =
            MyPropertyChangeListener(jcefUiComponent, "${jcefWrapperPanel.javaClass.simpleName}.jcefUiComponent")
        jcefUiComponent.addPropertyChangeListener(jcefPropertyChangeListener)

        Disposer.register(parentDisposable) {
            jcefUiComponent.removePropertyChangeListener(jcefPropertyChangeListener)
        }

    }


    private suspend fun checkChangesAndReload(event: Pair<ComponentDetails, PropertyChangeEvent>) {
        try {

            Log.log(logger::trace, "checking graphics changes for component {}", event.first.componentName)

            val componentDetails = event.first
            val component = componentDetails.component

            val currentDisplayMode = component.graphicsConfiguration?.device?.displayMode
            val currentGraphicsDevice = component.graphicsConfiguration?.device?.iDstring
            if (currentGraphicsDevice != componentDetails.graphicDevice) {
                Log.log(
                    logger::trace,
                    "component {} moved to another graphics device, oldValue:{},newValue:{}",
                    componentDetails.componentName,
                    componentDetails.graphicDevice,
                    currentGraphicsDevice
                )

                componentDetails.graphicDevice = currentGraphicsDevice
                componentDetails.displayMode = currentDisplayMode

                delay(1000)

                val currentGraphicsDeviceNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
                if (currentGraphicsDeviceNumber != componentDetails.graphicsDeviceNumber) {
                    Log.log(
                        logger::trace,
                        "graphics device number has changed for component {} ,oldValue:{},newValue:{}",
                        componentDetails.componentName,
                        componentDetails.graphicsDeviceNumber,
                        currentGraphicsDeviceNumber
                    )
                    componentDetails.graphicsDeviceNumber = currentGraphicsDeviceNumber

                    service<ReloadService>().reload()
                }
            } else if (currentDisplayMode != componentDetails.displayMode) {
                Log.log(
                    logger::trace,
                    "component {} display mode has changed, oldValue:{},newValue:{}",
                    componentDetails.componentName,
                    componentDetails.displayMode,
                    currentDisplayMode
                )
                componentDetails.displayMode = currentDisplayMode

                service<ReloadService>().reload()
            } else {
                Log.log(logger::trace, "no graphics changes for component {}", componentDetails.componentName)
            }

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ReloadObserver.checkChangesAndReload", e)
        }
    }


    private class ComponentDetails(val component: Component, val componentName: String) {
        var graphicDevice = component.graphicsConfiguration?.device?.iDstring
        var displayMode = component.graphicsConfiguration?.device?.displayMode
        var graphicsDeviceNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
    }


    private inner class MyPropertyChangeListener(
        component: Component,
        private val componentName: String
    ) :
        PropertyChangeListener {

        private val componentDetails = ComponentDetails(component, componentName)

        override fun propertyChange(evt: PropertyChangeEvent) {
            if (evt.propertyName == "graphicsConfiguration") {
                Log.log(logger::trace, "got PropertyChangeEvent for {}, {}", componentName, evt)
                //putting the events in a queue ensures we process them in the order they arrive.
                //just launching a coroutine here does not guarantee order and may cause wrong decisions
                //about reloading
                propertyChangedEvents.add(Pair(componentDetails, evt))
            }
        }
    }

}