package org.digma.intellij.plugin.ui.reports

import com.intellij.diagnostic.AbstractMessage
import com.intellij.diagnostic.LogMessage
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.Consumer
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.UserId
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import java.awt.Component


class DigmaErrorReportSubmitter : ErrorReportSubmitter() {


    override fun getReportActionText(): String {

        //getReportActionText is called every time before the error dialog opens, so use it as a hook to
        // send an event. it's an indication that user opened the error dialog on our error.
        findActiveProject()?.let {
            ActivityMonitor.getInstance(it).registerCustomEvent("prepare ErrorReportSubmitter", mapOf())
        }

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
                ErrorReporter.getInstance().reportError("DigmaErrorReportSubmitter.submit", e)
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

        //ordered for easier reading in posthog, although posthog doesn't always keep the order
        details["exceptionClass"] = getExceptionClassName(event)
        details["exceptionMessage"] = getExceptionMessage(event)
        details["additionalUserInfo"] = additionalInfo ?: ""
        details["event.message"] = event.message ?: ""

        val osType = System.getProperty("os.name")
        val ideInfo = ApplicationInfo.getInstance()
        val ideName = ideInfo.versionName
        val ideVersion = ideInfo.fullVersion
        val ideBuildNumber = ideInfo.build.asString()
        val pluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")

        details["ide.name"] = ideName
        details["ide.version"] = ideVersion
        details["ide.build"] = ideBuildNumber
        details["plugin.version"] = pluginVersion
        details["os.type"] = osType
        details["user.type"] = if (UserId.isDevUser) "internal" else "external"


        if (event.data is AbstractMessage) {
            val data = event.data as AbstractMessage
            details["event.date"] = data.date.toString()
        }


        details["event.throwableText"] = event.throwableText ?: ""


        if (event.data is LogMessage) {
            val data = event.data as LogMessage
            val attachments = data.includedAttachments

            attachments.forEach {
                details["event.attachment.${it.name}.displayText"] = it.displayText
            }
        }

        ActivityMonitor.getInstance(project).registerFatalError(details)
    }

    private fun getExceptionMessage(event: IdeaLoggingEvent): String {
        val data = event.data
        return if (data is LogMessage) {
            data.throwable.message ?: ""
        } else {
            event.throwable.message ?: ""
        }
    }

    private fun getExceptionClassName(event: IdeaLoggingEvent): String {
        val data = event.data
        return if (data is LogMessage) {
            data.throwable.javaClass.name
        } else {
            event.throwable.javaClass.name
        }
    }
}