package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.rest.insights.SpanFlow

class SpanListViewItem(val flows: MutableList<SpanFlow>, sortIndex: Int) : InsightListViewItem(sortIndex){

}