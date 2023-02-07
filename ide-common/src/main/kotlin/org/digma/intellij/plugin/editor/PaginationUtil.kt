package org.digma.intellij.plugin.editor

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
    } else if (insightsPaginationInfo.contains(uniqueInsightId)) {
        // remove pagination info for insights which had some page selected before
        insightsPaginationInfo.remove(uniqueInsightId)
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