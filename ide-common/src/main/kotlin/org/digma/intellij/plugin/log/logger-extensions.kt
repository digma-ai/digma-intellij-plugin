package org.digma.intellij.plugin.log

import com.intellij.openapi.diagnostic.Logger

inline fun Logger.debug(lazyMessage: () -> String) {
    if (isDebugEnabled) {
        debug(lazyMessage())
    }
}

inline fun Logger.debug(lazyMessage: () -> String, lazyThrowable: () -> Throwable) {
    if (isDebugEnabled) {
        debug(lazyMessage(), lazyThrowable())
    }
}


inline fun Logger.trace(lazyMessage: () -> String) {
    if (isTraceEnabled) {
        trace(lazyMessage())
    }
}