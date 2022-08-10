package org.digma.intellij.plugin.model;

import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;

import java.util.Collections;

public final class Models {

    private Models() {
    }

    public static final class Empties {

        private Empties() {
        }

        //todo: maybe change to kotlin object ?
        public static final UsageStatusResult EmptyUsageStatusResult = new UsageStatusResult(Collections.emptyList(), Collections.emptyList());

    }

}
