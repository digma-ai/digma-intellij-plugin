package org.digma.intellij.plugin.test.system.framework

import junit.framework.TestCase

object DigmaAssertion {

    @Volatile
    var flag: Throwable? = null
    fun assertTrue(condition: Boolean) {
        try {
            TestCase.assertTrue(condition)
        } catch (ex: AssertionError) {
            flag = ex
        }
    }

    fun <T> assertEquals(expected: T, actual: T) {
        try {
            TestCase.assertEquals(expected, actual)
        } catch (ex: AssertionError) {
            flag = ex
        }
    }
    
    fun assertFlag(){
        if(flag != null){
            throw flag!!
        }
    }
    
}