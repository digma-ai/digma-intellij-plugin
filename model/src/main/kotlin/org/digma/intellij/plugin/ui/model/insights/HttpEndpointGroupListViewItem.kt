package org.digma.intellij.plugin.ui.model.insights

class HttpEndpointGroupListViewItem(route: String) : InsightGroupListViewItem(route, InsightGroupType.HttpEndpoint) {

    fun getRoute(): String {
        return groupId;
    }

}