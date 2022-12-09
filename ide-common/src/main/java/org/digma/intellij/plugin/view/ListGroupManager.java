package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ListGroupManager {

    private final Map<String, GroupListViewItem> groups = new HashMap<>();

    public GroupListViewItem getOrCreateGroup(String groupKey, Supplier<GroupListViewItem> supplier) {
        Optional<String> existingMatchingGroupKey = groups.keySet()
                .stream()
        //FYI: Temporary fix because from BE we receive some insights with groupId = "epHTTP:POST Transfer/TransferFunds" and some insights with groupId = "HTTP POST Transfer/TransferFunds" but all these items should be displayed on the same group on UI
                .filter(s -> s.endsWith(groupKey.replace("HTTP ", "")))
                .findFirst();
        return groups.computeIfAbsent(existingMatchingGroupKey.orElse(groupKey), key -> supplier.get());
    }

    public Collection<GroupListViewItem> getGroupItems() {
        return groups.values();
    }

}
