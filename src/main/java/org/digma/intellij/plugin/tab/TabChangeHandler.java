package org.digma.intellij.plugin.tab;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.TabsChanged;

/**
 * The central handler of TabsChanged events.
 * it will perform the necessary actions that are common to all languages or IDEs.
 */
public class TabChangeHandler implements TabsChanged {

    @Override
    public void activeTabIndexChanged(int newTabIndex) {
        //nothing to do here
    }
}
