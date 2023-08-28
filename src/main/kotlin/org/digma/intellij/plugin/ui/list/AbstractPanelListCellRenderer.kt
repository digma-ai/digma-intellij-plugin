package org.digma.intellij.plugin.ui.list

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

abstract class AbstractPanelListCellRenderer: PanelListCellRenderer {

    private val panels: MutableMap<Int, JPanel> = HashMap()

    override fun getListCellRendererComponent(project: Project,
                                              list: PanelList,
                                              value: ListViewItem<*>,
                                              index: Int,
                                              cellHasFocus: Boolean,
                                              panelsLayoutHelper: PanelsLayoutHelper): JPanel {


        if (panels.containsKey(index)) {
            return panels[index]!!
        }

        //catch any exception so if a panel throws exception the following panels will still be built.
        //will return a JPanel instead that is not visible. the user will not see any error, error is in the log
        return try {
            val panel = createPanel(project, value, index, panelsLayoutHelper)

            panels[index] = panel

            wrap(panel)

        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError(project, "error creating list panel for ${value.modelObject}", e)
            Log.warnWithException(Logger.getInstance(this::class.java), e, "Error creating panel for {}", value.modelObject)
            val p = JPanel()
            p.isVisible = false
            p
        }
    }


    abstract fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel


    private fun wrap(panel: JPanel): JPanel {
        return panel
    }




    override fun intervalAdded(e: ListDataEvent?) {
        panels.clear()
    }

    override fun intervalRemoved(e: ListDataEvent?) {
        panels.clear()
    }

    override fun contentsChanged(e: ListDataEvent?) {
        panels.clear()
    }


}