package org.digma.intellij.plugin.idea.index

import com.intellij.util.indexing.FileBasedIndexExtension.EXTENSION_POINT_NAME

//can't put a getInstance in a companion object on extension because jetbrains warn about it.
//must support nullable, but it should always succeed.
fun getCandidateFilesForDiscoveryIndexInstance(): CandidateFilesForDiscoveryDetectionIndex? {
    return EXTENSION_POINT_NAME.findExtension<CandidateFilesForDiscoveryDetectionIndex>(CandidateFilesForDiscoveryDetectionIndex::class.java)
}