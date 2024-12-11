package org.digma.intellij.plugin.ui.recentactivity

const val RECENT_ACTIVITY_DOMAIN_NAME = "recentactivity"
const val RECENT_ACTIVITY_APP_SCHEMA = "https"
const val RECENT_ACTIVITY_RESOURCE_FOLDER_NAME = "recent-activity"
const val RECENT_ACTIVITY_URL = "$RECENT_ACTIVITY_APP_SCHEMA://$RECENT_ACTIVITY_DOMAIN_NAME/$RECENT_ACTIVITY_RESOURCE_FOLDER_NAME/index.html"
const val RECENT_ACTIVITY_TEMPLATE_FOLDER_NAME = "/webview/recentactivity"
const val RECENT_ACTIVITY_INDEX_TEMPLATE_NAME = "recentActivityTemplate.ftl"
const val RECENT_EXPIRATION_LIMIT_MILLIS: Long = 10 * 60 * 1000 // 10min