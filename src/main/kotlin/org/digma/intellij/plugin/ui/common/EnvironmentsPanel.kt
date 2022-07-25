package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.containers.stream
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsListChangedListener
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.util.*
import java.util.function.Function
import javax.swing.JPanel
import kotlin.math.min

fun environmentsPanel(project: Project, environmentsSupplier: EnvironmentsSupplier): JPanel {

    return EnvironmentsPanel(project, environmentsSupplier)
}


//need to remember we have two instances of this panel , one for the insights tab and one for the errors tab.
//both instances need to be in sync with the selected button and the environments list.
class EnvironmentsPanel(project: Project, private val environmentsSupplier: EnvironmentsSupplier) : JBPanel<EnvironmentsPanel>() {

    init {
        isOpaque = false
        andTransparent()
        layout = WrapLayout(FlowLayout.LEFT, 2, 2)
        rebuild()
        environmentsSupplier.addEnvironmentsListChangeListener(object : EnvironmentsListChangedListener {
            override fun environmentsListChanged(newEnvironments: List<String>) {
                rebuild()
            }
        })

        //we have two instances of EnvironmentsPanel, if a button is clicked in the insights tab the selected button
        //need to change also in the errors tab, and vice versa.
        val messageBusConnection = project.messageBus.connect()
        messageBusConnection.subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, EnvironmentChanged {
            select(it)
        })
        Disposer.register(project, messageBusConnection)
    }


    /*
    usually this panel works fine.
    there is one issue: sometimes when the plugin window opens on startup this panel takes too much
    vertical space and the insights list is almost not shown. any hover over this panel with the mouse or any
    other action related to the plugin will fix it and the tab will re-layout. from there on the panel functions ok.
    I could not find any way to cause this panel to layout correctly on startup.
    so the code in getPreferredSize limits the height on startup to a reasonable size and only the first few calls
    to getPreferredSize, from there on its ok. it proves to solve or at least hide the issue.
    it will be noticeable when there are many environments and only on first opening of the window and only occasionally.
     */
    private var startup = 0
    override fun getPreferredSize(): Dimension {
        if (startup < 5) {
            startup++
            val d = super.getPreferredSize()
            if (d != null) {
                return Dimension(d.width, min(d.height, 300))
            }
        }
        return super.getPreferredSize()
    }

    private fun select(newSelectedEnv: String?) {
        val currentSelected: EnvLink? = getSelected()
        if (currentSelected != null) {
            //both panels will catch the event,the one that generated the event will be ignored and not changed.
            if (Objects.equals(currentSelected.env, newSelectedEnv)) {
                return
            }
            currentSelected.deselect { buildLinkText(it, false) }
        }

        if (newSelectedEnv == null) {
            return
        }

        val toSelectPanel: SingleEnvPanel? = (components.stream().filter { (it as SingleEnvPanel).myLink.env == newSelectedEnv }.findAny().orElse(null) as SingleEnvPanel?)
        toSelectPanel?.myLink?.select { buildLinkText(it, true) }

    }

    private fun rebuild() {

        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
            revalidate()
        }

        val relevantEnvs = buildRelevantSortedEnvironments(environmentsSupplier)


        val currentEnvironment = environmentsSupplier.getCurrent()
        for (currEnv in relevantEnvs) {

            val isSelectedEnv = currEnv.contentEquals(currentEnvironment)
            val linkText = buildLinkText(currEnv, isSelectedEnv)
            val envLink = EnvLink(currEnv,linkText,isSelectedEnv)
            envLink.toolTipText = currEnv
            val singlePanel = SingleEnvPanel(envLink)
            add(singlePanel)

            envLink.addActionListener(){ event ->
                val currentSelected: EnvLink? = getSelected()

                if (currentSelected === event.source){
                    return@addActionListener
                }

                currentSelected?.deselect { buildLinkText(it, false) }

                val clickedLink: EnvLink = event.source as EnvLink
                clickedLink.select { buildLinkText(it, true) }
                environmentsSupplier.setCurrent(clickedLink.env)
            }
        }
        revalidate()
        repaint()
    }

    private fun getSelected(): EnvLink? {
        val currentSelectedPanel: SingleEnvPanel? = (components.stream().filter { (it as SingleEnvPanel).myLink.isSelectedEnvironment() }.findAny().orElse(null) as SingleEnvPanel?)
        return currentSelectedPanel?.myLink
    }


    private fun buildRelevantSortedEnvironments(envsSupplier: EnvironmentsSupplier): List<String> {
        val builtEnvs = ArrayList<String>()

        var mineLocalEnv = ""

        for (currEnv in envsSupplier.getEnvironments()) {
            if (isEnvironmentLocal(currEnv)) {
                if (isLocalEnvironmentMine(currEnv)) {
                    mineLocalEnv = currEnv
                }
                continue
            } else {
                builtEnvs.add(currEnv)
            }
        }

        builtEnvs.sortWith(String.CASE_INSENSITIVE_ORDER)
        if (mineLocalEnv.isNotBlank()) {
            builtEnvs.add(0, mineLocalEnv)
        }

        return builtEnvs
    }


    private fun buildLinkText(currEnv: String, isSelectedEnv: Boolean): String {
        var txtValue = currEnv
        if (isLocalEnvironmentMine(currEnv)) {
            txtValue = "LOCAL"
        }
        if (isSelectedEnv) {
            return asHtml(spanBoldUnderLine(txtValue))
        }
        return asHtml(span(txtValue))
    }


    private fun isEnvironmentLocal(environment: String): Boolean {
        return environment.endsWith("[local]", true)
    }

    private fun isLocalEnvironmentMine(environment: String): Boolean {
        val localHostname = CommonUtils.getLocalHostname()
        return environment.startsWith(localHostname, true)
    }


    //this is the method called by the platform when requesting focus with ContentManager.setSelectedContent
    override fun requestFocus() {
        getSelected()?.requestFocusInWindow()
    }

    override fun requestFocusInWindow(): Boolean {
        requestFocus()
        return true
    }
}



class SingleEnvPanel(val myLink: EnvLink) : JBPanel<SingleEnvPanel>() {
    init {
        isOpaque = false
        layout = GridLayout(1,1)
        border = JBUI.Borders.empty(1)
        add(myLink)
    }
}


class EnvLink(val env: String, text: String, private var isSelectedEnv: Boolean = false) : ActionLink(text) {

    fun select(textSupplier: Function<String, String>) {
        this.text = textSupplier.apply(env)
        this.isSelectedEnv = true
    }

    fun deselect(textSupplier: Function<String, String>) {
        this.text = textSupplier.apply(env)
        this.isSelectedEnv = false
    }

    fun isSelectedEnvironment(): Boolean = isSelectedEnv
}

