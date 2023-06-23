package org.digma.intellij.plugin.ui.settings

interface SettingsChangeListener {

    fun systemFontChange(fontName: String)

    fun systemThemeChange(theme: Theme)

    fun editorFontChange(fontName: String)

}