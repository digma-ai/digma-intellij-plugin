package org.digma.intellij.plugin.idea.index

import com.intellij.util.indexing.ID

val CANDIDATE_FILES_INDEX_ID: ID<String, Void> = ID.create("org.digma.discovery.index.Files")
const val CANDIDATE_FILES_INDEX_KEY_SPAN = "SPAN"
const val CANDIDATE_FILES_INDEX_KEY_ENDPOINT = "ENDPOINT"