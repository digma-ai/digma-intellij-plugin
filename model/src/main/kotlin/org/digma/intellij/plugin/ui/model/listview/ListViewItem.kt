package org.digma.intellij.plugin.ui.model.listview

open class ListViewItem<MO>(val modelObject: MO, val sortIndex: Int) {

    override fun toString(): String {
        return "${javaClass.simpleName}(model=$modelObject, sortIndex=$sortIndex)"
    }
}