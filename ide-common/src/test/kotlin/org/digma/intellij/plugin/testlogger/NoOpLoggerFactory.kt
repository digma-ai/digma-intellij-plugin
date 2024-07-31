package org.digma.intellij.plugin.testlogger

import com.intellij.openapi.diagnostic.DefaultLogger
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.Logger.Factory

//intellij DefaultLogger is selected in unit tests which prints to system err. it pollutes the gradle console,
// can't get rid of that , the only way is to replace the logger factory in tests that may log too many messages.
class NoOpLoggerFactory : Factory {
    override fun getLoggerInstance(category: String): Logger {
        return NoOpLogger(category)
    }
}

//log only info and error, warn doesn't help us in tests and pollutes the console
class NoOpLogger(category: String) : DefaultLogger(category) {
    override fun debug(t: Throwable?) {
        super.debug(t)
    }

    override fun debug(message: String?, t: Throwable?) {

    }

    override fun info(message: String?) {
        super.info(message)
    }

    override fun info(message: String?, t: Throwable?) {
        super.info(message, t)
    }

    override fun warn(message: String?, t: Throwable?) {

    }

    override fun error(message: String?, t: Throwable?, vararg details: String?) {
        super.error(message, t, *details)
    }
}
