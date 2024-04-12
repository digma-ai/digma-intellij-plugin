package org.digma.intellij.plugin.updates

import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.util.concurrency.FutureResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


/**
 * This method is called just before showing the update button in update suggestion or the aggressive
 * update button. it is meant to refresh the updatable plugins in case intellij didn't refresh for some
 * time or user disabled refresh.
 * the refresh takes a few seconds and by the time user clicks the button the list in Settings -> Plugins
 * should be refreshed already.
 * the method returns a Future that doesn't throw exception from get. callers should not rely on the real
 * completion of the refresh and should do their thing even if not completed.
 * this method may be called on any thread, UpdateChecker.updateAndShowResult runs a background thread
 * and ends with calling invokeLater.
 */
/*
 * implementation note: initially we wanted to let user click the button and only then call
 * UpdateChecker.updateAndShowResult and let user know of the refresh and wait until its finished
 * using some modal dialog, for example calling intellij modal task will show a modal progress bar.
 * but, it is impossible to call this method while a modal dialog or progress is showing.
 * a modal dialog blocks execution of the event queue, no swing event is processed while a modal dialog
 * is visible. but, the method UpdateChecker.updateAndShowResult wants to invokeLater
 * after downloading the plugins metadata, so it can never finish while there is a modal dialog visible.
 * it's possible to show a non-modal popup, but with non-modal popup user can click somewhere in the IDE and the
 * popup will close and may be a weird experience to pop up the plugin settings after few seconds.
 *
 * So the solution is to call UpdateChecker.updateAndShowResult before showing the update button.
 * showing the update button is not urgent and can wait a few seconds before we show it. that way when
 * user clicks the update button, and we open the plugin settings the plugins list will be refreshed.
 */
fun refreshPluginsMetadata(): Future<Boolean> {

    //a future that doesn't throw timeout exception and returns true on timeout.
    //callers just need to do something when UpdateChecker.updateAndShowResult is finished but should not rely
    // on success of the call. they should still do their thing even if the call didn't complete
    val future = object : FutureResult<Boolean>() {
        override fun get(timeout: Long, unit: TimeUnit): Boolean {
            return try {
                super.get(timeout, unit)
            } catch (e: Throwable) {
                true
            }
        }

        override fun get(): Boolean {
            return get(0, TimeUnit.SECONDS)
        }
    }

    return try {
        val actionCallback = UpdateChecker.updateAndShowResult()
        actionCallback.doWhenDone {
            future.set(true)
        }
        future
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("UpdatesUtilsKt.refreshPluginsMetadata", e)
        future.set(true)
        future
    }
}