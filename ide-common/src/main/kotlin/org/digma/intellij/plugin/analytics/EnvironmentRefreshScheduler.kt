package org.digma.intellij.plugin.analytics

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.settings.SettingsState


@Suppress("UnstableApiUsage")
fun scheduleEnvironmentRefresh(parentDisposable: Disposable, environment: Environment) {

    parentDisposable.disposingScope().launch {
        while (this.isActive) {
            environment.refreshNowOnBackground()
            delay(service<SettingsState>().refreshDelay.toLong() * 1000)
        }
    }
}