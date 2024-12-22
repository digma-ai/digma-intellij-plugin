package org.digma.intellij.plugin.common

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.service
import org.digma.intellij.plugin.model.rest.version.VersionRequest
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import org.digma.intellij.plugin.updates.ui.UIVersioningService


// returns one of:
// IC - Intellij Community
// RD - Rider
fun getPlatformType(): String {
    val appInfo = ApplicationInfo.getInstance()
    return appInfo.build.productCode
}

fun getPlatformVersion(): String {
    val appInfo = ApplicationInfo.getInstance()
    return appInfo.fullVersion
}

// when plugin is not installed it will return 0.0.0
fun getPluginVersion(defaultIfNotFound: String = "0.0.0"): String {
    return SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease(defaultIfNotFound)
}

fun getUiVersion(): String {
    return service<UIVersioningService>().getUiVersionForVersionRequest()
}




fun buildVersionRequest(): VersionRequest {
    return VersionRequest(
        getPluginVersion(), getPlatformType(), getPlatformVersion(), getUiVersion()
    )
}