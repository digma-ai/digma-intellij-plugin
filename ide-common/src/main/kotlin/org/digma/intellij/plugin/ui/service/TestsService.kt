package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable

interface TestsService : Disposable {

    fun getLatestTestsOfSpan(spanCodeObjectId: String, environments: Set<String>, pageNumber: Int, pageSize: Int): String

}