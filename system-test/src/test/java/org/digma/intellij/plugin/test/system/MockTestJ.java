package org.digma.intellij.plugin.test.system;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.junit.Test;

public class MockTestJ extends LightJavaCodeInsightFixtureTestCase {

    @Test
    public void testServicesAreUpAndRunning() {
        ActivityMonitor.getInstance(getProject());
    }

}
