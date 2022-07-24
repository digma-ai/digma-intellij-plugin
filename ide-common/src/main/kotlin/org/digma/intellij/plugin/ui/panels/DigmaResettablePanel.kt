package org.digma.intellij.plugin.ui.panels

import javax.swing.JPanel

abstract class DigmaResettablePanel: JPanel() {

    abstract fun reset()
}