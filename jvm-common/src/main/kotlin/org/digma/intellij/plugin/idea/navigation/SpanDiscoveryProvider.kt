package org.digma.intellij.plugin.idea.navigation

import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.navigation.model.SpanLocation

interface SpanDiscoveryProvider {

    fun discover(context: NavigationProcessContext): Map<String, SpanLocation>

    fun getName(): String

}