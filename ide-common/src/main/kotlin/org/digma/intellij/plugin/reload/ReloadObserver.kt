package org.digma.intellij.plugin.reload

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.log.Log
import java.awt.Component
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

@Service(Service.Level.APP)
class ReloadObserver {

    private val logger = Logger.getInstance("org.digmajcef.ReloadObserver")

    fun register(jcefPanel: JPanel, jcefUiComponent: JComponent, parentDisposable: Disposable) {

        val panelPropertyChangeListener = MyPropertyChangeListener(jcefPanel)
        jcefPanel.addPropertyChangeListener(panelPropertyChangeListener)

        val jcefPropertyChangeListener = MyPropertyChangeListener(jcefUiComponent)
        jcefUiComponent.addPropertyChangeListener(jcefPropertyChangeListener)

        Disposer.register(parentDisposable) {
            jcefPanel.removePropertyChangeListener(panelPropertyChangeListener)
            jcefUiComponent.removePropertyChangeListener(jcefPropertyChangeListener)
        }

    }


    private inner class MyPropertyChangeListener(private val component: Component) : PropertyChangeListener {


        override fun propertyChange(evt: PropertyChangeEvent) {

            Log.log(logger::trace, "got PropertyChangeEvent {}", evt)


        }

    }


}