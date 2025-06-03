package org.digma.intellij.plugin.idea.kotlin

import org.digma.intellij.plugin.idea.discovery.AbstractCodeObjectDiscovery


@Suppress("LightServiceMigrationCode")
class KotlinCodeObjectDiscovery : AbstractCodeObjectDiscovery(KotlinSpanDiscovery())