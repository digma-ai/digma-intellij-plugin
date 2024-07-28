package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import kotlin.time.Duration.Companion.seconds


fun scheduleEnvironmentRefresh(parentDisposable: Disposable, environment: Environment) {
    parentDisposable.disposingPeriodicTask("EnvironmentRefresh", 10.seconds.inWholeMilliseconds, true) {
        environment.refreshNowOnBackground()
    }
}