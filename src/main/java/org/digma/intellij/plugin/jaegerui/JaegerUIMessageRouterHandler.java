package org.digma.intellij.plugin.jaegerui;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.jaegerui.model.incoming.*;
import org.digma.intellij.plugin.jaegerui.model.outgoing.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.serializeAndExecuteWindowPostMessageJavaScript;

public class JaegerUIMessageRouterHandler extends BaseMessageRouterHandler {

    private final Logger logger = Logger.getInstance(JaegerUIMessageRouterHandler.class);


    public JaegerUIMessageRouterHandler(Project project) {
        super(project);
    }


    @NotNull
    @Override
    public String getOriginForTroubleshootingEvent() {
        return "jaegerui";
    }

    @Override
    public boolean doOnQuery(@NotNull Project project, @NotNull CefBrowser browser, @NotNull JsonNode requestJsonNode, @NotNull String rawRequest, @NotNull String action) throws Exception {

        switch (action) {
            case "GET_SPANS_DATA" -> {

                SpansMessage spansMessage = getObjectMapper().treeToValue(requestJsonNode, SpansMessage.class);

                Map<String, SpanData> resolvedSpans;
                try {
                    resolvedSpans = JaegerUIService.getInstance(project).getResolvedSpans(spansMessage);
                } catch (Exception e) {
                    Log.debugWithException(logger, e, "Exception while resolving spans for GET_SPANS_DATA");
                    resolvedSpans = Collections.emptyMap();
                }

                var spansWithResolvedLocationMessage = new SpansWithResolvedLocationMessage("digma",
                        "SET_SPANS_DATA", resolvedSpans);


                serializeAndExecuteWindowPostMessageJavaScript(browser, spansWithResolvedLocationMessage);
            }

            case "GO_TO_SPAN" -> {
                GoToSpanMessage goToSpanMessage = getObjectMapper().treeToValue(requestJsonNode, GoToSpanMessage.class);
                JaegerUIService.getInstance(project).navigateToCode(goToSpanMessage);
            }

            case "GO_TO_INSIGHTS" -> {
                //it's the same message as go to span
                GoToSpanMessage goToSpanMessage = getObjectMapper().treeToValue(requestJsonNode, GoToSpanMessage.class);
                JaegerUIService.getInstance(project).goToInsight(goToSpanMessage);
            }

            default -> {
                return false;
            }
        }

        return true;
    }

}
