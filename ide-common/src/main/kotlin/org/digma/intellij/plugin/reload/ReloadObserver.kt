package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.common.isProjectValid
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
import kotlin.time.Duration.Companion.seconds


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
                            delay(1000)
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


    fun register(project: Project, jcefWrapperPanel: JPanel, jcefUiComponent: JComponent, parentDisposable: Disposable) {

        if (GraphicsEnvironment.isHeadless()) {
            Log.log(logger::trace, "GraphicsEnvironment is headless, not registering components")
            return
        }

        if (!SystemInfo.isMac) {
            Log.log(logger::trace, "system is not mac, not registering components")
            return
        }

        val jcefPropertyChangeListener =
            MyPropertyChangeListener(project, jcefUiComponent, "${jcefWrapperPanel.javaClass.simpleName}.jcefUiComponent")
        jcefUiComponent.addPropertyChangeListener(jcefPropertyChangeListener)

        Disposer.register(parentDisposable) {
            jcefUiComponent.removePropertyChangeListener(jcefPropertyChangeListener)
        }

    }


    private suspend fun checkChangesAndReload(event: Pair<ComponentDetails, PropertyChangeEvent>) {
        try {

            val componentDetails = event.first
            val component = componentDetails.component
            val componentName = componentDetails.componentName
            val project = componentDetails.project

            Log.log(logger::trace, "checking graphics changes for component {} in project {}", componentName, project.name)


            if (!isProjectValid(project)) {
                Log.log(
                    logger::trace,
                    "skipping checking graphics changes for component {} in project {} because project is invalid",
                    componentName,
                    project.name
                )
                return
            }

            val currentDisplayMode = component.graphicsConfiguration?.device?.displayMode
            val currentGraphicsDevice = component.graphicsConfiguration?.device?.iDstring
            if (currentGraphicsDevice != componentDetails.graphicDevice) {
                Log.log(
                    logger::trace,
                    "component {} in project {} moved to another graphics device, oldValue:{},newValue:{}",
                    componentName,
                    project.name,
                    componentDetails.graphicDevice,
                    currentGraphicsDevice
                )

                componentDetails.graphicDevice = currentGraphicsDevice
                componentDetails.displayMode = currentDisplayMode

                //it may take the GraphicsEnvironment some time to refresh the screen devices. it depends on native OS calls timing,
                // it may take the OS some seconds to notify the GraphicsEnvironment about changes.
                //if we don't catch the change we may miss a reload when it's needed.
                //so wait for some seconds to try to detect the change. if there is no change no harm will be done and this
                // coroutine will just finish doing nothing
                var currentGraphicsDeviceNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
                val endWait = Clock.System.now() + 3.seconds
                while (currentGraphicsDeviceNumber == componentDetails.graphicsDeviceNumber &&
                    Clock.System.now() < endWait
                ) {
                    Log.log(logger::trace, "waiting for device number to refresh for component {} in project {}", componentName, project.name)
                    delay(100)
                    currentGraphicsDeviceNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
                }

                currentGraphicsDeviceNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
                if (currentGraphicsDeviceNumber != componentDetails.graphicsDeviceNumber) {
                    Log.log(
                        logger::trace,
                        "graphics device number has changed for component {} in project {},oldValue:{},newValue:{}",
                        componentName,
                        project.name,
                        componentDetails.graphicsDeviceNumber,
                        currentGraphicsDeviceNumber
                    )
                    componentDetails.graphicsDeviceNumber = currentGraphicsDeviceNumber

                    service<ReloadService>().reload(project)
                } else {
                    Log.log(
                        logger::trace,
                        "graphics device number has NOT changed for component {} in project {},oldValue:{},newValue:{}",
                        componentName,
                        project.name,
                        componentDetails.graphicsDeviceNumber,
                        currentGraphicsDeviceNumber
                    )
                }
            } else if (currentDisplayMode != componentDetails.displayMode) {
                Log.log(
                    logger::trace,
                    "component {} in project {} display mode has changed, oldValue:{},newValue:{}",
                    componentName,
                    project.name,
                    componentDetails.displayMode,
                    currentDisplayMode
                )
                componentDetails.displayMode = currentDisplayMode

                service<ReloadService>().reload(project)
            } else {
                Log.log(logger::trace, "no graphics changes for component {} in project {}", componentName, project.name)
            }

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ReloadObserver.checkChangesAndReload", e)
        }
    }


    private class ComponentDetails(val project: Project, val component: Component, val componentName: String) {
        var graphicDevice = component.graphicsConfiguration?.device?.iDstring
        var displayMode = component.graphicsConfiguration?.device?.displayMode
        var graphicsDeviceNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
    }


    private inner class MyPropertyChangeListener(
        project: Project,
        component: Component,
        private val componentName: String,
    ) :
        PropertyChangeListener {

        private val componentDetails = ComponentDetails(project, component, componentName)

        override fun propertyChange(evt: PropertyChangeEvent) {
            if (evt.propertyName == "graphicsConfiguration") {
                Log.log(logger::trace, "got PropertyChangeEvent for {}, {}", componentName, evt)

                //componentDetails.graphicDevice may be null on startup so run the event,
                // but if it's not null and new value or old value are null this only means that the component
                // becomes visible or hidden
                if (componentDetails.graphicDevice != null) {
                    if (evt.oldValue == null || evt.newValue == null) {
                        Log.log(
                            logger::trace,
                            "not adding event because old value or new value is null in PropertyChangeEvent for {}, {}",
                            componentName,
                            evt
                        )
                        return
                    }
                }

                //putting the events in a queue ensures we process them in the order they arrive.
                //just launching a coroutine here does not guarantee order and may cause wrong decisions
                //about reloading
                propertyChangedEvents.add(Pair(componentDetails, evt))
            }
        }
    }

}