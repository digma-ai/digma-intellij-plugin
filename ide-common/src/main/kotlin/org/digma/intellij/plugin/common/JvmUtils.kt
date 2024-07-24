package org.digma.intellij.plugin.common

import java.io.PrintWriter
import java.io.StringWriter
import java.lang.management.LockInfo
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean

fun generateThreadDump(): String {
    return try {
        val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
        val threadInfos: Array<ThreadInfo> = threadMXBean.dumpAllThreads(true, true)
        generateThreadDump(threadInfos)
    } catch (e: Throwable) {
        val exceptionStackTrace = e.let {
            val stringWriter = StringWriter()
            it.printStackTrace(PrintWriter(stringWriter))
            stringWriter.toString()
        }
        "could not generate thread dump\n$exceptionStackTrace"
    }
}


fun generateMonitorDeadlockedThreadDump(): String {
    return try {
        val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
        val monitorDeadLockThreads = threadMXBean.findMonitorDeadlockedThreads()
        if (monitorDeadLockThreads != null) {
            val threadInfos: Array<ThreadInfo> = threadMXBean.getThreadInfo(monitorDeadLockThreads, true, true)
            generateThreadDump(threadInfos)
        } else {
            ""
        }
    } catch (e: Throwable) {
        val exceptionStackTrace = e.let {
            val stringWriter = StringWriter()
            it.printStackTrace(PrintWriter(stringWriter))
            stringWriter.toString()
        }
        "could not generate thread dump\n$exceptionStackTrace"
    }
}


fun generateDeadlockedThreadDump(): String {
    return try {
        val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
        val deadLockThreads = threadMXBean.findDeadlockedThreads()
        if (deadLockThreads != null) {
            val threadInfos: Array<ThreadInfo> = threadMXBean.getThreadInfo(deadLockThreads, true, true)
            generateThreadDump(threadInfos)
        } else {
            ""
        }
    } catch (e: Throwable) {
        val exceptionStackTrace = e.let {
            val stringWriter = StringWriter()
            it.printStackTrace(PrintWriter(stringWriter))
            stringWriter.toString()
        }
        "could not generate thread dump\n$exceptionStackTrace"
    }
}


fun generateThreadDump(threadInfos: Array<ThreadInfo>): String {
    val dump = StringBuilder()

    for (threadInfo in threadInfos) {
        dump.append("id:" + threadInfo.threadId)
        dump.append(": ")
        dump.append(threadInfoHeader(threadInfo))
        val stackTraceElements: Array<StackTraceElement> = threadInfo.stackTrace
        for ((index, stackTraceElement) in stackTraceElements.withIndex()) {
            dump.append("\n        at ")
            dump.append(stackTraceElement)
            if (index == 0) {
                val ts: Thread.State = threadInfo.threadState
                when (ts) {
                    Thread.State.BLOCKED -> {
                        dump.append("\t-  blocked on " + threadInfo.lockInfo)
                        dump.append('\n')
                    }

                    Thread.State.WAITING -> {
                        dump.append("\t-  waiting on " + threadInfo.lockInfo.className + "," + threadInfo.lockInfo.identityHashCode)
                        dump.append('\n')
                    }

                    Thread.State.TIMED_WAITING -> {
                        dump.append("\t-  waiting on " + threadInfo.lockInfo)
                        dump.append('\n')
                    }

                    else -> {}
                }
            }


            for (mi in threadInfo.lockedMonitors) {
                if (mi.lockedStackDepth == index) {
                    dump.append("\t-  locked $mi")
                    dump.append('\n')
                }
            }

        }

        val locks: Array<LockInfo> = threadInfo.lockedSynchronizers
        if (locks.isNotEmpty()) {
            dump.append("\n\tNumber of locked synchronizers = " + locks.size)
            dump.append('\n')
            for (li in locks) {
                dump.append("\t- $li")
                dump.append('\n')
            }
        }


        dump.append("\n\n")
    }
    return dump.toString()

}


private fun threadInfoHeader(threadInfo: ThreadInfo): String {
    val sb = java.lang.StringBuilder(
        "\"" + threadInfo.threadName + "\"" +
                (if (threadInfo.isDaemon) " daemon" else "") +
                " priority=" + threadInfo.priority +
                threadInfo.threadState
    )
    if (threadInfo.lockName != null) {
        sb.append(" on " + threadInfo.lockName)
    }
    if (threadInfo.lockOwnerName != null) {
        sb.append(
            (" owned by \"" + threadInfo.lockOwnerName +
                    "\" Id=" + threadInfo.lockOwnerId)
        )
    }
    if (threadInfo.isSuspended) {
        sb.append(" (suspended)")
    }
    if (threadInfo.isInNative) {
        sb.append(" (in native)")
    }
    sb.append('\n')

    return sb.toString()
}