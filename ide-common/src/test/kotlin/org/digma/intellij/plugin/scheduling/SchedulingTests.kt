@file:OptIn(DelicateCoroutinesApi::class)

package org.digma.intellij.plugin.scheduling

import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulingTests {

    @Test
    fun testDisposingTask() {

        //this test should execute the task 3 times and then dispose it.

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingPeriodicTask("test", 100) {
            testList.add("test")
        }

        Thread.sleep(250)
        Disposer.dispose(disposable)
        //sleep more to make sure the task was disposed
        Thread.sleep(500)

        assertEquals(3, testList.size)
    }


    @Test
    fun testOneShotTask() {
        val future = oneShotTask("test") {
            "test"
        }

        val test = future?.get()
        assertEquals("test", test)
    }

    @Test
    fun testOneShotTaskWithTimeout() {
        val result = oneShotTask("test", 100) {
            println("test")
        }

        assertTrue(result)
    }

    //can't use ErrorReporter in  unit test, need to run an intellij test
//    @Test
//    fun testOneShotTaskWithTimeoutCanceled(){
//        val result = oneShotTask("test",100){
//            Thread.sleep(200)
//        }
//
//        assertFalse(result)
//
//    }

    @Test
    fun testOneShotTaskWithResult() {
        val result = oneShotTaskWithResult("test", 100) {
            "test"
        }

        assertEquals("test", result)

    }


    @Test
    fun testThreadsAreDaemon() {

        GlobalScope.launch {
            while (isActive) {
                try {
                    delay(50)
                    manage()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        val daemons = Collections.synchronizedMap(mutableMapOf<String, Boolean>())
        val disposable = Disposer.newDisposable()
        repeat((0..1000).count()) {
            disposable.disposingPeriodicTask("test", 10) {
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
    }


    @Test
    fun testCorePoolSizeIncreased() {

        GlobalScope.launch {
            while (isActive) {
                try {
                    delay(50)
                    manage()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        val threadNames = Collections.synchronizedSet(mutableSetOf<String>())
        val disposable = Disposer.newDisposable()
        repeat((0..1000).count()) {
            disposable.disposingPeriodicTask("test", 10) {
                try {
                    threadNames.add(Thread.currentThread().name)
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    //nothing to do
                }
            }
        }


        Thread.sleep(5000)
        Disposer.dispose(disposable)
        println("thread names: $threadNames")
        assertEquals(SCHEDULER_MAX_SIZE, threadNames.size)
    }


//for testing while developing
//    @Test
//    fun test() {
//
//        GlobalScope.launch {
//            while (isActive) {
//                try {
//                    delay(100)
//                    manage()
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