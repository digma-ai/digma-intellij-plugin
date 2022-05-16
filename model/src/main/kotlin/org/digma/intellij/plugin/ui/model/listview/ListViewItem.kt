package org.digma.intellij.plugin.ui.model.listview

abstract class ListViewItem(val sortIndex: Int) {
    lateinit var codeObjectId: String
}