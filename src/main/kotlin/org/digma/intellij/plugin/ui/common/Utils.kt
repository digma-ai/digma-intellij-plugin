package org.digma.intellij.plugin.ui.common

import java.awt.Color


fun __colorToHex(color: Color): String {
    return "#" + Integer.toHexString(color.getRGB()).substring(2)
}