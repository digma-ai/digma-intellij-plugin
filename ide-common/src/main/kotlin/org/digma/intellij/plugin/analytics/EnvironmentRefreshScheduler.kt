package org.digma.intellij.plugin.analytics

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.settings.SettingsState


@Suppress("UnstableApiUsage")
fun scheduleEnvironmentRefresh(parentDisposable: Disposable, environemnt: Environment) {

    DisposingScope(parentDisposable).launch {
        //start refreshing after one minute,let the IDE full start
        delay(60000)
        while (this.isActive) {
            delay(service<SettingsState>().refreshDelay.toLong() * 1000)
            environemnt.refreshNowOnBackground()
        }
    }
}