package org.digma.intellij.plugin.ui.list

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

open class PanelList(listViewItems: List<ListViewItem<*>>) : JBPanel<PanelList>(), ListDataListener {

    private var model: PanelListModel = DefaultPanelListModel()
    private var cellRenderer: PanelListCellRenderer = DefaultPanelListCellRenderer()


//    getPreferredScrollableViewportSize

//    private val viewport: JPanel = JPanel()
//    private var scrollPane: JScrollPane = JBScrollPane()

    init {
        //set data before registering listeners
        model.setListData(listViewItems)
        model.addListDataListener(this)
        model.addListDataListener(cellRenderer)
        this.layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
//        this.layout = BorderLayout()
//        this.layout = GridLayout(0,1,5,10)
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


    fun getModel(): PanelListModel {
        return model
    }

    fun setModel(newModel: PanelListModel) {
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

    fun setCellRenderer(newCellRenderer: PanelListCellRenderer) {
        if (this.cellRenderer === newCellRenderer) {
            return
        }
        this.model.removeListDataListener(this.cellRenderer)
        this.cellRenderer = newCellRenderer
        this.model.addListDataListener(this.cellRenderer)
    }


//    override fun repaint() {
//        super.repaint()
//    }

    private fun rebuild(e: ListDataEvent?) {

        if (components.isNotEmpty()){
            this.components.forEach {
                this.remove(it)
            }
            //revalidate()
        }

        if (model.size <= 0)
            return

        for (i in 0..model.getSize() - 1) run {
            val newComp: JPanel = this.cellRenderer.getListCellRendererComponent(this,
                model.getElementAt(i), i, true)

//            newComp.alignmentX = Component.LEFT_ALIGNMENT;
//            newComp.preferredSize = Dimension(500, 300)
//            newComp.maximumSize = Dimension(1000, 400)

//            newComp.preferredSize = Dimension(this.parent.parent.parent.preferredSize.width,newComp.preferredSize.height)
            add(newComp)
//            add(Box.createVerticalStrut(10))
        }

//        add(Box.createVerticalGlue())
        this.revalidate()
        this.repaint()
    }


    class DefaultPanelListCellRenderer : AbstractPanelListCellRenderer() {
        override fun createPanel(value: ListViewItem<*>, index: Int): JPanel {
            return JPanel()
        }
    }


    class DefaultPanelListModel : AbstractPanelListModel()


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