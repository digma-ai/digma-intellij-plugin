package org.digma.intellij.plugin.protocol

import com.intellij.util.text.nullize
import java.net.URLEncoder


fun Map<String, String>.toUrlQueryString() =
    this.map {(k,v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
        .joinToString("&")


fun getActionFromParameters(parameters: Map<String, String>):String?{
    return parameters[ACTION_PARAM_NAME]?.nullize(nullizeSpaces = true)
}


fun getCodeObjectIdFromParameters(parameters: Map<String, String>):String?{
    return parameters[CODE_OBJECT_ID_PARAM_NAME]?.nullize(nullizeSpaces = true)
}

fun getEnvironmentIdFromParameters(parameters: Map<String, String>):String?{
    return parameters[ENVIRONMENT_ID_PARAM_NAME]?.nullize(nullizeSpaces = true)
}


