package org.digma.intellij.plugin.process

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.Alarm
import com.intellij.util.concurrency.FutureResult
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_MEDIUM_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service(Service.Level.PROJECT)
class ProcessManager(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    override fun dispose() {
        //nothing to do, used as parent disposable
    }


    /**
     * This method is meant to run a process that does PSI queries and can be cancelable.
     * these processes should never hang waiting and thus there is no support for timeouts.
     * Please don't use it for any operation, it is suitable for PSI queries only.
     */
    fun runTaskUnderProcess(
        task: Runnable,
        context: ProcessContext,
        reuseCurrentThread: Boolean,
        maxRetries: Int,
        cancelOnPsiModification: Boolean
    ): ProcessResult {

        //must not be under progress already
        if (reuseCurrentThread) {
            assertNotUnderProgress()
            ReadActions.assertNotInReadAccess()
        }

        val future = if (reuseCurrentThread) {
            FutureResult(runUnderProcess(task, context, maxRetries, cancelOnPsiModification))
        } else {
            Backgroundable.executeOnPooledThread(Callable {
                runUnderProcess(task, context, maxRetries, cancelOnPsiModification)
            })
        }

        return future.get()

    }


    private fun runUnderProcess(task: Runnable, context: ProcessContext, maxRetries: Int, cancelOnPsiModification: Boolean): ProcessResult {

        val myModificationTracker = if (cancelOnPsiModification) {
            MyPsiModificationTracker(context.processName, maxRetries)
        } else {
            null
        }

        try {
            return runUnderProcess(task, context, maxRetries, myModificationTracker)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(
                "ProcessManager.runUnderProcess", e, mapOf(
                    "process.name" to context.processName,
                    SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                )
            )
            return ProcessResult(success = false, canceled = false, duration = Duration.ZERO, error = e)
        } finally {
            myModificationTracker?.let {
                Log.log(logger::trace, "disposing myModificationTracker for process {}", context.processName)
                Disposer.dispose(it)
            }
        }
    }


    private fun runUnderProcess(
        task: Runnable,
        context: ProcessContext,
        maxRetries: Int,
        myModificationTracker: MyPsiModificationTracker?
    ): ProcessResult {

        Log.log(logger::trace, "starting runUnderProcess {}", context.processName)

        var delayBetweenRetries = 0L
        var success = false
        var canceled: Boolean
        var retry = 0
        var error: Throwable?


        val stopWatch = StopWatch.createStarted()

        do {

            canceled = false
            error = null


            try {

                Log.log(logger::trace, "starting process {}, retry {}", context.processName, retry)

                //need a new indicator for every new process
                val indicator = EmptyProgressIndicator()
                context.indicator = indicator

                myModificationTracker?.indicator = indicator
                myModificationTracker?.currentRetry = retry

                ProgressManager.getInstance().runProcess({
                    task.run()
                }, indicator)

                success = true

                Log.log(logger::trace, "process {} completed successfully after {} retries", context.processName, retry)

            } catch (
                @Suppress("IncorrectProcessCanceledExceptionHandling")
                e: ProcessCanceledException,
            ) {
                Log.log(logger::trace, "process canceled {}", context.processName)
                canceled = true
                logPCE(e, context)
                error = e
            } catch (e: Throwable) {
                Log.log(logger::trace, "process {} failed {}", context.processName, e)
                logError(e, context)
                error = e
            }

            retry++
            val continueRetry = !success && retry < maxRetries && isProjectValid(project)

            if (continueRetry) {
                try {

                    if (canceled) {
                        //if blackout started wait for it to complete before the next retry
                        while (myModificationTracker?.inBlackout == true) {
                            Thread.sleep(100)
                        }
                    } else {
                        delayBetweenRetries += 5000L
                        Thread.sleep(delayBetweenRetries)
                    }

                } catch (_: InterruptedException) {
                }
            }

        } while (continueRetry)


        stopWatch.stop()
        return ProcessResult(success, canceled, stopWatch.getTime(TimeUnit.MILLISECONDS).toDuration(DurationUnit.MILLISECONDS), error)

    }

    private fun logPCE(e: ProcessCanceledException, context: ProcessContext) {
        ErrorReporter.getInstance().reportError(
            "${context.processName}.onPCE", e, mapOf(
                SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
            )
        )
    }

    private fun logError(e: Throwable, context: ProcessContext) {
        ErrorReporter.getInstance().reportError(
            "${context.processName}.onError", e, mapOf(
                SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
            )
        )
    }


    private inner class MyPsiModificationTracker(processName: String, maxRetries: Int) : DisposableAdaptor {

        val myAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
        var indicator: ProgressIndicator? = null
        var inBlackout = false
        var currentRetry: Int = 0

        init {
            project.messageBus.connect(this).subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {

                //don't cancel on last retry, let the process finish
                if (currentRetry + 1 == maxRetries) {
                    Log.log(logger::trace, "got psi modification, but last retry , not canceling, process {}", processName)
                    return@Listener
                }

                Log.log(logger::trace, "got psi modification, canceling process and starting blackout, process {}", processName)
                indicator?.cancel()
                inBlackout = true
                myAlarm.cancelAllRequests()
                myAlarm.addRequest({
                    Log.log(logger::trace, "ending blackout for process {}", processName)
                    inBlackout = false
                }, 5000)
            })
        }
    }

}