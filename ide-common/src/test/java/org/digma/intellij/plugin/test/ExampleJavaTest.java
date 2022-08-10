package org.digma.intellij.plugin.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ExampleJavaTest {

    //this test is just for checking that the workaround in buildSrc/src/main/kotlin/digma-base.gradle.kts
    //testing extension really works. see the dependency:
    //runtimeOnly("org.junit.platform:junit-platform-launcher")
    //runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
    //when it's fixed in intellij we can delete this test

    @Test
    void testMe() {
        Assertions.assertTrue(1 > 0);
    }

}
