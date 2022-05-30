package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.InsightType
import kotlin.test.Test
import kotlin.test.fail

internal class InsightListViewItemTest {

    @Test
    fun verifyThatSortIndexPerInsightTypeReturnsUniqueValue() {
        val verifyMap = HashMap<Int, InsightType>()
        for (type in InsightType.values()) {
            val currSortIndex = InsightListViewItem.sortIndexOf(type)
            if (verifyMap.containsKey(currSortIndex)) {
                val usedByType = verifyMap[currSortIndex]
                fail("returned SortIndex '$currSortIndex' for InsightType '$type' already being used by type '$usedByType'")
            }
            verifyMap[currSortIndex] = type
        }
    }
}