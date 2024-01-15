package org.digma.intellij.plugin.ui.reports

import com.intellij.diagnostic.AbstractMessage
import com.intellij.diagnostic.LogMessage
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.Consumer
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.awt.Component


class DigmaErrorReportSubmitter : ErrorReportSubmitter() {
    override fun getReportActionText(): String {
        return "Report to Digma"
    }

    override fun getPrivacyNoticeText(): String {
        return "I agree to product information, and the error details shown above being used by Digma INC." +
                " to let Digma improve its products in accordance with the privacy policy here <a href=\"https://digma.ai/privacy-policy/\">DIGMA PRIVACY POLICY</a>."
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {

        val mgr = DataManager.getInstance()
        val context = mgr.getDataContext(parentComponent)
        val project: Project = CommonDataKeys.PROJECT.getData(context) ?: findActiveProject() ?: ProjectManager.getInstance().defaultProject;

        Backgroundable.ensureBackground(project, "Reporting to Digma") {

            try {
                reportToPosthog(project, events, additionalInfo)
                EDT.ensureEDT {
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
                }
            } catch (e: Throwable) {
                EDT.ensureEDT {
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
                }
            }

        }
        return true
    }

    private fun reportToPosthog(project: Project, events: Array<out IdeaLoggingEvent>, additionalInfo: String?) {

        //from idea source code it looks like idea will always send one event, still we support many just in case..
        events.forEach {
            reportOneEvent(project, it, additionalInfo)
        }

    }

    private fun reportOneEvent(project: Project, event: IdeaLoggingEvent, additionalInfo: String?) {

        val details = mutableMapOf<String, String>()

        details["event.message"] = event.message ?: ""
        details["event.throwableText"] = event.throwableText ?: ""
        details["additionalUserInfo"] = additionalInfo ?: ""

        if (event.data is AbstractMessage) {
            val data = event.data as AbstractMessage
            details["event.date"] = data.date.toString()
        }

        if (event.data is LogMessage) {
            val data = event.data as LogMessage
            val attachments = data.includedAttachments

            attachments.forEach {
                details["event.attachment.${it.name}.displayText"] = it.displayText
            }
        }

        ActivityMonitor.getInstance(project).registerFatalError(details)

    }
}