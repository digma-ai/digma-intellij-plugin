package org.digma.intellij.plugin.ui.model.listview

/**
 * an item in a UI list.
 * ListViewItem implementations should not be kotlin data classes, we don't want a heavy equals and hashcode.
 */
abstract class ListViewItem(val sortIndex: Int) {
    lateinit var codeObjectId: String

    override fun equals(other: Any?): Boolean {
         return this === other
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}