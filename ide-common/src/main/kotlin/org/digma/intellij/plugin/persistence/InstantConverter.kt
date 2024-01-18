package org.digma.intellij.plugin.persistence

import com.intellij.util.xmlb.Converter
import org.digma.intellij.plugin.common.DatesUtils
import java.time.Instant

internal class InstantConverter : Converter<Instant>() {

    override fun toString(value: Instant): String {
        return DatesUtils.Instants.instantToString(value)
    }

    override fun fromString(value: String): Instant {
        return DatesUtils.Instants.stringToInstant(value)
    }
}