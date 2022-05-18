package org.digma.intellij.plugin.ui.list

import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

open class PanelList<T> : JPanel(), ListDataListener {

    private var model: PanelListModel<T> = DefaultPanelListModel()
    private var cellRenderer: PanelListCellRenderer<T> = DefaultPanelListCellRenderer()

//    private val viewport: JPanel = JPanel()
//    private var scrollPane: JScrollPane = JBScrollPane()

    init {
        model.addListDataListener(this)
        model.addListDataListener(cellRenderer)
        this.layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
//        this.layout = BorderLayout()
//        this.layout = GridLayout
        this.border = JBUI.Borders.empty(5)

//        this.maximumSize = Dimension(-1,500)
//        this.preferredSize = Dimension(-1,500)

//        viewport.layout = BoxLayout(viewport,BoxLayout.PAGE_AXIS)
//        viewport.maximumSize = Dimension(-1,300)
//        scrollPane.setViewportView(viewport)
//        scrollPane.maximumSize = Dimension(-1,300)

//        scrollPane.viewport = viewport
//        this.add(scrollPane,BorderLayout.CENTER)
//        this.add(scrollPane)

    }


    fun getModel(): PanelListModel<in T> {
        return model
    }

    fun setModel(newModel: PanelListModel<T>) {
        if (model === newModel) {
            return
        }
        this.model.removeListDataListener(this)
        this.model.removeListDataListener(cellRenderer)
        this.model = newModel
        this.model.addListDataListener(this)
        this.model.addListDataListener(cellRenderer)
        //fire something ?
    }

    fun setCellRenderer(newCellRenderer: PanelListCellRenderer<T>) {
        if (this.cellRenderer === newCellRenderer) {
            return
        }
        this.model.removeListDataListener(this.cellRenderer)
        this.cellRenderer = newCellRenderer
        this.model.addListDataListener(this.cellRenderer)
    }


    private fun rebuild(e: ListDataEvent?) {

        this.components.forEach {
            this.remove(it)
        }


        for (i in 0..model.getSize() - 1) run {
            val newComp: Component = this.cellRenderer.getListCellRendererComponent(this,
                model.getElementAt(i), i, true)

            add(newComp)
            add(Box.createVerticalStrut(10))
        }

//        add(Box.createVerticalGlue())
        this.revalidate()
        this.repaint()


    }


    class DefaultPanelListCellRenderer<T> : AbstractPanelListCellRenderer<T>() {
        override fun createPanel(value: ListViewItem<T>, index: Int): Component {
            return JPanel()
        }
    }


    class DefaultPanelListModel<T> : AbstractPanelListModel<T>()


    override fun intervalAdded(e: ListDataEvent?) {
        rebuild(e)
    }

    override fun intervalRemoved(e: ListDataEvent?) {
        rebuild(e)
    }

    override fun contentsChanged(e: ListDataEvent?) {
        rebuild(e)
    }
}