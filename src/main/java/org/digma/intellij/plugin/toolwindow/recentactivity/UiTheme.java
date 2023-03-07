package org.digma.intellij.plugin.toolwindow.recentactivity;

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