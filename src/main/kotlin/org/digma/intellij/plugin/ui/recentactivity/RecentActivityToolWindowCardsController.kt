package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionEvent
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.updates.AggressiveUpdateService
import org.digma.intellij.plugin.updates.AggressiveUpdateStateChangedEvent
import org.digma.intellij.plugin.updates.CurrentUpdateState
import org.digma.intellij.plugin.updates.PublicUpdateState
import java.awt.CardLayout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class RecentActivityToolWindowCardsController(private val project: Project) {

    val logger: Logger = Logger.getInstance(RecentActivityToolWindowCardsController::class.java)

    enum class RecentActivityWindowCard {
        MAIN, NO_CONNECTION, UPDATE_MODE
    }

    //the main card panel, our main view and no-connection panel
    private var cardsPanel: JPanel? = null

    //never use latestCalledCard , only in initComponents
    private var latestCalledCard: RecentActivityWindowCard? = null

    private val isConnectionLost = AtomicBoolean(false)


    companion object {
        fun getInstance(project: Project): RecentActivityToolWindowCardsController {
            return project.service<RecentActivityToolWindowCardsController>()
        }
    }

    init {
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(BackendConnectionEvent.BACKEND_CONNECTION_STATE_TOPIC, object : BackendConnectionEvent {
                override fun connectionLost() {
                    Log.log(logger::debug, "Got connectionLost")
                    isConnectionLost.set(true)
                    showNoConnection()
                }

                override fun connectionGained() {
                    Log.log(logger::debug, "Got connectionGained")
                    isConnectionLost.set(false)
                    showMainPanel()
                }
            })


        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(AggressiveUpdateStateChangedEvent.UPDATE_STATE_CHANGED_TOPIC, object : AggressiveUpdateStateChangedEvent {
                override fun stateChanged(updateState: PublicUpdateState) {
                    updateStateChanged(updateState)
                }
            })

    }


    //this method must be called with valid non-null components on startup.
    // if any of these is null this controller will fail.
    // can't send those to the constructor because it's a plugin service.
    fun initComponents(mainCardsPanel: JPanel) {
        Log.log(logger::debug, "initComponents called")

        Log.log(logger::debug, "got cardPanel {}", mainCardsPanel)
        this.cardsPanel = mainCardsPanel

        if (isConnectionLost.get() || BackendConnectionMonitor.getInstance(project).isConnectionError()) {
            showNoConnection()
        }

        //it may be that some showXXX is called before the components are initialized and
        // cardPanel is still null. one example is showUpdateBackendPanel that may be called very early.
        // so keep the latest called card and show it here after all components are initialized.
        if (latestCalledCard != null) {
            showCard(latestCalledCard!!)
        }
    }


    fun updateStateChanged(updateState: PublicUpdateState) {
        if (updateState.updateState == CurrentUpdateState.OK) {
            closeUpdateBackendPanel()
        } else {
            showUpdateBackendPanel()
        }
    }


    fun showMainPanel() {
        Log.log(logger::debug, "showMainPanel called")

        if (isConnectionLost.get()) {
            Log.log(logger::debug, "Not showing MainPanel because connection lost, showing NoConnection")
            showNoConnection()
        } else {

            EDT.ensureEDT { showCard(RecentActivityWindowCard.MAIN) }
        }
    }

    private fun showUpdateBackendPanel() {
        Log.log(logger::debug, "showUpdateBackendPanel called")

        if (isConnectionLost.get()) {
            Log.log(logger::debug, "Not showing MainPanel because connection lost, showing NoConnection")
            showNoConnection()
        } else {
            EDT.ensureEDT { showCard(RecentActivityWindowCard.UPDATE_MODE) }
        }
    }

    private fun closeUpdateBackendPanel() {
        Log.log(logger::debug, "closeUpdateBackendPanel called")

        //this may happen on startup,showMainPanel is called from the tool window factory,
        // but there may be a connection lost before the content was built and before this controller was initialized
        if (isConnectionLost.get()) {
            Log.log(logger::debug, "Not showing MainPanel because connection lost, showing NoConnection")
            showNoConnection()
        } else {
            EDT.ensureEDT { showCard(RecentActivityWindowCard.MAIN) }
        }
    }


    private fun showNoConnection() {
        Log.log(logger::debug, "showNoConnection called")

        showCard(RecentActivityWindowCard.NO_CONNECTION)
    }


    private fun showCard(card: RecentActivityWindowCard) {
        Log.log(logger::debug, "showCard called with {}", card)


        //need to keep the UPDATE_MODE if AggressiveUpdateService is still in update mode.
        // after AggressiveUpdateService enters update mode there may be connection lost, the connectionLost
        // will change to NO_CONNECTION, in that case we want to see the no connection message.
        // on connectionGained the listener will try to change it to MAIN but if
        // AggressiveUpdateService is still in update mode we need to replace back to UPDATE_MODE
        val cardToUse = if (AggressiveUpdateService.getInstance().isInUpdateMode() && card == RecentActivityWindowCard.MAIN) {
            RecentActivityWindowCard.UPDATE_MODE
        } else {
            card
        }


        latestCalledCard = card
        if (cardsPanel == null) {
            Log.log(logger::debug, project, "show {} was called but cardsPanel is null", card)
        } else {
            Log.log(logger::debug, project, "Showing card {}", card)
            EDT.ensureEDT {
                (cardsPanel?.layout as CardLayout?)?.show(cardsPanel, cardToUse.name)
            }
        }
    }
}