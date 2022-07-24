package org.digma.intellij.plugin.ui.panels

import javax.swing.JComponent

abstract class DigmaTabPanel: DigmaResettablePanel() {
    abstract fun getPreferredFocusableComponent(): JComponent
    abstract fun getPreferredFocusedComponent(): JComponent
}