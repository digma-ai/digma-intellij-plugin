package org.digma.intellij.plugin.ui.panels

import javax.swing.JPanel

abstract class ResettablePanel: JPanel() {

    abstract fun reset()
}