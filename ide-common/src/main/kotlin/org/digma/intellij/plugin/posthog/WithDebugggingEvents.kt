package org.digma.intellij.plugin.posthog



fun withDebuggingEvents(function: () -> Unit){
    if (java.lang.Boolean.getBoolean("org.digma.plugin.debug.events")){
        function.invoke()
    }
}