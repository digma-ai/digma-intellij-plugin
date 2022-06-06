package org.digma.intellij.plugin.ui.panels

import javax.swing.JComponent
import javax.swing.JPanel

abstract class DigmaTabPanel: JPanel() {

    abstract fun getPreferredFocusableComponent(): JComponent
    abstract fun getPreferredFocusedComponent(): JComponent
    abstract fun reset()
}