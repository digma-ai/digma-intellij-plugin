package org.digma.intellij.plugin.ui.common

import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.containers.stream
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsListChangedListener
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.awt.*
import javax.swing.JPanel

fun environmentsPanel(environmentsSupplier: EnvironmentsSupplier): JPanel {

    val envsPanel = EnvironmentsPanel(environmentsSupplier)

    val result = JPanel()
    result.isOpaque = false
    result.border = JBUI.Borders.empty()
    result.layout = BorderLayout()
    result.add(envsPanel, BorderLayout.CENTER)
    return result

}



class EnvironmentsPanel(private val environmentsSupplier: EnvironmentsSupplier) : JBPanel<EnvironmentsPanel>() {

    init {
        isOpaque = false
        andTransparent()
        layout = WrapLayout(FlowLayout.LEFT,2,2)
        rebuild()
        environmentsSupplier.addEnvironmentsListChangeListener(object: EnvironmentsListChangedListener{
            override fun environmentsListChanged(newEnvironments: List<String>) {
                rebuild()
            }
        })
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
                val currentSelectedPanel: SingleEnvPanel? = (components.stream().filter { (it as SingleEnvPanel).myLink.isSelectedEnv }.findAny().orElse(null) as SingleEnvPanel?)
                val currentSelected: EnvLink? = currentSelectedPanel?.myLink

                if (currentSelected === event.source){
                    return@addActionListener
                }

                if (currentSelected != null) {
                    currentSelected.isSelectedEnv = false
                    currentSelected.text = buildLinkText(currentSelected.env, false)
                }

                val clickedLink: EnvLink = event.source as EnvLink
                clickedLink.text = buildLinkText(clickedLink.env,true)
                clickedLink.isSelectedEnv = true
                environmentsSupplier.setCurrent(clickedLink.env)
            }
        }
        revalidate()
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
}



class SingleEnvPanel(val myLink: EnvLink) : JBPanel<SingleEnvPanel>() {
    init {
        isOpaque = false
        layout = GridLayout(1,1)
        border = JBUI.Borders.empty(1)
        add(myLink)
    }
}




class EnvLink(val env: String,text: String,var isSelectedEnv: Boolean = false) : ActionLink(text){

}

