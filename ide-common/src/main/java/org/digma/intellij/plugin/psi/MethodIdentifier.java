package org.digma.intellij.plugin.psi;

public class MethodIdentifier {

    private String name;
    private String file;

    public MethodIdentifier(String name, String file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "MethodIdentifier{\n" +
                "name='" + name + '\'' + "\n" +
                "file='" + file + '\'' + "\n" +
                '}';
    }
}
