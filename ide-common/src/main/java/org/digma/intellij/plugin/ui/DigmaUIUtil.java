package org.digma.intellij.plugin.ui;

import com.intellij.ui.GuiUtils;

import java.awt.*;

public class DigmaUIUtil {


    public static String digmaColorToHex(Color color){
        var s = GuiUtils.colorToHex(color);
        return s.startsWith("#") ? s : "#"+s;
    }
}
