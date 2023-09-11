package org.digma.intellij.plugin.ui.settings

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.Alarm
import com.intellij.util.ui.UIUtil
import org.digma.intellij.plugin.ui.service.InsightsViewService
import org.digma.intellij.plugin.ui.service.SummaryViewService


@Service(Service.Level.PROJECT)
class ApplicationUISettingsChangeNotifier : Disposable{

    private val listeners = mutableListOf<SettingsChangeListener>()

    private val myLafChangeAlarm = Alarm(this)
    private val myEditorUIChangeAlarm = Alarm(this)

    companion object{
        @JvmStatic
        fun getInstance(project: Project):ApplicationUISettingsChangeNotifier{
            return project.service<ApplicationUISettingsChangeNotifier>()
        }
    }

    init {

        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            LafManagerListener.TOPIC,
            LafManagerListener {
                myLafChangeAlarm.cancelAllRequests()
                myLafChangeAlarm.addRequest({
                    fireLafChanged()
                },200)
            })


        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            EditorColorsManager.TOPIC,
            EditorColorsListener {
                myEditorUIChangeAlarm.cancelAllRequests()
                myEditorUIChangeAlarm.addRequest({
                    fireEditorUIChanged()
                },200)
            })

    }

    private fun fireEditorUIChanged() {
        listeners.forEach {
            it.editorFontChange(AppEditorFontOptions.getInstance().fontPreferences.fontFamily)
        }
    }

    private fun fireLafChanged() {
        listeners.forEach {
            it.systemFontChange(UIUtil.getLabelFont().fontName)
            it.systemThemeChange(if(JBColor.isBright()) Theme.LIGHT else Theme.DARK)
        }
    }


    fun addSettingsChangeListener(listener: SettingsChangeListener){
        listeners.add(listener)
    }

    fun removeSettingsChangeListener(listener: SettingsChangeListener){
        listeners.remove(listener)
    }

    override fun dispose() {
        listeners.clear()
    }

}