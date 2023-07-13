package org.digma.intellij.plugin.recentactivity

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date


class RecentActivityLogic {

    companion object {
        const val RECENT_EXPIRATION_LIMIT_MILLIS: Long = 10 * 60 * 1000 // 10min

        @JvmStatic
        fun isRecentTime(date: Date?): Boolean {
            if (date == null) return false
            return date.toInstant().plus(RECENT_EXPIRATION_LIMIT_MILLIS, ChronoUnit.MILLIS).isAfter(Instant.now())
        }
    }
}