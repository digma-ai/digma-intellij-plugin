package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ListGroupManager {

    private final Map<String, GroupListViewItem> groups = new HashMap<>();

    public GroupListViewItem getOrCreateGroup(String groupKey, Supplier<GroupListViewItem> supplier) {
        return groups.computeIfAbsent(groupKey, key -> supplier.get());
    }

    public Collection<GroupListViewItem> getGroupItems() {
        return groups.values();
    }

}
