package org.digma.intellij.plugin.toolwindow;

public enum UiTheme {
    DARK("dark"),
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