package org.digma.intellij.plugin.scheduling

import com.intellij.openapi.util.Disposer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulingTests {

    @Test
    fun testDisposingTask() {

        val testList = mutableListOf<String>()
        val disposable = Disposer.newDisposable()
        disposable.disposingPeriodicTask("test", 100) {
            testList.add("test")
        }

        Thread.sleep(250)
        Disposer.dispose(disposable)
        Thread.sleep(300)

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


//    @Test
//    fun test(){
//
//        val disposable = Disposer.newDisposable()
//        (0..1000).forEach {
//            disposable.disposingPeriodicTask("test", 100) {
//                println("$it")
//            }
//        }
//
//
//        Thread.sleep(1000000)
//    }

}