package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Objects;

public class NotificationsMock {
    private final AnalyticsService analyticsService;

    private int seenNotifications = 0;

    public NotificationsMock(AnalyticsService analyticsService) {

        this.analyticsService = analyticsService;
    }


    public String getNotifications(int pageNumber, int pageSize, boolean isRead) throws AnalyticsServiceException, JsonProcessingException {

        var env = analyticsService.getEnvironment().getCurrent();
        var accountId = "0000-0000-0000";

        ObjectMapper mapper = new ObjectMapper();
        var notificationRoot = mapper.createObjectNode();
        var notificationsArray = mapper.createArrayNode();
        notificationRoot.set("notifications", notificationsArray);


        var om = new ObjectMapper();
        var assets = om.readTree(analyticsService.getAssets());
        var serviceAssetsEntries = (ArrayNode) assets.get("serviceAssetsEntries");
        serviceAssetsEntries.forEach(jsonNode -> {
            if (Objects.equals(jsonNode.get("serviceName").asText(), "spring-petclinic")) {
                var assetEntries = (ArrayNode) jsonNode.get("assetEntries");

                int startAt = 0;

                startAt = (pageNumber - 1) * pageSize;
                int endAt = Math.min(startAt + pageSize, assetEntries.size());

                if (pageNumber == 1 && pageSize == 3) {
                    startAt = 0;
                    endAt = Math.min(3, assetEntries.size());
                }


                for (int index = startAt; index < endAt; index++) {
                    JsonNode asset = assetEntries.get(index);
                    var spanCodeObjectId = asset.get("span").get("spanCodeObjectId").asText();
                    var displayName = asset.get("span").get("displayName").asText();
                    var insights = (ArrayNode) asset.get("insights");
                    var insightType = "HotSpot";
                    if (insights.size() == 2) {
                        insightType = insights.get(1).get("type").asText();
                    } else if (insights.size() == 1) {
                        insightType = insights.get(0).get("type").asText();
                    }


                    var notification = mapper.createObjectNode();
                    notification.set("notificationId", new IntNode(index));
                    notification.set("accountId", new TextNode(accountId));
                    notification.set("environment", new TextNode(env));
                    notification.set("title", new TextNode("title " + index));
                    notification.set("message", new TextNode("message " + index));
                    notification.set("type", new TextNode("insight"));
                    var data = mapper.createObjectNode();
                    notification.set("data", data);
                    data.set("insightType", new TextNode("InsightType" + insightType));
                    var codeObject = mapper.createObjectNode();
                    data.set("codeObject", codeObject);
                    codeObject.set("displayName", new TextNode(displayName));
                    codeObject.set("codeObjectId", new TextNode(spanCodeObjectId));
                    notification.set("isRead", BooleanNode.FALSE);
                    notification.set("timestamp", new TextNode("2023-01-07T12:59:21.794Z"));

                    notificationsArray.add(notification);

                }

                notificationRoot.set("totalCount", new IntNode(assetEntries.size()));
                notificationRoot.set("unreadCount", new IntNode(assetEntries.size()));
            }
        });


        return mapper.writeValueAsString(notificationRoot);

    }

    public long getUnreadCount() throws AnalyticsServiceException, JsonProcessingException {

        int unread = 0;

        var om = new ObjectMapper();
        var assets = om.readTree(analyticsService.getAssets());
        var serviceAssetsEntries = (ArrayNode) assets.get("serviceAssetsEntries");
        for (JsonNode jsonNode : serviceAssetsEntries) {
            if (Objects.equals(jsonNode.get("serviceName").asText(), "spring-petclinic")) {
                var assetEntries = (ArrayNode) jsonNode.get("assetEntries");
                unread = assetEntries.size() - seenNotifications;
            }
        }
        return unread;
    }
}
