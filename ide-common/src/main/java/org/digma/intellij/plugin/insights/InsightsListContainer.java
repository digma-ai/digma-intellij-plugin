package org.digma.intellij.plugin.insights;

import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult;

public class InsightsListContainer {

    private final int count;
    private final List<ListViewItem<?>> listViewItems;
    private final UsageStatusResult usageStatus;

    public InsightsListContainer(List<ListViewItem<?>> listViewItems, int count, UsageStatusResult usageStatus) {
        this.listViewItems = listViewItems;
        this.count = count;
        this.usageStatus = usageStatus;
    }

    public InsightsListContainer() {
        this(Collections.emptyList(), 0, EmptyUsageStatusResult);
    }


    public int getCount() {
        return count;
    }

    @Nullable
    public List<ListViewItem<?>> getListViewItems() {
        return listViewItems;
    }

    @Nullable
    public UsageStatusResult getUsageStatus() {
        return usageStatus;
    }
}
