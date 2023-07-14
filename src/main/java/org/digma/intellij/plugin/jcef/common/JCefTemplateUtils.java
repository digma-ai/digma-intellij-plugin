package org.digma.intellij.plugin.jcef.common;

import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;
import org.digma.intellij.plugin.ui.settings.Theme;

import java.util.Map;

public class JCefTemplateUtils {
    public static final String ENV_VARIABLE_THEME = "theme";
    public static final String ENV_VARIABLE_FONT = "mainFont";
    public static final String ENV_VARIABLE_CODE_FONT = "codeFont";
    public static final String IS_JAEGER_ENABLED = "isJaegerEnabled";

    private JCefTemplateUtils() {
    }


    public static void addCommonEnvVariables(Map<String, Object> env) {
        env.put(ENV_VARIABLE_THEME, JBColor.isBright() ? Theme.LIGHT.getThemeName() : Theme.DARK.getThemeName());
        env.put(ENV_VARIABLE_FONT, UIUtil.getLabelFont().getFontName());
        env.put(ENV_VARIABLE_CODE_FONT, AppEditorFontOptions.getInstance().getFontPreferences().getFontFamily());
    }

    public static void addIsJaegerEnabled(Map<String, Object> env) {
        env.put(IS_JAEGER_ENABLED, String.valueOf(JaegerUtilKt.isJaegerButtonEnabled()));
    }


}
