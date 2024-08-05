
package org.digma.intellij.plugin.scheduling

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.testlogger.NoOpLoggerFactory
import org.junit.jupiter.api.assertThrows
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.timer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class SchedulingTests {

    //enable running this test in a non intellij test.
    //ErrorReporter.pause() will cause ErrorReporter.getInstance return a no-op proxy instead of a plugin service
    @BeforeTest
    fun pauseErrorReporter() {
        Logger.setFactory(NoOpLoggerFactory())
        ErrorReporter.pause()
    }


    @Test
    fun testDisposingTask() {

        //this test should execute the task 3 times and then dispose it.

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingPeriodicTask("testDisposingTask", 100, false) {
            testList.add("test")
        }

        Thread.sleep(250)
        Disposer.dispose(disposable)
        //sleep some more to make sure the task was disposed and not executing anymore
        Thread.sleep(500)

        assertEquals(3, testList.size)
    }


    @Test
    fun testDisposingOneShotDelayedTask() {

        //this test should execute once after the given delay.

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingOneShotDelayedTask("testDisposingOneShotDelayedTask", 200) {
            testList.add("test")
        }

        Thread.sleep(300)
        Disposer.dispose(disposable)
        //sleep some more to make sure the task was disposed and not executing anymore
        Thread.sleep(500)

        assertEquals(1, testList.size)
    }


    @Test
    fun testDisposingOneShotDelayedTaskCanceled() {

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingOneShotDelayedTask("testDisposingOneShotDelayedTaskCanceled", 200) {
            testList.add("test")
        }

        //dispose the task immediately, it should not run
        Disposer.dispose(disposable)
        //sleep some more to make sure the task was disposed and not executing anymore
        Thread.sleep(500)

        assertEquals(0, testList.size)
    }


    @Test
    fun testDisposingOneShotDelayedTaskCanceledParentDisposableHasNoChildren() {

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingOneShotDelayedTask("testDisposingOneShotDelayedTaskCanceledParentDisposableHasNoChildren", 300) {
            testList.add("test")
        }

        //dispose the task , it should not run
        Disposer.dispose(disposable)
        //sleep some more to make sure the task was disposed and not executing anymore
        Thread.sleep(500)

        //the disposable is disposed and should have no children
        @Suppress("UnstableApiUsage")
        Disposer.disposeChildren(disposable) {
            fail("this parent disposable should have no children")
        }

        assertEquals(0, testList.size)
    }

    @Test
    fun testDisposingOneShotDelayedTaskParentDisposableHasNoChildren() {

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingOneShotDelayedTask("testDisposingOneShotDelayedTaskParentDisposableHasNoChildren", 200) {
            testList.add("test")
        }

        Thread.sleep(500)

        //the task will run. but not disposing the disposable, it should have no children because the task
        // itself removed its children when it finished executing
        @Suppress("UnstableApiUsage")
        Disposer.disposeChildren(disposable) {
            fail("this parent disposable should have no children")
        }

        assertEquals(1, testList.size)
    }


    @Test
    fun testTaskCancelsItself() {

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingPeriodicTask("testTaskCancelsItself", 100, false) {
            testList.add("test")
            if (testList.size == 2) {
                Disposer.dispose(disposable)
            }
        }

        //the task will run twice and cancel itself, wait here some more to make sure it's not running anymore
        Thread.sleep(600)

        assertEquals(2, testList.size)
    }





    @Test
    fun testOneShotTask() {
        val future = oneShotTask("testOneShotTask") {
            "test"
        }

        val test = future?.get()
        assertEquals("test", test)
    }

    @Test
    fun testOneShotTaskWithTimeout() {
        val result = blockingOneShotTask("testOneShotTaskWithTimeout", 100) {
            println("test")
        }

        assertTrue(result)
    }


    @Test
    fun testOneShotTaskWithResult() {
        val result = blockingOneShotTaskWithResult("testOneShotTaskWithResult", 100) {
            "test"
        }

        assertEquals("test", result)

    }


    @Test
    fun testOneShotTaskCanceledOnTimeout() {
        val result = blockingOneShotTask("testOneShotTaskCanceledOnTimeout", 500) {
            assertThrows<InterruptedException> {
                Thread.sleep(2000)
            }
        }
        assertFalse(result)
    }


    @Test
    fun testOneShotTaskWithResultCanceledOnTimeout() {
        assertThrows<TimeoutException> {
            blockingOneShotTaskWithResult("testOneShotTaskWithResultCanceledOnTimeout", 100) {
                Thread.sleep(1000)
                @Suppress("UNUSED_EXPRESSION")
                "test"
            }
        }
    }

    @Test
    fun testOneShotTaskWithFutureCanceledOnTimeout() {
        val future = oneShotTask("testOneShotTaskWithFutureCanceledOnTimeout") {
            assertThrows<InterruptedException> {
                Thread.sleep(5000)
            }
            "test"
        }

        assertNotNull(future)
        assertThrows<TimeoutException> {
            try {
                future.get(100, TimeUnit.MILLISECONDS)
            } catch (e: Throwable) {
                future.cancel(true)
                throw e
            }

        }
    }


    @Test
    fun testThreadsAreDaemon() {

        val timer = timer("management", true, 0, 50) {
            try {
                manage(true)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        val daemons = Collections.synchronizedMap(mutableMapOf<String, Boolean>())
        val disposable = Disposer.newDisposable()
        repeat((0..1000).count()) {
            disposable.disposingPeriodicTask("testThreadsAreDaemon", 10, false) {
                try {
                    daemons[Thread.currentThread().name] = Thread.currentThread().isDaemon
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    //nothing to do
                }
            }
        }


        Thread.sleep(5000)
        Disposer.dispose(disposable)
        println("daemons: $daemons")
        assertEquals(SCHEDULER_MAX_SIZE, daemons.size)
        assertTrue(daemons.values.all { true })
        timer.cancel()
    }


    @Test
    fun testCorePoolSizeIncreased() {

        val timer = timer("management", true, 0, 50) {
            try {
                manage(true)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        val threadNames = Collections.synchronizedSet(mutableSetOf<String>())
        val disposable = Disposer.newDisposable()
        repeat((0..10000).count()) {
            disposable.disposingPeriodicTask("testCorePoolSizeIncreased", 10, false) {
                try {
                    threadNames.add(Thread.currentThread().name)
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    //nothing to do
                }
            }
        }


        Thread.sleep(10000)
        Disposer.dispose(disposable)
        println("thread names: $threadNames")
        assertEquals(SCHEDULER_MAX_SIZE, threadNames.size)
        timer.cancel()
    }


//for testing while developing
//    @Test
//    fun test() {
//
//        GlobalScope.launch {
//            while (isActive) {
//                try {
//                    delay(100)
//                    manage(true)
//                }catch (e:Throwable){
//                    e.printStackTrace()
//                }
//            }
//        }
//
//        val threadNames = Collections.synchronizedSet(mutableSetOf<String>())
//        val executedTasks = AtomicInteger()
//        val disposable = Disposer.newDisposable()
//        repeat((0..10000).count()) {
//            disposable.disposingPeriodicTask("test", 10) {
//                try {
//                    threadNames.add(Thread.currentThread().name)
//                    Thread.sleep(100)
//                    executedTasks.incrementAndGet()
//                } catch (e: InterruptedException) {
//                    //nothing to do
//                }
//            }
//        }
//
//
//        Thread.sleep(30000)
//        Disposer.dispose(disposable)
//        println("thread named: $threadNames")
//        println("executedTasks: ${executedTasks.get()}")
//        assertEquals(3, threadNames.size)
//        assertEquals(30, executedTasks.get())
//    }

}