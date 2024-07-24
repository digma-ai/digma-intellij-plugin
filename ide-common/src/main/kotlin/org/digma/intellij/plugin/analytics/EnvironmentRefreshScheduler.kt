package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.settings.SettingsState


fun scheduleEnvironmentRefresh(parentDisposable: Disposable, environment: Environment) {
    val period = service<SettingsState>().refreshDelay.toLong() * 1000
    parentDisposable.disposingPeriodicTask("EnvironmentRefresh", period, true) {
        environment.refreshNowOnBackground()
    }
}