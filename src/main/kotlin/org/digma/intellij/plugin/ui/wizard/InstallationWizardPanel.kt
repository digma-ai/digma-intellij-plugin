package org.digma.intellij.plugin.ui.wizard

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.LocalInstallationFacade
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.reload.ReloadService
import org.digma.intellij.plugin.reload.ReloadableJCefContainer
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.ui.jcef.JCEF_WIZARD_SKIP_INSTALLATION_STEP_PROPERTY_NAME
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.updateDigmaEngineStatus
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.Insets
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.time.Duration.Companion.seconds

class InstallationWizardPanel(private val project: Project, private val wizardSkipInstallationStep: Boolean) : DisposablePanel(),
    ReloadableJCefContainer {


    private var jCefComponent: JCefComponent? = null

    private val digmaStatusUpdater = DigmaStatusUpdater()

    private var parentDisposable = Disposer.newDisposable()

    init {
        jCefComponent = build()

        Disposer.register(InstallationWizardService.getInstance(project)) {
            dispose()
        }

        jCefComponent?.let {
            InstallationWizardService.getInstance(project).setJcefBrowser(it.jbCefBrowser)
        }

        //parent disposable for ReloadService should be the same lifetime as the jCefComponent because InstallationWizardPanel does not live
        // for the lifetime of the project, it is closed by user.
        //parent disposable for ReloadObserver needs to be a disposable that is the lifetime of the jCefComponent.
        service<ReloadService>().register(this, parentDisposable)
        jCefComponent?.registerForReloadObserver(INSTALLATION_WIZARD_APP_NAME)
    }


    private fun build(): JCefComponent? {

        val jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        return jCefComponent
    }

    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponent.JCefComponentBuilder(
                project, INSTALLATION_WIZARD_APP_NAME, parentDisposable,
                INSTALLATION_WIZARD_URL,
                InstallationWizardMessageRouterHandler(project, digmaStatusUpdater),
                InstallationWizardSchemeHandlerFactory())
                .withArg(JCEF_WIZARD_SKIP_INSTALLATION_STEP_PROPERTY_NAME, wizardSkipInstallationStep)
                .build()
        } else {
            null
        }
    }


    override fun reload() {
        dispose()
        removeAll()
        parentDisposable = Disposer.newDisposable()
        jCefComponent = build()
        jCefComponent?.registerForReloadObserver(INSTALLATION_WIZARD_APP_NAME)
    }

    override fun getProject(): Project {
        return project
    }


    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }

    override fun dispose() {
        digmaStatusUpdater.stop()
        Disposer.dispose(parentDisposable)
        jCefComponent?.let {
            Disposer.dispose(it)
        }
    }


    class DigmaStatusUpdater {

        private val logger: Logger = Logger.getInstance(this::class.java)

        private var myDisposable: Disposable? = null

        private var digmaInstallationStatus = AtomicReference<DigmaInstallationStatus?>(null)

        fun start(project: Project, browser: CefBrowser) {

            Log.log(logger::trace, project, "starting DigmaStatusUpdater")

            myDisposable?.let {
                Disposer.dispose(it)
            }

            myDisposable = Disposer.newDisposable()
            digmaInstallationStatus.set(null)

            myDisposable?.let {
                it.disposingPeriodicTask("InstallationWizard.DigmaStatusUpdater", 2.seconds.inWholeMilliseconds, false) {
                    try {
                        val currentStatus = service<LocalInstallationFacade>().getDigmaInstallationStatus(project)

                        //DigmaInstallationStatus is data class so we can rely on equals
                        if (digmaInstallationStatus.get() == null || currentStatus != digmaInstallationStatus.get()) {
                            Log.log(logger::trace, project, "status changed current:{}, previous:{}", currentStatus, digmaInstallationStatus)
                            digmaInstallationStatus.set(currentStatus)
                            Log.log(logger::trace, project, "updating wizard with digmaInstallationStatus {}", digmaInstallationStatus)
                            digmaInstallationStatus.get()?.let { status ->
                                updateDigmaEngineStatus(browser, status)
                            }
                        }

                        val isLocalEngineInstalled = LocalInstallationFacade.getInstance().isLocalEngineInstalled()
                        val isLocalEngineRunning = LocalInstallationFacade.getInstance().isLocalEngineRunning(project)
                        Log.log(
                            logger::trace,
                            project,
                            "updating wizard with isLocalEngineInstalled={},isLocalEngineRunning={}",
                            isLocalEngineInstalled,
                            isLocalEngineRunning
                        )
                        sendIsDigmaEngineInstalled(isLocalEngineInstalled, browser)
                        sendIsDigmaEngineRunning(isLocalEngineRunning, browser)


                    } catch (e: Exception) {
                        Log.warnWithException(logger, project, e, "error in DigmaStatusUpdater {}", e)
                        ErrorReporter.getInstance().reportError(project, "DigmaStatusUpdater.loop", e)
                    }
                }
            }
        }

        fun stop() {
            Log.log(logger::trace, "stopping DigmaStatusUpdater")
            myDisposable?.let {
                Disposer.dispose(it)
            }
        }
    }
}