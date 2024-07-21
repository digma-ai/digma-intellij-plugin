package org.digma.intellij.plugin.common

import java.util.Arrays
import java.util.stream.Collectors

fun firstPart(str: String, maxLen: Int): String {
    if (maxLen >= str.length) {
        return str
    }
    return str.substring(0, maxLen)
}

fun evalBoolean(str: String): Boolean {
    val cleanVal = str.trim().lowercase()

    return when (cleanVal) {
        "true" -> true
        "yes" -> true
        else -> false
    }
}

fun argsToString(args: Array<Any?>?): String {
    return try {
        if (args.isNullOrEmpty()) "" else Arrays.stream(args).map { obj: Any? -> obj.toString() }
            .collect(Collectors.joining(","))
    } catch (e: Exception) {
        "Error parsing args " + e.message
    }
}
