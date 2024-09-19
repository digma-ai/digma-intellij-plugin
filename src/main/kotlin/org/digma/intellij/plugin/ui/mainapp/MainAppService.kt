package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import org.digma.intellij.plugin.engagement.EngagementScoreService
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.sendGenericPluginEvent
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.PROJECT)
class MainAppService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    init {

        if ( //todo: check if user clicked don't show again
            !PersistenceService.getInstance().isUserRequestedEarlyAccess() &&
            PersistenceService.getInstance().getUserEmail() == null && PersistenceService.getInstance().getUserRegistrationEmail() == null
        ) {
            val disposable = Disposer.newDisposable()
            Disposer.register(this, disposable)
            disposable.disposingPeriodicTask("EarlyAccessNotificationTimer", 1.minutes.inWholeMilliseconds, 1.hours.inWholeMilliseconds, false) {
                jCefComponent?.let { jcefComp ->
                    val installTime = PersistenceService.getInstance().getFirstTimePluginLoadedTimestamp()
                    if (installTime != null) {
                        if (Clock.System.now() > (installTime.toKotlinInstant().plus(14.days))) {
                            if (EngagementScoreService.getInstance().getLatestRegisteredActiveDays() >= 5){
                                sendGenericPluginEvent(project,jcefComp.jbCefBrowser.cefBrowser,"SHOW_EARLY_ACCESS_PROMOTION")
                                Disposer.dispose(disposable)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MainAppService {
            return project.service<MainAppService>()
        }
    }


    override fun dispose() {
        this.jCefComponent = null
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

}