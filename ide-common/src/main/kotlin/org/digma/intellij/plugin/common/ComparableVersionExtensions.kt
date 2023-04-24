package org.digma.intellij.plugin.common

import org.apache.maven.artifact.versioning.ComparableVersion

fun ComparableVersion.newerThan(other: ComparableVersion): Boolean {
    return this > other
}

fun ComparableVersion.olderThan(other: ComparableVersion): Boolean {
    return this < other
}