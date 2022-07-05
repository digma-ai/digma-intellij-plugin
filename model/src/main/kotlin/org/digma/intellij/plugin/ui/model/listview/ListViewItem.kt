package org.digma.intellij.plugin.ui.model.listview

open class ListViewItem<MO>(val modelObject: MO, val sortIndex: Int) {

    //moreData is if necessary to enrich the object with extra data.
    //see for example SlowestSpans
    val moreData = HashMap<String,Any>()
}