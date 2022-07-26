package org.digma.intellij.plugin.errors;

import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.Collections;
import java.util.List;

import static org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult;

public class ErrorsListContainer {

    private final List<ListViewItem<CodeObjectError>> listViewItems;
    private final UsageStatusResult usageStatus;

    public ErrorsListContainer(List<ListViewItem<CodeObjectError>> listViewItems, UsageStatusResult usageStatus) {
        this.listViewItems = listViewItems;
        this.usageStatus = usageStatus;
    }

    public ErrorsListContainer() {
        this(Collections.emptyList(), EmptyUsageStatusResult);
    }

    public int getCount() {
        return listViewItems.size();
    }

    public List<ListViewItem<CodeObjectError>> getListViewItems() {
        return listViewItems;
    }

    public UsageStatusResult getUsageStatus() {
        return usageStatus;
    }
}
