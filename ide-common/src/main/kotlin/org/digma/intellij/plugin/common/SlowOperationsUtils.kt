package org.digma.intellij.plugin.common

import com.google.common.base.Supplier
import com.intellij.openapi.application.AccessToken
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.SlowOperations


fun <T> allowSlowOperation(task: Supplier<T>): T {

    try {
        val startSectionMethod = SlowOperations::class.java.getMethod("startSection", String::class.java)

        val accessToken: AccessToken = startSectionMethod.invoke(null, SlowOperations.ACTION_PERFORM) as AccessToken

        try {

            return task.get()

        } finally {
            accessToken.close()
        }


    } catch (e: ReflectiveOperationException) {

        return SlowOperations.allowSlowOperations(ThrowableComputable {
            return@ThrowableComputable task.get()
        })

    }


}