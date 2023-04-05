package org.digma.intellij.plugin.toolwindow.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ToolWindowUtil {
    public static final String REQUEST_MESSAGE_TYPE = "digma";
    public static final String GLOBAL_SET_UI_THEME = "GLOBAL/SET_THEME";
    public static final String GLOBAL_SET_IS_JAEGER_ENABLED = "GLOBAL/SET_IS_JAEGER_ENABLED";
    public static final String GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER = "GLOBAL/OPEN_URL_IN_DEFAULT_BROWSER";
    public static final String RECENT_ACTIVITY_SET_DATA = "RECENT_ACTIVITY/SET_DATA";
    public static final String RECENT_ACTIVITY_GET_DATA = "RECENT_ACTIVITY/GET_DATA";
    public static final String RECENT_ACTIVITY_GO_TO_SPAN = "RECENT_ACTIVITY/GO_TO_SPAN";
    public static final String RECENT_ACTIVITY_GO_TO_TRACE = "RECENT_ACTIVITY/GO_TO_TRACE";
    public static final String INSTALLATION_WIZARD_FINISH = "INSTALLATION_WIZARD/FINISH";
    public static final String INSTALLATION_WIZARD_CHECK_CONNECTION = "INSTALLATION_WIZARD/CHECK_CONNECTION";
    public static final String INSTALLATION_WIZARD_SET_CHECK_CONNECTION = "INSTALLATION_WIZARD/SET_CONNECTION_CHECK_RESULT";
    public static final String INSTALLATION_WIZARD_SEND_TRACKING_EVENT = "GLOBAL/SEND_TRACKING_EVENT";
    public static final String INSTALLATION_WIZARD_SET_OBSERVABILITY = "INSTALLATION_WIZARD/SET_OBSERVABILITY";

    public static <T> T parseJsonToObject(String jsonString, Class<T> jcefMessageRequestClass) {
        JsonObject object = JsonParser.parseString(jsonString).getAsJsonObject();
        return new Gson().fromJson(object, jcefMessageRequestClass);
    }
}
