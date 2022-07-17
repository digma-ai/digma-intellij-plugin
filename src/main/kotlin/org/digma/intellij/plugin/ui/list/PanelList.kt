package org.digma.intellij.plugin.ui.list

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Laf.scaleBorders
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.util.*
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.Scrollable
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

abstract class PanelList(val project: Project, listViewItems: List<ListViewItem<*>>, private val gapBetweenItems: Boolean = true) : JBPanel<PanelList>(), ListDataListener, Scrollable {

    private val LOGGER = Logger.getInstance(PanelList::class.java)

    private var model: PanelListModel = DefaultPanelListModel()
    private var cellRenderer: PanelListCellRenderer = DefaultPanelListCellRenderer()
    private var scrollablePanelList: ScrollablePanelList? = null


    init {
        //set data before registering listeners
        model.setListData(listViewItems)
        model.addListDataListener(this)
        model.addListDataListener(cellRenderer)
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        var size = scaleBorders(5)
        this.border = Borders.empty(size)
        this.background = getListBackground()
        this.isOpaque = true
    }


    abstract fun getListBackground():Color

    override fun getBackground(): Color {
        return getListBackground()
    }

    fun getModel(): PanelListModel {
        return model
    }

    @Suppress("unused")
    fun setModel(newModel: PanelListModel) {
        Objects.requireNonNull(newModel,"New model is null")
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



    fun setScrollablePanelListPanel(scrollablePanelList: ScrollablePanelList) {
        this.scrollablePanelList = scrollablePanelList
    }

    override fun update(g: Graphics?) {
        rebuild()
        super.update(g)
    }



    private fun rebuild() {

        //create a new instance for every rebuild
        val panelsLayoutHelper = PanelsLayoutHelper()

        Log.log(LOGGER::debug, "{} in rebuild,model size: {}",this.javaClass.simpleName, model.size)

        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
            revalidate()
        }

        if (model.size <= 0)
            return

        for (i in 0 until model.getSize()) run {
            cellRenderer.apply {
                val newComp: JPanel = getListCellRendererComponent(project,this@PanelList,
                    model.getElementAt(i), i, this@PanelList.hasFocus(),panelsLayoutHelper)
                add(newComp)
                if (gapBetweenItems) {
                    add(Box.createVerticalStrut(Laf.scaleBorders(3)))
                }
            }

        }

        this.revalidate()
    }


    class DefaultPanelListCellRenderer : AbstractPanelListCellRenderer() {
        override fun createPanel(project: Project,
                                 value: ListViewItem<*>,
                                 index: Int,
                                 panelsLayoutHelper: PanelsLayoutHelper): JPanel {
            return JPanel()
        }
    }


    class DefaultPanelListModel : AbstractPanelListModel()



    override fun intervalAdded(e: ListDataEvent?) {
        rebuild()
    }

    override fun intervalRemoved(e: ListDataEvent?) {
        rebuild()
    }

    override fun contentsChanged(e: ListDataEvent?) {
        rebuild()
    }

    override fun getPreferredScrollableViewportSize(): Dimension? {

        Log.log(LOGGER::debug, "in getPreferredScrollableViewportSize,my size: {}", size)

        if (scrollablePanelList != null){
            return Dimension(scrollablePanelList!!.size.width-20, scrollablePanelList!!.size.height-20)
        }

        return Dimension(500,300)
    }

    override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int {
        return 30
    }

    override fun getScrollableBlockIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int {
        return 60
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        return true
    }

    override fun getScrollableTracksViewportHeight(): Boolean {
        return false
    }

}