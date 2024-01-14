package org.digma.intellij.plugin.jcef.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//todo: move all to src/main/kotlin/org/digma/intellij/plugin/ui/jcef/JCEFGlobalConstants.kt
public class JCefMessagesUtils {

    private JCefMessagesUtils() {}

    public static final String REQUEST_MESSAGE_TYPE = "digma";
    public static final String GLOBAL_SET_UI_THEME = "GLOBAL/SET_THEME";
    public static final String GLOBAL_SET_UI_MAIN_FONT = "GLOBAL/SET_MAIN_FONT";
    public static final String GLOBAL_SET_UI_CODE_FONT = "GLOBAL/SET_CODE_FONT";
    public static final String GLOBAL_SET_IS_JAEGER_ENABLED = "GLOBAL/SET_IS_JAEGER_ENABLED";
    public static final String GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER = "GLOBAL/OPEN_URL_IN_DEFAULT_BROWSER";
    public static final String GLOBAL_OPEN_URL_IN_EDITOR_TAB = "GLOBAL/OPEN_URL_IN_EDITOR_TAB";
    public static final String GLOBAL_SEND_TRACKING_EVENT = "GLOBAL/SEND_TRACKING_EVENT";
    public static final String GLOBAL_OPEN_TROUBLESHOOTING_GUIDE = "GLOBAL/OPEN_TROUBLESHOOTING_GUIDE";
    public static final String GLOBAL_OPEN_DOCUMENTATION = "GLOBAL/OPEN_DOCUMENTATION";
    public static final String INSTALLATION_WIZARD_FINISH = "INSTALLATION_WIZARD/FINISH";
    public static final String INSTALLATION_WIZARD_CHECK_CONNECTION = "INSTALLATION_WIZARD/CHECK_CONNECTION";
    public static final String INSTALLATION_WIZARD_SET_CHECK_CONNECTION = "INSTALLATION_WIZARD/SET_CONNECTION_CHECK_RESULT";
    public static final String INSTALLATION_WIZARD_SET_OBSERVABILITY = "INSTALLATION_WIZARD/SET_OBSERVABILITY";
    public static final String INSTALLATION_WIZARD_INSTALL_DIGMA_ENGINE = "INSTALLATION_WIZARD/INSTALL_DIGMA_ENGINE";
    public static final String INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT = "INSTALLATION_WIZARD/SET_INSTALL_DIGMA_ENGINE_RESULT";
    public static final String INSTALLATION_WIZARD_UNINSTALL_DIGMA_ENGINE = "INSTALLATION_WIZARD/UNINSTALL_DIGMA_ENGINE";
    public static final String INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT = "INSTALLATION_WIZARD/SET_UNINSTALL_DIGMA_ENGINE_RESULT";
    public static final String INSTALLATION_WIZARD_START_DIGMA_ENGINE = "INSTALLATION_WIZARD/START_DIGMA_ENGINE";
    public static final String INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT = "INSTALLATION_WIZARD/SET_START_DIGMA_ENGINE_RESULT";
    public static final String INSTALLATION_WIZARD_STOP_DIGMA_ENGINE = "INSTALLATION_WIZARD/STOP_DIGMA_ENGINE";
    public static final String INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT = "INSTALLATION_WIZARD/SET_STOP_DIGMA_ENGINE_RESULT";
    public static final String GLOBAL_SET_ENVIRONMENT = "GLOBAL/SET_ENVIRONMENT";
    public static final String GLOBAL_SET_ENVIRONMENTS = "GLOBAL/SET_ENVIRONMENTS";
    public static final String GLOBAL_SET_IS_DIGMA_ENGINE_INSTALLED = "GLOBAL/SET_IS_DIGMA_ENGINE_INSTALLED";
    public static final String GLOBAL_SET_IS_DIGMA_ENGINE_RUNNING = "GLOBAL/SET_IS_DIGMA_ENGINE_RUNNING";
    public static final String GLOBAL_SET_IS_DOCKER_INSTALLED = "GLOBAL/SET_IS_DOCKER_INSTALLED";
    public static final String GLOBAL_SET_IS_DOCKER_COMPOSE_INSTALLED = "GLOBAL/SET_IS_DOCKER_COMPOSE_INSTALLED";
    public static final String INSTALLATION_WIZARD_SET_CURRENT_STEP = "INSTALLATION_WIZARD/SET_CURRENT_STEP";
    public static final String GLOBAL_SET_OBSERVABILITY = "GLOBAL/SET_OBSERVABILITY";
    public static final String GLOBAL_REGISTER = "GLOBAL/REGISTER";
    public static final String GLOBAL_SET_IS_OBSERVABILITY_ENABLED = "GLOBAL/SET_IS_OBSERVABILITY_ENABLED";
    public static final String GLOBAL_SET_BACKEND_INFO = "GLOBAL/SET_BACKEND_INFO";


    //todo: replace with org.digma.intellij.plugin.ui.jcef.JCefBrowserUtilsKt.jsonToObject
    public static <T> T parseJsonToObject(String jsonString, Class<T> jcefMessageRequestClass) {
        JsonObject object = JsonParser.parseString(jsonString).getAsJsonObject();
        return new Gson().fromJson(object, jcefMessageRequestClass);
    }
}
