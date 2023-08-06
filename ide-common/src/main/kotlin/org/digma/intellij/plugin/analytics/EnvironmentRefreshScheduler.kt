package org.digma.intellij.plugin.analytics

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState


@Suppress("UnstableApiUsage")
fun scheduleEnvironmentRefresh(parentDisposable: Disposable, environemnt: Environment) {
    val logger = Logger.getInstance("scheduleEnvironmentRefresh");
    Log.test(logger, "launched analyticsService.refreshNowOnBackground() loop")
    DisposingScope(parentDisposable).launch {
        while (this.isActive) {
            delay(service<SettingsState>().refreshDelay.toLong() * 1000)
            Log.test(logger, "call analyticsService.refreshNowOnBackground() ")
            environemnt.refreshNowOnBackground()
        }
    }
}