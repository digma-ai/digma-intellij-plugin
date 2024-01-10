package org.digma.intellij.plugin.common

import com.intellij.util.Alarm
import org.digma.intellij.plugin.errorreporting.ErrorReporter


fun addRequestWithErrorReporting(alarm: Alarm, task: Runnable, delayMillis: Int, messageOnError: String) {
    alarm.addRequest({
        try {
            task.run()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(messageOnError, e)
        }
    }, delayMillis)
}