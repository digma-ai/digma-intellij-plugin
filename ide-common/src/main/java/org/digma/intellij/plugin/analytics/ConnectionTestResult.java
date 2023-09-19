package org.digma.intellij.plugin.analytics;

public class ConnectionTestResult {
    private String result;
    private String error;

    public ConnectionTestResult(String result, String error) {
        this.result = result;
        this.error = error;
    }


    public static ConnectionTestResult success() {
        return new ConnectionTestResult("success", null);
    }

    public static ConnectionTestResult failure(String errorMessage) {
        return new ConnectionTestResult("failure", errorMessage);
    }
}