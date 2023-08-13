package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.diagnostic.Logger
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.digma.intellij.plugin.log.Log

class WaitFinishRule : TestRule {

    private val logger = Logger.getInstance(WaitFinishRule::class.java)

    private var latch: CountDownLatch = CountDownLatch(1)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (description.getAnnotation(WaitForAsync::class.java) != null)
                    latch = CountDownLatch(1)
                try {
                    base.evaluate()
                } finally {
                    waitForCompletion()
                }
            }
        }
    }

    fun signalComplete() {
        latch.countDown()
    }

    //using this method to wait for async operations to complete, e.g.: in tearDown()
    fun waitForCompletion() {
        if (latch.await(10, TimeUnit.SECONDS))
            Log.test(logger, "WaitForAsync completed")
        else
            Log.test(logger, "WaitForAsync timeout")
    }
}