package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.instrumentation.MethodInstrumentationPresenter
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.time.Instant
import javax.swing.JLabel

class OtelDependencyButton(text: String, val project: Project, val theModel: MethodInstrumentationPresenter) :
    ActionLink(text, null) {

    private val clickAlarm: Alarm

    init {
        autoHideOnDisable = false
        setText(asHtml(buildLinkTextWithUnderLine(text)))

        clickAlarm = AlarmFactory.getInstance().create()
    }

    companion object {
        const val MAX_SECONDS_WAIT_FOR_DEPENDENCY: Long = 6
        const val WAIT_FOR_DEPENDENCY_INTERVAL_MILLIS: Long = 250
    }

    fun defineTheAction(panelToReset: DigmaResettablePanel?, labelOfWorking: JLabel) {

        val actionDef: () -> Unit = {
            this.isEnabled = false
            labelOfWorking.isVisible = true

            WriteAction.run<Exception> {
                theModel.addDependencyToOtelLibAndRefresh()
            }

            Backgroundable.executeOnPooledThread {
                waitForOtelDependencyToBeAvailable()

                EDT.ensureEDT {
                    this.isEnabled = true
                    labelOfWorking.isVisible = false

                    panelToReset?.reset()
                }
            }
        }

        addActionListener {
            clickAlarm.cancelAllRequests()
            clickAlarm.addRequest(actionDef, 250)
        }
    }

    // be careful, this method has sleeps
    private fun waitForOtelDependencyToBeAvailable() {
        val startPollingTimeSeconds = Instant.now().epochSecond
        var canInstrument = theModel.canInstrumentMethod
        while (!canInstrument) {
            val nowTimeSeconds = Instant.now().epochSecond
            if (nowTimeSeconds >= startPollingTimeSeconds + MAX_SECONDS_WAIT_FOR_DEPENDENCY) {
                break
            }
            Thread.sleep(WAIT_FOR_DEPENDENCY_INTERVAL_MILLIS)

            theModel.update(theModel.selectedMethodId)

            canInstrument = theModel.canInstrumentMethod
        }
    }

}