package org.digma.intellij.plugin.rdparties

import org.ocpsoft.prettytime.PrettyTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PrettyTimeTest {

    @Test
    fun verifyPrettyNameOnMinutes() {
        val nowMinus3Minutes = Calendar.getInstance()
        nowMinus3Minutes.add(Calendar.MINUTE, -3)

        val ptNow = PrettyTime()

        val actualStr = ptNow.format(nowMinus3Minutes.time)
        assertEquals("3 minutes ago", actualStr)
    }

    @Test
    fun verifyPrettyNameOnHours() {
        val nowMinus4Hours = Calendar.getInstance()
        nowMinus4Hours.add(Calendar.HOUR, -4)

        val ptNow = PrettyTime()

        val actualStr = ptNow.format(nowMinus4Hours.time)
        assertEquals("4 hours ago", actualStr)
    }
}