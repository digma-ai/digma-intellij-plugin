package org.digma.intellij.plugin.editor

import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

private val insightsPaginationInfo: ConcurrentHashMap<String, ConcurrentHashMap<String, Int>> = ConcurrentHashMap()

fun addInsightPaginationInfo(focusedDocumentName: String, uniqueInsightId: String, activePage: Int) {
    if (activePage > 1) {
        val insightsPaginationInfosMapForActualFile = insightsPaginationInfo[focusedDocumentName]
        if (insightsPaginationInfosMapForActualFile != null) {
            insightsPaginationInfosMapForActualFile[uniqueInsightId] = activePage
        } else {
            val filesAndInsightsPaginationInfosMap: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
            filesAndInsightsPaginationInfosMap[uniqueInsightId] = activePage
            insightsPaginationInfo[focusedDocumentName] = filesAndInsightsPaginationInfosMap
        }
    } else if (insightsPaginationInfo[focusedDocumentName] != null && insightsPaginationInfo[focusedDocumentName]!!.containsKey(uniqueInsightId)) {
        // remove pagination info for insights which had some page selected before
        insightsPaginationInfo[focusedDocumentName]!!.remove(uniqueInsightId)
    }
}

fun getInsightPaginationInfo(uniqueInsightId: String): Int? {
    return insightsPaginationInfo[getFocusedDocumentName()]?.get(uniqueInsightId)
}

fun removeInsightsPaginationInfoForClosedDocument(documentName: String) {
    if (insightsPaginationInfo.isNotEmpty()) {
        insightsPaginationInfo.remove(documentName)
    }
}

fun <T> updateListOfEntriesToDisplay(
        entries: List<T>,
        entriesToDisplay: ArrayList<T>,
        currPageNum: Int,
        recordsPerPage: Int,
        project: Project
) {
    entriesToDisplay.clear()
    if (entries.isNotEmpty()) {
        val start = (currPageNum - 1) * recordsPerPage
        var end = start + recordsPerPage
        if (end >= entries.size) {
            end = entries.size
        }
        for (i in start until end) {
            entriesToDisplay.add(entries[i])
        }
    }
}

fun getCurrentPageNumberForInsight(uniqueInsightId: String, lastPageNum: Int): Int {
    var currPageNum = 0
    getInsightPaginationInfo(uniqueInsightId)?.let { currPageNum = getInsightPaginationInfo(uniqueInsightId)!! }
    if (currPageNum < 1) {
        currPageNum = if (lastPageNum > 0) 1 else 0
    }
    return currPageNum
}