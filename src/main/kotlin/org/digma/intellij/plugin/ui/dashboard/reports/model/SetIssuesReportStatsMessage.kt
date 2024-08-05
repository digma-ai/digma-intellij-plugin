package org.digma.intellij.plugin.ui.dashboard.reports.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetIssuesReportStatsMessage(@JsonRawValue val payload: String) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "DASHBOARD/SET_REPORT_ISSUES_STATS"
}