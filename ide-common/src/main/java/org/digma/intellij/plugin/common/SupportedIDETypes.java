package org.digma.intellij.plugin.common;


public enum SupportedIDETypes {

    IDEA("IDEA"),
    RIDER("Rider"),
    PYCHARM("PyCharm");

    private final String productName;

    SupportedIDETypes(String productName) {
        this.productName = productName;
    }

    public String getProductName() {
        return productName;
    }
}