package org.digma.intellij.plugin.ui;

import com.intellij.ui.GuiUtils;

import java.awt.*;

public class DigmaUIUtil {


    public static String digmaColorToHex(Color color){
        var s = GuiUtils.colorToHex(color);
        return s.startsWith("#") ? s : "#"+s;
    }

    public static Color digmaDecodeColor(String hex,Color defaultColor){
        try {
            return Color.decode(hex);
        }catch(Exception e){
            return defaultColor;
        }
    }




}
