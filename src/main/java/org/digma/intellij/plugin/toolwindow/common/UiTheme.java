package org.digma.intellij.plugin.toolwindow.common;

public enum UiTheme {
    DARK("dark-jetbrains"),
    LIGHT("light"),
    ;

    private String themeName;
    UiTheme(String themeName) {
        this.themeName = themeName;
    }

    public String getThemeName() {
        return themeName;
    }

}