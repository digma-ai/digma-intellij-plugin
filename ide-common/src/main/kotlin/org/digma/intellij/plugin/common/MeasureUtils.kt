package org.digma.intellij.plugin.common

inline fun <T> measureTimeMillisWithResult(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - start
    return result to duration
}